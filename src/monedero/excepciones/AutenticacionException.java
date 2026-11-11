package monedero.excepciones;

public class AutenticacionException extends RuntimeException {

    public AutenticacionException() {
        super("Usuario o contrasena incorrectos");
    }
}
