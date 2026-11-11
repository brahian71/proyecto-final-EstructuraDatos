package monedero.servicio;

import monedero.modelo.Cliente;
import monedero.modelo.TipoTransaccion;

import java.time.LocalDateTime;

public interface PoliticaComision {

    PoliticaComision SIN_COMISION = (tipo, monto, cliente, fecha) -> 0;

    long calcular(TipoTransaccion tipo, long monto, Cliente cliente, LocalDateTime fecha);
}
