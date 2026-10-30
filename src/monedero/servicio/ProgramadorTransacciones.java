package monedero.servicio;

import monedero.estructuras.Coleccion;
import monedero.estructuras.Mapa;
import monedero.estructuras.MonticuloBinario;
import monedero.estructuras.TablaHash;
import monedero.excepciones.NoEncontradoException;
import monedero.excepciones.OperacionInvalidaException;
import monedero.excepciones.SaldoInsuficienteException;
import monedero.modelo.Categoria;
import monedero.modelo.Frecuencia;
import monedero.modelo.Monedero;
import monedero.modelo.OperacionProgramada;
import monedero.modelo.TipoNotificacion;
import monedero.modelo.TipoTransaccion;

import java.time.LocalDateTime;

/**
 * Transacciones programadas y recurrentes con una COLA DE PRIORIDAD
 * (monticulo binario de minimos) ordenada por fecha de ejecucion.
 *
 * Simulacion por eventos: avanzarHasta(destino) saca de la cola la
 * operacion mas proxima mientras su fecha sea <= destino, mueve el reloj a
 * esa fecha exacta, la ejecuta y, si es recurrente, la vuelve a meter con
 * su siguiente fecha. Asi cada transaccion queda con la fecha en que debia
 * ocurrir y el historial sigue en orden cronologico.
 *
 * Si una ejecucion falla (saldo insuficiente, monedero inexistente) se avisa
 * al cliente, se cuenta el fallo y se sigue con la siguiente fecha.
 *
 * Costos: programar O(log n); procesar k ejecuciones O(k log n);
 * cancelar O(n) (hay que encontrarla en el arreglo del monticulo);
 * recordatorios O(n) (se revisan todas las pendientes).
 */
public class ProgramadorTransacciones {

    /** Se avisa cuando falta esto o menos para la ejecucion. */
    public static final int HORAS_RECORDATORIO = 24;

    private final SistemaMonedero sistema;
    private final MonticuloBinario<OperacionProgramada> cola = new MonticuloBinario<>(OperacionProgramada.POR_FECHA);
    private final Mapa<String, OperacionProgramada> porId = new TablaHash<>();
    private int consecutivo = 0;
    private long secuencia = 0;

    ProgramadorTransacciones(SistemaMonedero sistema) {
        this.sistema = sistema;
    }

    /**
     * @param origenId        null en depositos
     * @param destinoId       null en retiros
     * @param ejecuciones     cuantas veces (OperacionProgramada.SIN_LIMITE para siempre); UNA_VEZ ignora este valor
     */
    public OperacionProgramada programar(TipoTransaccion tipo, String origenId, String destinoId, long monto,
                                         Categoria categoria, String descripcion, Frecuencia frecuencia,
                                         LocalDateTime inicio, int ejecuciones) {
        if (monto <= 0) {
            throw new OperacionInvalidaException("El monto debe ser mayor que cero: " + monto);
        }
        if (inicio.isBefore(sistema.getReloj().ahora())) {
            throw new OperacionInvalidaException("La primera ejecucion no puede ser en el pasado: " + inicio);
        }
        // el duenio es quien paga (origen) o, en un deposito, quien recibe
        Monedero duenio = sistema.buscarMonedero(tipo == TipoTransaccion.DEPOSITO ? destinoId : origenId);
        if (tipo == TipoTransaccion.TRANSFERENCIA) {
            sistema.buscarMonedero(destinoId);
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

    /** @return cuantas ejecuciones se procesaron (exitosas o no). */
    public int avanzarHasta(LocalDateTime destino) {
        int procesadas = 0;
        while (!cola.estaVacia() && !cola.verPrimero().getProximaEjecucion().isAfter(destino)) {
            OperacionProgramada op = cola.extraer();
            // en un salto largo, los recordatorios salen en su momento (24 h antes), no al final
            sistema.getReloj().avanzarHasta(op.getProximaEjecucion().minusHours(HORAS_RECORDATORIO));
            enviarRecordatorios();
            sistema.getReloj().avanzarHasta(op.getProximaEjecucion());
            boolean exitosa = ejecutar(op);
            op.registrarEjecucion(exitosa);
            if (!op.terminada()) {
                cola.insertar(op); // vuelve con su nueva fecha
            }
            procesadas++;
        }
        sistema.getReloj().avanzarHasta(destino);
        enviarRecordatorios();
        return procesadas;
    }

    /** Pendientes de la mas proxima a la mas lejana (no modifica la cola). */
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

    /** Propiedad de monticulo (la usa la integridad). */
    public boolean colaValida() {
        return cola.esValido();
    }

    // ------------------------------------------------------------------ apoyo

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
        } catch (SaldoInsuficienteException | OperacionInvalidaException | NoEncontradoException e) {
            sistema.getNotificador().notificar(op.getClienteId(), TipoNotificacion.RECORDATORIO,
                    "No se pudo ejecutar " + op.getId() + " (" + op.getTipo() + " $" + op.getMonto() + "): "
                            + e.getMessage(), sistema.getReloj().ahora());
            return false;
        }
    }

    /** Avisa una vez por ejecucion cuando falta HORAS_RECORDATORIO o menos. */
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
                        "Manana se ejecuta " + op.getId() + ": " + op.getTipo() + " de $" + op.getMonto()
                                + " (" + proxima.toLocalDate() + ")", ahora);
                op.marcarRecordada();
            }
        }
    }
}
