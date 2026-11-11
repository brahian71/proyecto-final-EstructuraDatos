package monedero.servicio;

import monedero.modelo.Beneficio;
import monedero.modelo.Cliente;
import monedero.modelo.TipoTransaccion;

import java.time.LocalDateTime;

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
