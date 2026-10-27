package monedero.modelo;

public enum TipoNotificacion {
    SALDO_BAJO,
    TRANSFERENCIA_RECIBIDA,
    CAMBIO_RANGO,
    REVERSION,
    /** Para las transacciones programadas (fase 4). */
    RECORDATORIO
}
