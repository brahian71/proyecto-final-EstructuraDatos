package monedero.servicio;

import monedero.modelo.TipoTransaccion;

// unidad = $100: con $1, un deposito normal daria miles de puntos
public final class CalculadoraPuntos {

    public static final long UNIDADES_POR_BLOQUE = 100;

    public static final CalculadoraPuntos PESOS_COLOMBIANOS = new CalculadoraPuntos(100);
    public static final CalculadoraPuntos UNIDAD_UN_PESO = new CalculadoraPuntos(1);

    private final long pesosPorUnidad;

    public CalculadoraPuntos(long pesosPorUnidad) {
        if (pesosPorUnidad <= 0) {
            throw new IllegalArgumentException("Una unidad debe valer al menos $1: " + pesosPorUnidad);
        }
        this.pesosPorUnidad = pesosPorUnidad;
    }

    public int calcular(TipoTransaccion tipo, long monto) {
        if (monto <= 0) {
            return 0;
        }
        long bloques = monto / pesosPorBloque();
        // toIntExact lanza excepcion en vez de desbordar en silencio con montos absurdos
        return Math.toIntExact(bloques * tipo.getPuntosPorCada100());
    }

    public long pesos(long unidades) {
        return unidades * pesosPorUnidad;
    }

    public long pesosPorBloque() {
        return UNIDADES_POR_BLOQUE * pesosPorUnidad;
    }

    public long getPesosPorUnidad() {
        return pesosPorUnidad;
    }
}
