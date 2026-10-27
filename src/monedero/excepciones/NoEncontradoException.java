package monedero.excepciones;

/** Cliente o monedero que no existe en el sistema. */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String que, String id) {
        super(que + " no encontrado: " + id);
    }
}
