package monedero.servicio;

import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;
import monedero.estructuras.Mapa;
import monedero.estructuras.TablaHash;
import monedero.excepciones.NoEncontradoException;
import monedero.excepciones.OperacionInvalidaException;
import monedero.excepciones.SaldoInsuficienteException;
import monedero.modelo.Beneficio;
import monedero.modelo.BeneficioActivo;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Moneda;
import monedero.modelo.Monedero;
import monedero.modelo.MovimientoPuntos;
import monedero.modelo.Rango;
import monedero.modelo.Rol;
import monedero.modelo.TipoNotificacion;
import monedero.modelo.TipoMonedero;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Transaccion;

public class SistemaMonedero {

    public static final long UNIDADES_SALDO_BAJO = 500;

    public static final long MONTO_MAXIMO = 1_000_000_000L;

    private final Mapa<String, Cliente> clientes = new TablaHash<>();
    private final Mapa<String, Monedero> monederos = new TablaHash<>();
    private final Coleccion<Transaccion> transacciones = new ListaEnlazada<>();
    private final RegistroPuntos registroPuntos = new RegistroPuntos();
    private final ServicioUsuarios usuarios = new ServicioUsuarios();
    private final Notificador notificador = new Notificador();
    private final ProgramadorTransacciones programador = new ProgramadorTransacciones(this);
    private final PoliticaComision politicaComision;
    private final CalculadoraPuntos calculadoraPuntos;
    private final Reloj reloj;

    private int consecutivoCliente = 0;
    private int consecutivoMonedero = 0;
    private int consecutivoTransaccion = 0;

    public SistemaMonedero() {
        this(new Reloj());
    }

    public SistemaMonedero(Reloj reloj) {
        this(reloj, new ComisionEstandar());
    }

    public SistemaMonedero(Reloj reloj, PoliticaComision politicaComision) {
        this(reloj, politicaComision, CalculadoraPuntos.PESOS_COLOMBIANOS);
    }

    public SistemaMonedero(Reloj reloj, PoliticaComision politicaComision, CalculadoraPuntos calculadoraPuntos) {
        if (reloj == null || politicaComision == null || calculadoraPuntos == null) {
            throw new IllegalArgumentException("El reloj, la comision y la escala de puntos no pueden ser nulos");
        }
        this.reloj = reloj;
        this.politicaComision = politicaComision;
        this.calculadoraPuntos = calculadoraPuntos;
    }

    public Cliente registrarCliente(String nombre) {
        Cliente cliente = new Cliente(String.format("C%03d", consecutivoCliente + 1), nombre);
        consecutivoCliente++;
        clientes.poner(cliente.getId(), cliente);
        registroPuntos.agregar(cliente);
        crearMonedero(cliente.getId(), "Principal", TipoMonedero.PRINCIPAL);
        return cliente;
    }

    public Cliente registrarClienteConUsuario(String nombre, String nombreUsuario, String contrasena) {
        usuarios.validarNuevo(nombreUsuario, contrasena);
        Cliente cliente = registrarCliente(nombre);
        usuarios.registrar(nombreUsuario, contrasena, Rol.CLIENTE, cliente.getId());
        return cliente;
    }

    public ServicioUsuarios getUsuarios() {
        return usuarios;
    }

    public Monedero crearMonedero(String clienteId, String nombre, TipoMonedero tipo) {
        Cliente cliente = buscarCliente(clienteId);
        Monedero monedero = new Monedero(String.format("M%03d", consecutivoMonedero + 1), clienteId, nombre, tipo);
        consecutivoMonedero++;
        cliente.agregarMonedero(monedero);
        monederos.poner(monedero.getId(), monedero);
        return monedero;
    }

    public Cliente buscarCliente(String clienteId) {
        Cliente cliente = clientes.obtener(clienteId);
        if (cliente == null) {
            throw new NoEncontradoException("Cliente", clienteId);
        }
        return cliente;
    }

    public Monedero buscarMonedero(String monederoId) {
        Monedero monedero = monederos.obtener(monederoId);
        if (monedero == null) {
            throw new NoEncontradoException("Monedero", monederoId);
        }
        return monedero;
    }

    public boolean existeMonedero(String monederoId) {
        return monederos.contiene(monederoId);
    }

    public Transaccion depositar(String monederoId, long monto, String descripcion) {
        Monedero destino = buscarMonedero(monederoId);
        validarMonto(monto);
        Transaccion t = crearTransaccion(TipoTransaccion.DEPOSITO, monto, 0, null, destino.getId(), null, descripcion);
        destino.aplicar(t);
        return registrar(t, destino.getClienteId());
    }

    public Transaccion retirar(String monederoId, long monto, Categoria categoria, String descripcion) {
        Monedero origen = buscarMonedero(monederoId);
        validarMonto(monto);
        long comision = calcularComision(TipoTransaccion.RETIRO, monto, origen);
        validarSaldo(origen, monto + comision);
        Transaccion t = crearTransaccion(TipoTransaccion.RETIRO, monto, comision, origen.getId(), null, categoria, descripcion);
        origen.aplicar(t);
        registrar(t, origen.getClienteId());
        avisarSiSaldoBajo(origen);
        return t;
    }

