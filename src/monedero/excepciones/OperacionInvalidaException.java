package monedero.excepciones;

/** Operacion que viola una regla de negocio (ej. transferir a un monedero propio como TRANSFERENCIA). */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
