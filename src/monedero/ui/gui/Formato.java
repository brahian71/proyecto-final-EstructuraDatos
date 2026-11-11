package monedero.ui.gui;

import monedero.modelo.Rango;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formatos y colores compartidos por toda la interfaz. */
final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static final Color PRIMARIO = new Color(0x1F4E79);
    static final Color FONDO_TARJETA = new Color(0xF4F7FB);
    static final Color BORDE = new Color(0xC9D3DF);
    static final Color EXITO = new Color(0x2E7D32);
    static final Color ERROR = new Color(0xC62828);
    static final Color APAGADO = new Color(0x8A8F98);

    private Formato() {
    }

    /** 12345 -> "$12.345" (separador de miles colombiano). */
    static String pesos(long valor) {
        return monedero.modelo.Moneda.pesos(valor);
    }

    static String fecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA_HORA);
    }

    /**
     * Lee un monto entero en pesos, con o sin puntos de miles: "12500",
     * "12.500", "$1.250.000". Rechaza decimales ("12.5" o "12,50"): con
     * puntos de miles, "12.5" se leeria como 125 sin que el usuario lo note.
     */
    static long leerMonto(String texto) {
        String limpio = texto == null ? "" : texto.replace("$", "").replace(" ", "").trim();
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("Escriba un monto");
        }
        if (!limpio.matches("\\d{1,3}(\\.\\d{3})+|\\d+")) {
            throw new IllegalArgumentException("Monto no valido: " + texto.trim()
                    + ". Use pesos enteros, por ejemplo 12500 o 12.500");
        }
        try {
            return Long.parseLong(limpio.replace(".", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Monto demasiado grande: " + texto.trim());
        }
    }

    static Color colorRango(Rango rango) {
        return switch (rango) {
            case BRONCE -> new Color(0xA0522D);
            case PLATA -> new Color(0x708090);
            case ORO -> new Color(0xB8860B);
            case PLATINO -> new Color(0x4B6584);
        };
    }

    /** El id es lo que va antes del primer espacio en las etiquetas de los combos ("M001 Principal ..."). */
    static String idDe(Object etiqueta) {
        if (etiqueta == null) {
            throw new IllegalArgumentException("Seleccione una opcion");
        }
        String texto = etiqueta.toString();
        int espacio = texto.indexOf(' ');
        return espacio < 0 ? texto : texto.substring(0, espacio);
    }
}