    public Transaccion transferir(String origenId, String destinoId, long monto,
                                  Categoria categoria, String descripcion) {
        Monedero origen = buscarMonedero(origenId);
        Monedero destino = buscarMonedero(destinoId);
        if (origen.equals(destino)) {
            throw new OperacionInvalidaException("No se puede transferir al mismo monedero");
        }
        validarMonto(monto);

        TipoTransaccion tipo = origen.getClienteId().equals(destino.getClienteId())
                ? TipoTransaccion.MOVIMIENTO_INTERNO
                : TipoTransaccion.TRANSFERENCIA;
        long comision = calcularComision(tipo, monto, origen);
        validarSaldo(origen, monto + comision);

        Transaccion t = crearTransaccion(tipo, monto, comision, origen.getId(), destino.getId(), categoria, descripcion);
        origen.aplicar(t);
        destino.aplicar(t);
        registrar(t, origen.getClienteId());
        avisarSiSaldoBajo(origen);
        if (tipo == TipoTransaccion.TRANSFERENCIA) {
            notificador.notificar(destino.getClienteId(), TipoNotificacion.TRANSFERENCIA_RECIBIDA,
                    "Recibiste " + Moneda.pesos(monto) + " de " + buscarCliente(origen.getClienteId()).getNombre()
                            + " en " + destino.getNombre(), reloj.ahora());
        }
        return t;
    }

    public Transaccion revertirUltima(String clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        if (!cliente.tieneReversibles()) {
            throw new OperacionInvalidaException(clienteId + " no tiene transacciones para revertir");
        }
        Transaccion t = cliente.ultimaReversible();

        Monedero origen = t.getOrigenId() == null ? null : buscarMonedero(t.getOrigenId());
        Monedero destino = t.getDestinoId() == null ? null : buscarMonedero(t.getDestinoId());
        if (destino != null && destino.saldoMinimoDesde(t) < t.getMonto()) {
            throw new OperacionInvalidaException("No se puede revertir " + t.getId() + ": los " + Moneda.pesos(t.getMonto())
                    + " que recibio " + destino.getId() + " ya se usaron despues");
        }
        if (cliente.getPuntosDisponibles() < t.getPuntos()) {
            throw new OperacionInvalidaException("No se puede revertir " + t.getId() + ": sus " + t.getPuntos()
                    + " puntos ya se canjearon");
        }

        Rango antes = cliente.getRango();
        if (destino != null) {
            destino.revertir(t);
        }
        if (origen != null) {
            origen.revertir(t);
        }
        t.marcarRevertida(reloj.ahora());
        cliente.sacarReversible();
        registroPuntos.perder(cliente, t.getPuntos(), reloj.ahora(), "Reversion " + t.getId());

        notificador.notificar(clienteId, TipoNotificacion.REVERSION,
                "Se revirtio " + t.getId() + " (" + t.getTipo() + " " + Moneda.pesos(t.getMonto()) + "), -" + t.getPuntos() + " pts",
                reloj.ahora());
        if (t.getTipo() == TipoTransaccion.TRANSFERENCIA) {
            notificador.notificar(destino.getClienteId(), TipoNotificacion.REVERSION,
                    "Se revirtio la transferencia " + t.getId() + " de " + Moneda.pesos(t.getMonto()) + " que recibiste",
                    reloj.ahora());
        }
        avisarSiCambioRango(cliente, antes);
        return t;
    }

    public Transaccion proximaReversible(String clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        return cliente.tieneReversibles() ? cliente.ultimaReversible() : null;
    }

    public void canjear(String clienteId, Beneficio beneficio) {
        Cliente cliente = buscarCliente(clienteId);
        if (cliente.getPuntosDisponibles() < beneficio.getCostoPuntos()) {
            throw new OperacionInvalidaException("Puntos insuficientes para " + beneficio + ": tiene "
                    + cliente.getPuntosDisponibles() + ", necesita " + beneficio.getCostoPuntos());
        }
        registroPuntos.canjear(cliente, beneficio, reloj.ahora());
        if (beneficio == Beneficio.BONO_SALDO) {
            Monedero principal = cliente.getMonederos().iterator().next();
            Transaccion t = crearTransaccion(TipoTransaccion.BONO, montoBono(), 0, null,
                    principal.getId(), null, "Canje de puntos");
            principal.aplicar(t);
            transacciones.agregar(t);
        }
        notificador.notificar(clienteId, TipoNotificacion.CANJE, "Canjeaste " + beneficio.getCostoPuntos()
                + " pts: " + beneficio.getDescripcion() + ". Te quedan " + cliente.getPuntosDisponibles(),
                reloj.ahora());
    }

    public Iterable<MovimientoPuntos> historialPuntos(String clienteId) {
        return buscarCliente(clienteId).getHistorialPuntos();
    }

    public Iterable<BeneficioActivo> beneficios(String clienteId) {
        return buscarCliente(clienteId).getBeneficios();
    }

    public Coleccion<Cliente> clientesDeRango(Rango rango) {
        return registroPuntos.clientesDeRango(rango);
    }

