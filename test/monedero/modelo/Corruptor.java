package monedero.modelo;

public final class Corruptor {

    private Corruptor() {
    }

    public static void meterSinAfectarSaldo(Monedero monedero, Transaccion transaccion) {
        monedero.registrarSinAfectarSaldo(transaccion);
    }
}
