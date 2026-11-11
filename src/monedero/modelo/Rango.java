package monedero.modelo;

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

    public int bonoSobre(int puntosBase) {
        return Math.toIntExact((long) puntosBase * bonoPuntos / 100);
    }
}
