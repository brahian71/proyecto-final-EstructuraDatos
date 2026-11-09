package monedero.modelo;

/**
 * Rangos segun puntos acumulados (limites del enunciado, inclusivos):
 *   Bronce 0-500, Plata 501-1000, Oro 1001-5000, Platino mas de 5000.
 *
 * Beneficios del rango:
 *   descuentoTransferencia  % menos en la comision de las transferencias
 *   bonoPuntos              % de puntos extra en retiros y transferencias
 *                           ("bonos de puntos adicionales por ciertas transacciones")
 *
 * desdePuntos es la funcion de calculo directa O(1). El arbol balanceado
 * (fase 3) se usa para tener a los clientes ordenados y responder consultas
 * como "todos los clientes Oro", no para calcular el rango de uno solo.
 */
public enum Rango {

    BRONCE(0, 500, 0, 0),
    PLATA(501, 1000, 10, 10),
    ORO(1001, 5000, 20, 20),
    PLATINO(5001, Integer.MAX_VALUE, 30, 30);

    private final int puntosMinimos;
    private final int puntosMaximos;
    private final int descuentoTransferencia;
    private final int bonoPuntos;

    Rango(int puntosMinimos, int puntosMaximos, int descuentoTransferencia, int bonoPuntos) {
        this.puntosMinimos = puntosMinimos;
        this.puntosMaximos = puntosMaximos;
        this.descuentoTransferencia = descuentoTransferencia;
        this.bonoPuntos = bonoPuntos;
    }

    public static Rango desdePuntos(int puntos) {
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos no pueden ser negativos: " + puntos);
        }
        for (Rango rango : values()) {
            if (puntos <= rango.puntosMaximos) {
                return rango;
            }
        }
        return PLATINO;
    }

    public int getPuntosMinimos() {
        return puntosMinimos;
    }

    public int getPuntosMaximos() {
        return puntosMaximos;
    }

    public int getDescuentoTransferencia() {
        return descuentoTransferencia;
    }

    public int getBonoPuntos() {
        return bonoPuntos;
    }

    /** Puntos extra sobre los puntos base (division entera: 36 base en Plata = 3 extra). */
    public int bonoSobre(int puntosBase) {
        return Math.toIntExact((long) puntosBase * bonoPuntos / 100); // en long para no desbordar
    }
}
