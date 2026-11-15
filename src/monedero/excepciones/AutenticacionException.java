package monedero.excepciones;

// mismo mensaje si falla el usuario o la contrasena: no revela cual existe
public class AutenticacionException extends RuntimeException {

    public AutenticacionException() {
        super("Usuario o contrasena incorrectos");
    }
}
