package monedero.modelo;

import java.time.LocalDateTime;

/**
 * Cada cuanto se repite una operacion programada.
 *
 * fechaNumero calcula la ejecucion n (0 = la primera) siempre desde la
 * fecha inicial, no sumando a la anterior: asi una mensual que empieza el 31
 * cae el 30 de abril y vuelve al 31 en mayo, en vez de quedarse corrida.
 */
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
