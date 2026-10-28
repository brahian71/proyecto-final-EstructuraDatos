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
import monedero.modelo.Monedero;
import monedero.modelo.MovimientoPuntos;
import monedero.modelo.Rango;
import monedero.modelo.TipoNotificacion;
import monedero.modelo.TipoMonedero;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Transaccion;

/**
 * Fachada del monedero: la interfaz (consola o grafica) solo habla con esta
 * clase. Funcionalidades basicas: depositos, retiros, transferencias,
 * saldo e historial, mas puntos y rangos, comisiones, canje, la reversion
 * (pila por cliente) y las notificaciones (lista circular por cliente).
 *
 * Estructuras:
 *   clientes, monederos  TablaHash      buscar por id O(1) promedio
 *   registroPuntos       ArbolAVL       clientes ordenados por puntos
 *   transacciones        ListaEnlazada  registro global en orden, para
 *                                       reportes y grafos (fase 5)
 */
public class SistemaMonedero {

    /** Por debajo de este saldo, despues de un retiro o envio, se avisa. */
    public static final long UMBRAL_SALDO_BAJO = 1_000;

    private final Mapa<String, Cliente> clientes = new TablaHash<>();
    private final Mapa<String, Monedero> monederos = new TablaHash<>();
    private final Coleccion<Transaccion> transacciones = new ListaEnlazada<>();
    private final RegistroPuntos registroPuntos = new RegistroPuntos();
    private final Notificador notificador = new Notificador();
    private final PoliticaComision politicaComision;
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
        if (reloj == null || politicaComision == null) {
            throw new IllegalArgumentException("El reloj y la politica de comision no pueden ser nulos");
        }
        this.reloj = reloj;
        this.politicaComision = politicaComision;
    }

    // ------------------------------------------------------------------ clientes y monederos

    /** Crea el cliente con un monedero PRINCIPAL. */
    public Cliente registrarCliente(String nombre) {
        Cliente cliente = new Cliente(String.format("C%03d", ++consecutivoCliente), nombre);
        clientes.poner(cliente.getId(), cliente);
        registroPuntos.agregar(cliente);
        crearMonedero(cliente.getId(), "Principal", TipoMonedero.PRINCIPAL);
        return cliente;
    }

    public Monedero crearMonedero(String clienteId, String nombre, TipoMonedero tipo) {
        Cliente cliente = buscarCliente(clienteId);
        Monedero monedero = new Monedero(String.format("M%03d", ++consecutivoMonedero), clienteId, nombre, tipo);
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

    // ------------------------------------------------------------------ operaciones basicas

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

    /**
     * Si ambos monederos son del mismo cliente queda como MOVIMIENTO_INTERNO
     * (sin puntos); si son de clientes distintos es TRANSFERENCIA.
     */
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
                    "Recibiste $" + monto + " de " + buscarCliente(origen.getClienteId()).getNombre()
                            + " en " + destino.getNombre(), reloj.ahora());
        }
        return t;
    }

    // ------------------------------------------------------------------ reversion

    /**
     * Revierte la transaccion mas reciente que inicio el cliente (cima de su
     * pila) y recalcula sus puntos. La transaccion no se borra: queda en el
     * historial marcada como REVERTIDA y deja de contar en saldos y puntos.
     *
     * Se rechaza si el monedero que recibio la plata ya la gasto en algun
     * momento posterior (ver Monedero.saldoMinimoDesde): quitarla dejaria un
     * saldo negativo en la historia.
     *
     * Costo: O(1) la pila + O(n) revisar el historial del destino.
     */
    public Transaccion revertirUltima(String clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        if (!cliente.tieneReversibles()) {
            throw new OperacionInvalidaException(clienteId + " no tiene transacciones para revertir");
        }
        Transaccion t = cliente.ultimaReversible();

        Monedero origen = t.getOrigenId() == null ? null : buscarMonedero(t.getOrigenId());
        Monedero destino = t.getDestinoId() == null ? null : buscarMonedero(t.getDestinoId());
        if (destino != null && destino.saldoMinimoDesde(t) < t.getMonto()) {
            throw new OperacionInvalidaException("No se puede revertir " + t.getId() + ": los $" + t.getMonto()
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
                "Se revirtio " + t.getId() + " (" + t.getTipo() + " $" + t.getMonto() + "), -" + t.getPuntos() + " pts",
                reloj.ahora());
        if (t.getTipo() == TipoTransaccion.TRANSFERENCIA) {
            notificador.notificar(destino.getClienteId(), TipoNotificacion.REVERSION,
                    "Se revirtio la transferencia " + t.getId() + " de $" + t.getMonto() + " que recibiste",
                    reloj.ahora());
        }
        avisarSiCambioRango(cliente, antes);
        return t;
    }

    /** La que se revertiria, o null si no hay ninguna. */
    public Transaccion proximaReversible(String clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        return cliente.tieneReversibles() ? cliente.ultimaReversible() : null;
    }

    // ------------------------------------------------------------------ puntos y canje

    /**
     * Canjea puntos disponibles por un beneficio. El bono se deposita en el
     * monedero principal como transaccion BONO (no da puntos, no se revierte).
     */
    public void canjear(String clienteId, Beneficio beneficio) {
        Cliente cliente = buscarCliente(clienteId);
        if (cliente.getPuntosDisponibles() < beneficio.getCostoPuntos()) {
            throw new OperacionInvalidaException("Puntos insuficientes para " + beneficio + ": tiene "
                    + cliente.getPuntosDisponibles() + ", necesita " + beneficio.getCostoPuntos());
        }
        registroPuntos.canjear(cliente, beneficio, reloj.ahora());
        if (beneficio == Beneficio.BONO_SALDO) {
            Monedero principal = cliente.getMonederos().iterator().next();
            Transaccion t = crearTransaccion(TipoTransaccion.BONO, Beneficio.MONTO_BONO, 0, null,
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

    public RegistroPuntos getRegistroPuntos() {
        return registroPuntos;
    }

    /** Comision que se cobraria hoy; la interfaz la muestra antes de confirmar. */
    public long cotizarComision(TipoTransaccion tipo, long monto, String monederoOrigenId) {
        return calcularComision(tipo, monto, buscarMonedero(monederoOrigenId));
    }

    // ------------------------------------------------------------------ consultas

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

    public Notificador getNotificador() {
        return notificador;
    }

    public Reloj getReloj() {
        return reloj;
    }

    /**
     * Cliente que gana los puntos de una transaccion: el dueno del destino en
     * un deposito, el del origen en el resto. Lo usa tambien la integridad.
     */
    public String clienteQueGanaPuntos(Transaccion t) {
        String monederoId = t.getTipo().requiereOrigen() ? t.getOrigenId() : t.getDestinoId();
        return buscarMonedero(monederoId).getClienteId();
    }

    // ------------------------------------------------------------------ apoyo

    /** La paga el dueno del monedero origen, con su rango y beneficios de hoy. */
    private long calcularComision(TipoTransaccion tipo, long monto, Monedero origen) {
        return politicaComision.calcular(tipo, monto, buscarCliente(origen.getClienteId()), reloj.ahora());
    }

    private Transaccion crearTransaccion(TipoTransaccion tipo, long monto, long comision, String origenId,
                                         String destinoId, Categoria categoria, String descripcion) {
        int puntos = CalculadoraPuntos.calcular(tipo, monto);
        String id = String.format("T%04d", ++consecutivoTransaccion);
        return new Transaccion(id, tipo, monto, comision, origenId, destinoId, categoria,
                reloj.ahora(), descripcion, puntos);
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
        if (monedero.getSaldo() < UMBRAL_SALDO_BAJO) {
            notificador.notificar(monedero.getClienteId(), TipoNotificacion.SALDO_BAJO,
                    "Saldo bajo en " + monedero.getNombre() + " (" + monedero.getId() + "): $" + monedero.getSaldo(),
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

    private static void validarMonto(long monto) {
        if (monto <= 0) {
            throw new OperacionInvalidaException("El monto debe ser mayor que cero: " + monto);
        }
    }

    private static void validarSaldo(Monedero monedero, long requerido) {
        if (monedero.getSaldo() < requerido) {
            throw new SaldoInsuficienteException(monedero.getId(), monedero.getSaldo(), requerido);
        }
    }
}
