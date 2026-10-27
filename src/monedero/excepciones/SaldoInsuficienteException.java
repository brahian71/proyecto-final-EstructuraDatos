package monedero.excepciones;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String monederoId, long saldo, long requerido) {
        super("Saldo insuficiente en " + monederoId + ": tiene $" + saldo + ", se requieren $" + requerido);
    }
}
