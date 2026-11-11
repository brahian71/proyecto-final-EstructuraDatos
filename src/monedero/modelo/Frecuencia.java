package monedero.modelo;

import java.time.LocalDateTime;

public enum Frecuencia {

    UNA_VEZ,
    DIARIA,
    SEMANAL,
    QUINCENAL,
    MENSUAL;

    public LocalDateTime fechaNumero(LocalDateTime inicio, int n) {
        return switch (this) {
            case UNA_VEZ -> inicio;
            case DIARIA -> inicio.plusDays(n);
            case SEMANAL -> inicio.plusWeeks(n);
            case QUINCENAL -> inicio.plusDays(15L * n);
            case MENSUAL -> inicio.plusMonths(n);
        };
    }
}
