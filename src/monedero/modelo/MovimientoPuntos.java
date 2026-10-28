package monedero.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Una entrada del historial de puntos de un cliente. cantidad es positiva
 * al ganar y negativa al revertir o canjear, asi la suma del historial es
 * igual a los puntos disponibles (lo comprueba la integridad).
 */
public class MovimientoPuntos {

    public enum Tipo {
        GANADOS,
        REVERTIDOS,
        CANJEADOS
    }

    private final Tipo tipo;
    private final int cantidad;
    private final LocalDateTime fecha;
    private final String detalle;
    private final int disponiblesDespues;

    public MovimientoPuntos(Tipo tipo, int cantidad, LocalDateTime fecha, String detalle, int disponiblesDespues) {
        this.tipo = Objects.requireNonNull(tipo, "El tipo no puede ser nulo");
        if ((tipo == Tipo.GANADOS) != (cantidad >= 0)) {
            throw new IllegalArgumentException(tipo + " no admite la cantidad " + cantidad);
        }
        this.cantidad = cantidad;
        this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser nula");
        this.detalle = detalle == null ? "" : detalle;
        this.disponiblesDespues = disponiblesDespues;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getCantidad() {
        return cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getDetalle() {
        return detalle;
    }

    public int getDisponiblesDespues() {
        return disponiblesDespues;
    }

    @Override
    public String toString() {
        return fecha.toLocalDate() + " " + tipo + " " + (cantidad >= 0 ? "+" : "") + cantidad
                + " (" + detalle + ") -> " + disponiblesDespues + " disponibles";
    }
}
