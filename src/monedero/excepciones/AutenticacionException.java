package monedero.excepciones;

/**
 * Usuario o contrasena incorrectos. El mensaje es siempre el mismo para no
 * revelar si lo que fallo fue el usuario o la contrasena.
 */
public class AutenticacionException extends RuntimeException {

    public AutenticacionException() {
        super("Usuario o contrasena incorrectos");
    }
}
