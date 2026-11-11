package monedero.modelo;

public enum TipoTransaccion {

    DEPOSITO(1),
    RETIRO(2),
    TRANSFERENCIA(3),
    MOVIMIENTO_INTERNO(0),
    BONO(0);

    private final int puntosPorCada100;

    TipoTransaccion(int puntosPorCada100) {
        this.puntosPorCada100 = puntosPorCada100;
    }

    public int getPuntosPorCada100() {
        return puntosPorCada100;
    }

    public boolean requiereOrigen() {
        return this != DEPOSITO && this != BONO;
    }

    public boolean requiereDestino() {
        return this != RETIRO;
    }

    public boolean recibeBonoPorRango() {
        return this == RETIRO || this == TRANSFERENCIA;
    }
}
