package monedero.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro inmutable de una operacion. Una transferencia es UN solo objeto
 * que queda en el historial de los dos monederos; efectoSobre() dice cuanto
 * cambia el saldo de cada uno.
 *
 * Dinero en long (unidades enteras): con double los redondeos harian fallar
 * la verificacion de integridad.
 *
 * Los puntos se calculan una vez y se guardan aqui, asi al revertir se
 * descuentan exactamente los mismos aunque cambien las reglas o el rango.
 */
public class Transaccion {

    private final String id;
    private final TipoTransaccion tipo;
    private final long monto;
    private final long comision;
    private final String origenId;
    private final String destinoId;
    private final Categoria categoria;
    private final LocalDateTime fecha;
    private final String descripcion;
    private final int puntos;

    public Transaccion(String id, TipoTransaccion tipo, long monto, long comision,
                       String origenId, String destinoId, Categoria categoria,
                       LocalDateTime fecha, String descripcion, int puntos) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id no puede ser nulo ni vacio");
        }
        Objects.requireNonNull(tipo, "El tipo no puede ser nulo");
        Objects.requireNonNull(fecha, "La fecha no puede ser nula");
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero: " + monto);
        }
        if (comision < 0) {
            throw new IllegalArgumentException("La comision no puede ser negativa: " + comision);
        }
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos no pueden ser negativos: " + puntos);
        }
        if (tipo.requiereOrigen() != (origenId != null)) {
            throw new IllegalArgumentException(tipo + (tipo.requiereOrigen() ? " necesita" : " no lleva") + " monedero origen");
        }
        if (tipo.requiereDestino() != (destinoId != null)) {
            throw new IllegalArgumentException(tipo + (tipo.requiereDestino() ? " necesita" : " no lleva") + " monedero destino");
        }
        if (origenId != null && origenId.equals(destinoId)) {
            throw new IllegalArgumentException("Origen y destino no pueden ser el mismo monedero");
        }

        this.id = id;
        this.tipo = tipo;
        this.monto = monto;
        this.comision = comision;
        this.origenId = origenId;
        this.destinoId = destinoId;
        this.categoria = categoria == null ? Categoria.OTROS : categoria;
        this.fecha = fecha;
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.puntos = puntos;
    }

    /**
     * Cambio de saldo que produce esta transaccion en el monedero indicado:
     * el origen pierde monto + comision, el destino gana monto, otro 0.
     */
    public long efectoSobre(String monederoId) {
        if (monederoId == null) {
            return 0;
        }
        if (monederoId.equals(origenId)) {
            return -(monto + comision);
        }
        if (monederoId.equals(destinoId)) {
            return monto;
        }
        return 0;
    }

    public boolean involucra(String monederoId) {
        return monederoId != null && (monederoId.equals(origenId) || monederoId.equals(destinoId));
    }

    public String getId() {
        return id;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public long getMonto() {
        return monto;
    }

    public long getComision() {
        return comision;
    }

    public String getOrigenId() {
        return origenId;
    }

    public String getDestinoId() {
        return destinoId;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getPuntos() {
        return puntos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transaccion otra)) {
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
        return id + " " + tipo + " $" + monto
                + (comision > 0 ? " (+com $" + comision + ")" : "")
                + " " + (origenId == null ? "-" : origenId) + " -> " + (destinoId == null ? "-" : destinoId)
                + " [" + categoria + "] +" + puntos + "pts " + fecha.toLocalDate();
    }
}
