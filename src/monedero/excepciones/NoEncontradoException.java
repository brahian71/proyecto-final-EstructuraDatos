package monedero.excepciones;

public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String que, String id) {
        super(que + " no encontrado: " + id);
    }
}
