package monedero.servicio;

public record FilaReporte(String etiqueta, long valor, int veces) {

    @Override
    public String toString() {
        return etiqueta + ": " + monedero.modelo.Moneda.pesos(valor) + " (" + veces + (veces == 1 ? " vez)" : " veces)");
    }
}
