package monedero.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/** Aviso para un cliente. Lo unico que cambia despues de creada es si ya se leyo. */
public class Notificacion {

    private final TipoNotificacion tipo;
    private final String mensaje;
    private final LocalDateTime fecha;
    private boolean leida;

    public Notificacion(TipoNotificacion tipo, String mensaje, LocalDateTime fecha) {
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede ser nulo ni vacio");
        }
        this.tipo = Objects.requireNonNull(tipo, "El tipo no puede ser nulo");
        this.mensaje = mensaje.trim();
        this.fecha = Objects.requireNonNull(fecha, "La fecha no puede ser nula");
    }

    public void marcarLeida() {
        leida = true;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean isLeida() {
        return leida;
    }

    @Override
    public String toString() {
        return (leida ? "  " : "* ") + fecha.toLocalDate() + " [" + tipo + "] " + mensaje;
    }
}
