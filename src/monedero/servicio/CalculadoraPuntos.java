package monedero.servicio;

import monedero.modelo.TipoTransaccion;

/**
 * Regla del enunciado: deposito 1, retiro 2, transferencia 3 puntos por
 * cada 100 unidades COMPLETAS (division entera: 250 depositados = 2 puntos).
 *
 * Costo O(1). Pendiente fase 3: bono por rango.
 */
public final class CalculadoraPuntos {

    public static final long UNIDAD = 100;

    private CalculadoraPuntos() {
    }

    public static int calcular(TipoTransaccion tipo, long monto) {
        if (monto <= 0) {
            return 0;
        }
        // toIntExact lanza excepcion en vez de desbordar en silencio con montos absurdos
        return Math.toIntExact((monto / UNIDAD) * tipo.getPuntosPorCada100());
    }
}
