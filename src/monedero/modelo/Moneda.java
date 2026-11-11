package monedero.modelo;

import java.util.Locale;

public final class Moneda {

    private static final Locale COLOMBIA = Locale.of("es", "CO");

    private Moneda() {
    }

    public static String pesos(long valor) {
        return (valor < 0 ? "-" : "") + String.format(COLOMBIA, "$%,d", Math.abs(valor));
    }
}
