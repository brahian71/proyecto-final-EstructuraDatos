package monedero.modelo;

public enum TipoNotificacion {
    SALDO_BAJO,
    TRANSFERENCIA_RECIBIDA,
    CAMBIO_RANGO,
    REVERSION,
    CANJE,
    /** Una transaccion programada se ejecuta pronto. */
    RECORDATORIO,
    /** Una transaccion programada no se pudo ejecutar. */
    FALLO_PROGRAMADA
}
