package monedero.servicio;

/** Una fila de un reporte: que (etiqueta), cuanto dinero (valor) y cuantas veces. */
public record FilaReporte(String etiqueta, long valor, int veces) {

    @Override
    public String toString() {
        return etiqueta + ": $" + valor + " (" + veces + (veces == 1 ? " vez)" : " veces)");
    }
}
