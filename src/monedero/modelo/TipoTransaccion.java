package monedero.modelo;

// MOVIMIENTO_INTERNO no da puntos: si no, se moveria plata entre monederos propios solo por puntos
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

    // el bono premia usar el dinero (retiro, transferencia), no guardarlo
    public boolean recibeBonoPorRango() {
        return this == RETIRO || this == TRANSFERENCIA;
    }
}
