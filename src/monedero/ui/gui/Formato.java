package monedero.ui.gui;

import monedero.modelo.Rango;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

final class Formato {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static final Color PRIMARIO = Estilo.PRIMARIO;
    static final Color FONDO_TARJETA = new Color(0xEFF6FF);
    static final Color BORDE = Estilo.BORDE;
    static final Color EXITO = Estilo.EXITO;
    static final Color ERROR = Estilo.ERROR;
    static final Color APAGADO = Estilo.TEXTO_SUAVE;

    private Formato() {
    }

    static String pesos(long valor) {
        return monedero.modelo.Moneda.pesos(valor);
    }

    static String fecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }

    static String fechaHora(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA_HORA);
    }

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

    static String idDe(Object etiqueta) {
        if (etiqueta == null) {
            throw new IllegalArgumentException("Seleccione una opcion");
        }
        String texto = etiqueta.toString();
        int espacio = texto.indexOf(' ');
        return espacio < 0 ? texto : texto.substring(0, espacio);
    }
}
