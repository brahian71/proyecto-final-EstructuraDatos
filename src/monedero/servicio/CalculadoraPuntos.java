package monedero.servicio;

import monedero.modelo.TipoTransaccion;

/**
 * Regla del enunciado: deposito 1, retiro 2, transferencia 3 puntos por
 * cada 100 UNIDADES de moneda completas (division entera).
 *
 * El enunciado no dice cuanto vale una unidad. Con pesos colombianos y
 * unidad = $1, un salario de $2.000.000 daria 20.000 puntos y el cliente
 * seria Platino desde el primer deposito. Por eso la unidad es configurable
 * (opcion C de docs/decisiones.md):
 *   PESOS_COLOMBIANOS  unidad = $100 -> puntos por cada $10.000 completos
 *   UNIDAD_UN_PESO     unidad = $1   -> puntos por cada $100 (pruebas: numeros simples)
 *
 * Lo que el enunciado expresa en unidades (el bono de 50 unidades, el umbral
 * de saldo bajo) tambien se convierte con pesos(unidades).
 *
 * Costo O(1).
 */
public final class CalculadoraPuntos {

    /** "por cada 100 unidades" del enunciado. */
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

    /** Cuantos pesos son estas unidades de moneda. */
    public long pesos(long unidades) {
        return unidades * pesosPorUnidad;
    }

    /** Pesos que hay que mover para ganar un "bloque" de puntos ($10.000 en pesos colombianos). */
    public long pesosPorBloque() {
        return UNIDADES_POR_BLOQUE * pesosPorUnidad;
    }

    public long getPesosPorUnidad() {
        return pesosPorUnidad;
    }
}
