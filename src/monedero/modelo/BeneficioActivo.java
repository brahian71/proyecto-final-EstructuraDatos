package monedero.modelo;

import java.time.LocalDateTime;
import java.util.Objects;

/** Beneficio canjeado que sigue vigente hasta la fecha vence (exclusiva). */
public class BeneficioActivo {

    private final Beneficio beneficio;
    private final LocalDateTime vence;

    public BeneficioActivo(Beneficio beneficio, LocalDateTime vence) {
        this.beneficio = Objects.requireNonNull(beneficio, "El beneficio no puede ser nulo");
        this.vence = Objects.requireNonNull(vence, "La fecha no puede ser nula");
    }

    public boolean vigenteEn(LocalDateTime fecha) {
        return fecha.isBefore(vence);
    }

    public Beneficio getBeneficio() {
        return beneficio;
    }

    public LocalDateTime getVence() {
        return vence;
    }

    @Override
    public String toString() {
        return beneficio + " hasta " + vence.toLocalDate();
    }
}
