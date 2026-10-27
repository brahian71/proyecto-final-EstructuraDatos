package monedero.modelo;

/**
 * Solo pruebas: esta en el paquete modelo para poder usar el metodo
 * package-private de Monedero y simular datos corruptos.
 */
public final class Corruptor {

    private Corruptor() {
    }

    public static void meterSinAfectarSaldo(Monedero monedero, Transaccion transaccion) {
        monedero.registrarSinAfectarSaldo(transaccion);
    }
}
