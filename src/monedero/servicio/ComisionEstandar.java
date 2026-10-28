package monedero.servicio;

import monedero.modelo.Beneficio;
import monedero.modelo.Cliente;
import monedero.modelo.TipoTransaccion;

import java.time.LocalDateTime;

/**
 * Tarifas (opcion C de docs/decisiones.md):
 *   TRANSFERENCIA       1 % del monto, minimo $500, menos los descuentos:
 *                         por rango (Bronce 0, Plata 10, Oro 20, Platino 30 %)
 *                         + 10 % si tiene activo DESCUENTO_TRANSFERENCIAS
 *   RETIRO              $2.000 fijos, $0 si tiene activo RETIROS_SIN_CARGO
 *   DEPOSITO, MOVIMIENTO_INTERNO, BONO   gratis
 *
 * Todo en enteros: el descuento se aplica como comision * (100 - %) / 100.
 */
public class ComisionEstandar implements PoliticaComision {

    public static final long PORCENTAJE_TRANSFERENCIA = 1;
    public static final long MINIMO_TRANSFERENCIA = 500;
    public static final long TARIFA_RETIRO = 2_000;
    public static final int DESCUENTO_CANJE = 10;

    @Override
    public long calcular(TipoTransaccion tipo, long monto, Cliente cliente, LocalDateTime fecha) {
        return switch (tipo) {
            case TRANSFERENCIA -> comisionTransferencia(monto, cliente, fecha);
            case RETIRO -> cliente.tieneBeneficio(Beneficio.RETIROS_SIN_CARGO, fecha) ? 0 : TARIFA_RETIRO;
            case DEPOSITO, MOVIMIENTO_INTERNO, BONO -> 0;
        };
    }

    private long comisionTransferencia(long monto, Cliente cliente, LocalDateTime fecha) {
        long base = Math.max(MINIMO_TRANSFERENCIA, monto * PORCENTAJE_TRANSFERENCIA / 100);
        int descuento = cliente.getRango().getDescuentoTransferencia();
        if (cliente.tieneBeneficio(Beneficio.DESCUENTO_TRANSFERENCIAS, fecha)) {
            descuento += DESCUENTO_CANJE;
        }
        return base * (100 - descuento) / 100;
    }
}
