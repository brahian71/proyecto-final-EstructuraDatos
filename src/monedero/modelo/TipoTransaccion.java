package monedero.modelo;

/**
 * Tipos de transaccion y los puntos que da cada uno por cada 100 unidades.
 *
 * MOVIMIENTO_INTERNO es pasar plata entre monederos del mismo cliente
 * (ahorros -> gastos). No da puntos: si diera, un cliente podria mover la
 * misma plata de un lado a otro solo para sumar puntos.
 */
public enum TipoTransaccion {

    DEPOSITO(1),
    RETIRO(2),
    TRANSFERENCIA(3),
    MOVIMIENTO_INTERNO(0);

    private final int puntosPorCada100;

    TipoTransaccion(int puntosPorCada100) {
        this.puntosPorCada100 = puntosPorCada100;
    }

    public int getPuntosPorCada100() {
        return puntosPorCada100;
    }

    /** Depositos solo tienen destino, retiros solo origen, el resto ambos. */
    public boolean requiereOrigen() {
        return this != DEPOSITO;
    }

    public boolean requiereDestino() {
        return this != RETIRO;
    }
}