    public Coleccion<Cliente> ranking(int n) {
        return registroPuntos.ranking(n);
    }

    public long montoBono() {
        return calculadoraPuntos.pesos(Beneficio.UNIDADES_BONO);
    }

    public long umbralSaldoBajo() {
        return calculadoraPuntos.pesos(UNIDADES_SALDO_BAJO);
    }

    public CalculadoraPuntos getCalculadoraPuntos() {
        return calculadoraPuntos;
    }

    public RegistroPuntos getRegistroPuntos() {
        return registroPuntos;
    }

    public int cotizarPuntos(TipoTransaccion tipo, long monto, String monederoOrigenId) {
        int base = calculadoraPuntos.calcular(tipo, monto);
        return base + bonoPorRango(tipo, base, monederoOrigenId);
    }

    public long cotizarComision(TipoTransaccion tipo, long monto, String monederoOrigenId) {
        return calcularComision(tipo, monto, buscarMonedero(monederoOrigenId));
    }

    public long consultarSaldo(String monederoId) {
        return buscarMonedero(monederoId).getSaldo();
    }

    public Iterable<Transaccion> historial(String monederoId) {
        return buscarMonedero(monederoId).getHistorial();
    }

    public Iterable<Cliente> getClientes() {
        return clientes.valores();
    }

    public Iterable<Monedero> getMonederos() {
        return monederos.valores();
    }

    public Iterable<Transaccion> getTransacciones() {
        return transacciones;
    }

    public int cantidadTransacciones() {
        return transacciones.tamanio();
    }

    public ProgramadorTransacciones getProgramador() {
        return programador;
    }

    public int avanzarDias(int dias) {
        if (dias < 0) {
            throw new OperacionInvalidaException("No se puede retroceder el reloj: " + dias);
        }
        return programador.avanzarHasta(reloj.ahora().plusDays(dias));
    }

    public Notificador getNotificador() {
        return notificador;
    }

    public Reloj getReloj() {
        return reloj;
    }

    public String clienteQueGanaPuntos(Transaccion t) {
        String monederoId = t.getTipo().requiereOrigen() ? t.getOrigenId() : t.getDestinoId();
        return buscarMonedero(monederoId).getClienteId();
    }

    private long calcularComision(TipoTransaccion tipo, long monto, Monedero origen) {
        return politicaComision.calcular(tipo, monto, buscarCliente(origen.getClienteId()), reloj.ahora());
    }

    private Transaccion crearTransaccion(TipoTransaccion tipo, long monto, long comision, String origenId,
                                         String destinoId, Categoria categoria, String descripcion) {
        int base = calculadoraPuntos.calcular(tipo, monto);
        int bono = bonoPorRango(tipo, base, origenId);
        String id = String.format("T%04d", ++consecutivoTransaccion);
        return new Transaccion(id, tipo, monto, comision, origenId, destinoId, categoria,
                reloj.ahora(), descripcion, base + bono, bono);
    }

    private int bonoPorRango(TipoTransaccion tipo, int base, String origenId) {
        if (!tipo.recibeBonoPorRango() || origenId == null) {
            return 0;
        }
        return buscarCliente(buscarMonedero(origenId).getClienteId()).getRango().bonoSobre(base);
    }

    private Transaccion registrar(Transaccion t, String clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        Rango antes = cliente.getRango();
        transacciones.agregar(t);
        cliente.registrarReversible(t);
        registroPuntos.ganar(cliente, t.getPuntos(), t.getFecha(), t.getTipo() + " " + t.getId());
        avisarSiCambioRango(cliente, antes);
        return t;
    }

    private void avisarSiSaldoBajo(Monedero monedero) {
        if (monedero.getSaldo() < umbralSaldoBajo()) {
            notificador.notificar(monedero.getClienteId(), TipoNotificacion.SALDO_BAJO,
                    "Saldo bajo en " + monedero.getNombre() + " (" + monedero.getId() + "): " + Moneda.pesos(monedero.getSaldo()),
                    reloj.ahora());
        }
    }

    private void avisarSiCambioRango(Cliente cliente, Rango antes) {
        Rango ahora = cliente.getRango();
        if (ahora != antes) {
            notificador.notificar(cliente.getId(), TipoNotificacion.CAMBIO_RANGO,
                    (ahora.compareTo(antes) > 0 ? "Subiste" : "Bajaste") + " de " + antes + " a " + ahora
                            + " (" + cliente.getPuntos() + " pts)", reloj.ahora());
        }
    }

    static void validarMonto(long monto) {
        if (monto <= 0) {
            throw new OperacionInvalidaException("El monto debe ser mayor que cero: " + monto);
        }
        if (monto > MONTO_MAXIMO) {
            throw new OperacionInvalidaException("El monto maximo por transaccion es $1.000.000.000");
        }
    }

    private static void validarSaldo(Monedero monedero, long requerido) {
        if (monedero.getSaldo() < requerido) {
            throw new SaldoInsuficienteException(monedero.getId(), monedero.getSaldo(), requerido);
        }
    }
}
