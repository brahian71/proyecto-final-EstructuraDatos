package monedero.servicio;

import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;
import monedero.estructuras.Mapa;
import monedero.estructuras.MapaJava;
import monedero.excepciones.NoEncontradoException;
import monedero.excepciones.OperacionInvalidaException;
import monedero.excepciones.SaldoInsuficienteException;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.TipoMonedero;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Transaccion;

/**
 * Fachada del monedero: la interfaz (consola o grafica) solo habla con esta
 * clase. Funcionalidades basicas: depositos, retiros, transferencias,
 * saldo e historial, mas el calculo de puntos.
 *
 * Estructuras:
 *   clientes, monederos  Mapa (hash)   buscar por id O(1) promedio
 *   transacciones        ListaEnlazada registro global en orden, para
 *                                      reportes y grafos (fase 5)
 */
public class SistemaMonedero {

    private final Mapa<String, Cliente> clientes = new MapaJava<>();
    private final Mapa<String, Monedero> monederos = new MapaJava<>();
    private final Coleccion<Transaccion> transacciones = new ListaEnlazada<>();
    private final Reloj reloj;

    private int consecutivoCliente = 0;
    private int consecutivoMonedero = 0;
    private int consecutivoTransaccion = 0;

    public SistemaMonedero() {
        this(new Reloj());
    }

    public SistemaMonedero(Reloj reloj) {
        if (reloj == null) {
            throw new IllegalArgumentException("El reloj no puede ser nulo");
        }
        this.reloj = reloj;
    }

    // ------------------------------------------------------------------ clientes y monederos

    /** Crea el cliente con un monedero PRINCIPAL. */
    public Cliente registrarCliente(String nombre) {
        Cliente cliente = new Cliente(String.format("C%03d", ++consecutivoCliente), nombre);
        clientes.poner(cliente.getId(), cliente);
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
        Transaccion t = crearTransaccion(TipoTransaccion.DEPOSITO, monto, null, destino.getId(), null, descripcion);
        destino.aplicar(t);
        return registrar(t, destino.getClienteId());
    }

    public Transaccion retirar(String monederoId, long monto, Categoria categoria, String descripcion) {
        Monedero origen = buscarMonedero(monederoId);
        validarMonto(monto);
        long comision = calcularComision(TipoTransaccion.RETIRO, monto);
        validarSaldo(origen, monto + comision);
        Transaccion t = crearTransaccion(TipoTransaccion.RETIRO, monto, origen.getId(), null, categoria, descripcion);
        origen.aplicar(t);
        return registrar(t, origen.getClienteId());
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
        long comision = calcularComision(tipo, monto);
        validarSaldo(origen, monto + comision);

        Transaccion t = crearTransaccion(tipo, monto, origen.getId(), destino.getId(), categoria, descripcion);
        origen.aplicar(t);
        destino.aplicar(t);
        return registrar(t, origen.getClienteId());
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

    public Reloj getReloj() {
        return reloj;
    }

    /**
     * Cliente que gana los puntos de una transaccion: el dueno del destino en
     * un deposito, el del origen en el resto. Lo usa tambien la integridad.
     */
    public String clienteQueGanaPuntos(Transaccion t) {
        String monederoId = t.getTipo() == TipoTransaccion.DEPOSITO ? t.getDestinoId() : t.getOrigenId();
        return buscarMonedero(monederoId).getClienteId();
    }

    // ------------------------------------------------------------------ apoyo

    /**
     * PENDIENTE (pregunta al profesor): valor de las comisiones. Mientras
     * tanto es 0; el modelo ya las soporta (Transaccion.comision) para que
     * los beneficios del canje (10% menos de comision, retiros sin cargo)
     * se conecten aqui sin cambiar nada mas.
     */
    long calcularComision(TipoTransaccion tipo, long monto) {
        return 0;
    }

    private Transaccion crearTransaccion(TipoTransaccion tipo, long monto, String origenId, String destinoId,
                                         Categoria categoria, String descripcion) {
        int puntos = CalculadoraPuntos.calcular(tipo, monto);
        long comision = calcularComision(tipo, monto);
        String id = String.format("T%04d", ++consecutivoTransaccion);
        return new Transaccion(id, tipo, monto, comision, origenId, destinoId, categoria,
                reloj.ahora(), descripcion, puntos);
    }

    private Transaccion registrar(Transaccion t, String clienteId) {
        transacciones.agregar(t);
        buscarCliente(clienteId).sumarPuntos(t.getPuntos());
        return t;
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
