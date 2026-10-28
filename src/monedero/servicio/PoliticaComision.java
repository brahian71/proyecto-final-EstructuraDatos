package monedero.servicio;

import monedero.modelo.Cliente;
import monedero.modelo.TipoTransaccion;

import java.time.LocalDateTime;

/**
 * Regla para calcular la comision de una transaccion. SistemaMonedero recibe
 * una politica en el constructor: la real es ComisionEstandar; las pruebas
 * de las fases 1 y 2 usan SIN_COMISION para verificar saldos exactos sin
 * depender de las tarifas.
 */
public interface PoliticaComision {

    PoliticaComision SIN_COMISION = (tipo, monto, cliente, fecha) -> 0;

    /** @param cliente quien paga (el dueno del monedero origen) */
    long calcular(TipoTransaccion tipo, long monto, Cliente cliente, LocalDateTime fecha);
}
