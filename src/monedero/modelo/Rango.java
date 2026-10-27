package monedero.modelo;

/**
 * Rangos segun puntos acumulados (limites del enunciado, inclusivos):
 *   Bronce 0-500, Plata 501-1000, Oro 1001-5000, Platino mas de 5000.
 *
 * desdePuntos es la funcion de calculo directa O(1). El arbol balanceado
 * (fase 3) se usa para tener a los clientes ordenados y responder consultas
 * como "todos los clientes Oro", no para calcular el rango de uno solo.
 */
public enum Rango {

    BRONCE(0, 500),
    PLATA(501, 1000),
    ORO(1001, 5000),
    PLATINO(5001, Integer.MAX_VALUE);

    private final int puntosMinimos;
    private final int puntosMaximos;

    Rango(int puntosMinimos, int puntosMaximos) {
        this.puntosMinimos = puntosMinimos;
        this.puntosMaximos = puntosMaximos;
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
}
