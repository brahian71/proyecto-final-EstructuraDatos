package monedero.modelo;

import java.util.Locale;

/** Formato de los montos en pesos colombianos para mensajes y notificaciones. */
public final class Moneda {

    private static final Locale COLOMBIA = Locale.of("es", "CO");

    private Moneda() {
    }

    /** 1250000 -> "$1.250.000"; -500 -> "-$500". */
    public static String pesos(long valor) {
        return (valor < 0 ? "-" : "") + String.format(COLOMBIA, "$%,d", Math.abs(valor));
    }
}
