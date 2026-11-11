package monedero.servicio;

import monedero.estructuras.Coleccion;
import monedero.estructuras.Mapa;
import monedero.estructuras.MonticuloBinario;
import monedero.estructuras.TablaHash;
import monedero.excepciones.NoEncontradoException;
import monedero.excepciones.OperacionInvalidaException;
import monedero.modelo.Categoria;
import monedero.modelo.Frecuencia;
import monedero.modelo.Moneda;
import monedero.modelo.Monedero;
import monedero.modelo.OperacionProgramada;
import monedero.modelo.TipoNotificacion;
import monedero.modelo.TipoTransaccion;

import java.time.LocalDateTime;

public class ProgramadorTransacciones {

    public static final int HORAS_RECORDATORIO = 24;

    private final SistemaMonedero sistema;
    private final MonticuloBinario<OperacionProgramada> cola = new MonticuloBinario<>(OperacionProgramada.POR_FECHA);
    private final Mapa<String, OperacionProgramada> porId = new TablaHash<>();
    private int consecutivo = 0;
    private long secuencia = 0;

    ProgramadorTransacciones(SistemaMonedero sistema) {
        this.sistema = sistema;
    }

    public OperacionProgramada programar(TipoTransaccion tipo, String origenId, String destinoId, long monto,
                                         Categoria categoria, String descripcion, Frecuencia frecuencia,
                                         LocalDateTime inicio, int ejecuciones) {
        SistemaMonedero.validarMonto(monto);
        if (inicio == null) {
            throw new OperacionInvalidaException("Falta la fecha de la primera ejecucion");
        }
        if (inicio.isBefore(sistema.getReloj().ahora())) {
            throw new OperacionInvalidaException("La primera ejecucion no puede ser en el pasado: " + inicio);
        }
        Monedero duenio = sistema.buscarMonedero(tipo == TipoTransaccion.DEPOSITO ? destinoId : origenId);
        if (tipo == TipoTransaccion.TRANSFERENCIA) {
            sistema.buscarMonedero(destinoId);
            if (origenId.equals(destinoId)) {
                throw new OperacionInvalidaException("No se puede programar una transferencia al mismo monedero");
            }
        }
        OperacionProgramada op = new OperacionProgramada(String.format("P%03d", ++consecutivo), ++secuencia,
                duenio.getClienteId(), tipo, tipo == TipoTransaccion.DEPOSITO ? null : origenId,
                tipo == TipoTransaccion.RETIRO ? null : destinoId, monto, categoria, descripcion,
                frecuencia, inicio, ejecuciones);
        cola.insertar(op);
        porId.poner(op.getId(), op);
        enviarRecordatorios();
        return op;
    }

    public void cancelar(String id) {
        OperacionProgramada op = buscar(id);
        if (op.terminada()) {
            throw new OperacionInvalidaException("La operacion " + id + " ya termino o fue cancelada");
        }
        cola.eliminar(op);
        op.cancelar();
    }

    public int avanzarHasta(LocalDateTime destino) {
        int procesadas = 0;
        while (!cola.estaVacia() && !cola.verPrimero().getProximaEjecucion().isAfter(destino)) {
            OperacionProgramada op = cola.extraer();
            sistema.getReloj().avanzarHasta(op.getProximaEjecucion().minusHours(HORAS_RECORDATORIO));
            enviarRecordatorios();
            sistema.getReloj().avanzarHasta(op.getProximaEjecucion());
            boolean exitosa = ejecutar(op);
            op.registrarEjecucion(exitosa);
            if (!op.terminada()) {
                cola.insertar(op);
            }
            procesadas++;
        }
        sistema.getReloj().avanzarHasta(destino);
        enviarRecordatorios();
        return procesadas;
    }

    public Coleccion<OperacionProgramada> pendientes() {
        return cola.ordenados();
    }

    public OperacionProgramada buscar(String id) {
        OperacionProgramada op = porId.obtener(id);
        if (op == null) {
            throw new NoEncontradoException("Operacion programada", id);
        }
        return op;
    }

    public int cantidadPendientes() {
        return cola.tamanio();
    }

    public boolean colaConsistente() {
        int activas = 0;
        for (OperacionProgramada op : porId.valores()) {
            if (!op.terminada()) {
                activas++;
            }
        }
        return activas == cola.tamanio();
    }

    public boolean colaValida() {
        return cola.esValido();
    }

    private boolean ejecutar(OperacionProgramada op) {
        String descripcion = "Programada " + op.getId() + (op.getDescripcion().isEmpty() ? "" : ": " + op.getDescripcion());
        try {
            switch (op.getTipo()) {
                case DEPOSITO -> sistema.depositar(op.getDestinoId(), op.getMonto(), descripcion);
                case RETIRO -> sistema.retirar(op.getOrigenId(), op.getMonto(), op.getCategoria(), descripcion);
                case TRANSFERENCIA -> sistema.transferir(op.getOrigenId(), op.getDestinoId(), op.getMonto(),
                        op.getCategoria(), descripcion);
                default -> throw new IllegalStateException("Tipo no programable: " + op.getTipo());
            }
            return true;
        } catch (RuntimeException e) {
            sistema.getNotificador().notificar(op.getClienteId(), TipoNotificacion.FALLO_PROGRAMADA,
                    "No se pudo ejecutar " + op.getId() + " (" + op.getTipo() + " " + Moneda.pesos(op.getMonto()) + "): "
                            + e.getMessage(), sistema.getReloj().ahora());
            return false;
        }
    }

    private void enviarRecordatorios() {
        LocalDateTime ahora = sistema.getReloj().ahora();
        LocalDateTime limite = ahora.plusHours(HORAS_RECORDATORIO);
        for (OperacionProgramada op : porId.valores()) {
            if (op.terminada() || op.isRecordada()) {
                continue;
            }
            LocalDateTime proxima = op.getProximaEjecucion();
            if (proxima.isAfter(ahora) && !proxima.isAfter(limite)) {
                sistema.getNotificador().notificar(op.getClienteId(), TipoNotificacion.RECORDATORIO,
                        "Manana se ejecuta " + op.getId() + ": " + op.getTipo() + " de " + Moneda.pesos(op.getMonto())
                                + " (" + proxima.toLocalDate() + ")", ahora);
                op.marcarRecordada();
            }
        }
    }
}
