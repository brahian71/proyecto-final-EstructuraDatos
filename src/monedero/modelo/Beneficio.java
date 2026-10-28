package monedero.modelo;

/**
 * Beneficios que se canjean con puntos (valores del enunciado).
 * duracionDias 0 = se aplica una sola vez en el momento del canje.
 */
public enum Beneficio {

    DESCUENTO_TRANSFERENCIAS(100, 30, "10% menos de comision en transferencias por 30 dias"),
    RETIROS_SIN_CARGO(500, 30, "Retiros sin comision por 30 dias"),
    BONO_SALDO(1000, 0, "Bono de $" + Beneficio.MONTO_BONO + " en el monedero principal");

    /** Valor del enunciado ("50 unidades"); pendiente reescalar a pesos colombianos. */
    public static final long MONTO_BONO = 50;

    private final int costoPuntos;
    private final int duracionDias;
    private final String descripcion;

    Beneficio(int costoPuntos, int duracionDias, String descripcion) {
        this.costoPuntos = costoPuntos;
        this.duracionDias = duracionDias;
        this.descripcion = descripcion;
    }

    public int getCostoPuntos() {
        return costoPuntos;
    }

    public int getDuracionDias() {
        return duracionDias;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
