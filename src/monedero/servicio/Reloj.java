package monedero.servicio;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Reloj simulado. Todo el sistema pide la hora aqui en vez de a
 * LocalDateTime.now(), asi las transacciones programadas (fase 4) se pueden
 * probar y mostrar con "avanzar dias" sin esperar tiempo real.
 */
public class Reloj {

    private LocalDateTime actual;

    public Reloj() {
        this(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
    }

    public Reloj(LocalDateTime inicio) {
        if (inicio == null) {
            throw new IllegalArgumentException("La fecha inicial no puede ser nula");
        }
        this.actual = inicio;
    }

    public LocalDateTime ahora() {
        return actual;
    }

    public void avanzarDias(int dias) {
        if (dias < 0) {
            throw new IllegalArgumentException("No se puede retroceder el reloj: " + dias);
        }
        actual = actual.plusDays(dias);
    }

    /** Mueve el reloj a la fecha indicada (si ya paso, no hace nada: nunca retrocede). */
    public void avanzarHasta(LocalDateTime fecha) {
        if (fecha.isAfter(actual)) {
            actual = fecha;
        }
    }

    public void avanzarMinutos(long minutos) {
        if (minutos < 0) {
            throw new IllegalArgumentException("No se puede retroceder el reloj: " + minutos);
        }
        actual = actual.plusMinutes(minutos);
    }
}
