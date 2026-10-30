package monedero.modelo;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

/**
 * Deposito, retiro o transferencia que se ejecuta sola en una fecha y, si
 * es recurrente, se vuelve a programar para la siguiente.
 *
 * Su prioridad en la cola es proximaEjecucion; si dos coinciden, sale
 * primero la que se programo antes (secuencia), asi el orden es estable.
 */
public class OperacionProgramada {

    /** Orden de la cola de prioridad: fecha mas cercana primero, luego la mas antigua. */
    public static final Comparator<OperacionProgramada> POR_FECHA =
            Comparator.comparing(OperacionProgramada::getProximaEjecucion)
                    .thenComparingLong(OperacionProgramada::getSecuencia);

    /** Ejecuciones sin limite (hasta que se cancele). */
    public static final int SIN_LIMITE = Integer.MAX_VALUE;

    private final String id;
    private final long secuencia;
    private final String clienteId;
    private final TipoTransaccion tipo;
    private final String origenId;
    private final String destinoId;
    private final long monto;
    private final Categoria categoria;
    private final String descripcion;
    private final Frecuencia frecuencia;
    private final LocalDateTime inicio;
    private final int ejecucionesTotales;

    private int ejecucionesHechas;
    private int fallos;
    private boolean cancelada;
    private boolean recordada;

    public OperacionProgramada(String id, long secuencia, String clienteId, TipoTransaccion tipo,
                               String origenId, String destinoId, long monto, Categoria categoria,
                               String descripcion, Frecuencia frecuencia, LocalDateTime inicio,
                               int ejecucionesTotales) {
        if (tipo != TipoTransaccion.DEPOSITO && tipo != TipoTransaccion.RETIRO && tipo != TipoTransaccion.TRANSFERENCIA) {
            throw new IllegalArgumentException("Solo se programan depositos, retiros y transferencias: " + tipo);
        }
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero: " + monto);
        }
        if (ejecucionesTotales <= 0) {
            throw new IllegalArgumentException("Debe ejecutarse al menos una vez: " + ejecucionesTotales);
        }
        this.id = Objects.requireNonNull(id);
        this.secuencia = secuencia;
        this.clienteId = Objects.requireNonNull(clienteId);
        this.tipo = tipo;
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.monto = monto;
        this.categoria = categoria;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.frecuencia = Objects.requireNonNull(frecuencia, "La frecuencia no puede ser nula");
        this.inicio = Objects.requireNonNull(inicio, "La fecha de inicio no puede ser nula");
        this.ejecucionesTotales = frecuencia == Frecuencia.UNA_VEZ ? 1 : ejecucionesTotales;
    }

    /**
     * Fecha de la siguiente ejecucion. No se guarda: se calcula desde inicio
     * y ejecucionesHechas. Por eso la operacion debe SACARSE de la cola antes
     * de registrar una ejecucion y volver a meterse despues (si no, la cola
     * quedaria con una prioridad vieja).
     */
    public LocalDateTime getProximaEjecucion() {
        return frecuencia.fechaNumero(inicio, ejecucionesHechas);
    }

    /** Cuenta una ejecucion (exitosa o fallida) y deja lista la siguiente. */
    public void registrarEjecucion(boolean exitosa) {
        ejecucionesHechas++;
        if (!exitosa) {
            fallos++;
        }
        recordada = false;
    }

    public boolean terminada() {
        return cancelada || ejecucionesHechas >= ejecucionesTotales;
    }

    public void cancelar() {
        cancelada = true;
    }

    public void marcarRecordada() {
        recordada = true;
    }

    public boolean isRecordada() {
        return recordada;
    }

    public String getId() {
        return id;
    }

    public long getSecuencia() {
        return secuencia;
    }

    public String getClienteId() {
        return clienteId;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public String getOrigenId() {
        return origenId;
    }

    public String getDestinoId() {
        return destinoId;
    }

    public long getMonto() {
        return monto;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Frecuencia getFrecuencia() {
        return frecuencia;
    }

    public int getEjecucionesHechas() {
        return ejecucionesHechas;
    }

    public int getEjecucionesTotales() {
        return ejecucionesTotales;
    }

    public int getFallos() {
        return fallos;
    }

    public boolean isCancelada() {
        return cancelada;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperacionProgramada otra)) {
            return false;
        }
        return id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        String restantes = ejecucionesTotales == SIN_LIMITE ? "sin limite"
                : (ejecucionesTotales - ejecucionesHechas) + " restantes";
        return id + " " + tipo + " $" + monto + " " + (origenId == null ? "-" : origenId) + " -> "
                + (destinoId == null ? "-" : destinoId) + " " + frecuencia + ", proxima "
                + getProximaEjecucion().toLocalDate() + " (" + restantes
                + (fallos > 0 ? ", " + fallos + " fallos" : "") + ")";
    }
}
