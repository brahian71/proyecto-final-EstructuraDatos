package monedero.servicio;

import monedero.estructuras.Mapa;
import monedero.estructuras.TablaHash;
import monedero.excepciones.AutenticacionException;
import monedero.excepciones.OperacionInvalidaException;
import monedero.modelo.Rol;
import monedero.modelo.Usuario;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Usuarios e inicio de sesion. Los usuarios se guardan en una TablaHash por
 * nombre de usuario (en minusculas), asi iniciar sesion es O(1) promedio.
 *
 * Contrasenas: nunca se guardan. Se guarda hash = SHA-256(sal + contrasena),
 * con una sal aleatoria de 16 bytes distinta para cada usuario. Asi dos
 * usuarios con la misma contrasena tienen hashes distintos, y quien vea los
 * datos no puede leer las contrasenas. Al iniciar sesion se recalcula el hash
 * con la sal guardada y se compara en tiempo constante (MessageDigest.isEqual).
 */
public class ServicioUsuarios {

    public static final int MINIMO_USUARIO = 3;
    public static final int MINIMO_CONTRASENA = 6;

    private final Mapa<String, Usuario> usuarios = new TablaHash<>();
    private final SecureRandom aleatorio = new SecureRandom();

    /** Revisa los datos sin crear nada (para validar antes de crear el cliente). */
    public void validarNuevo(String nombreUsuario, String contrasena) {
        String clave = normalizar(nombreUsuario);
        if (clave.length() < MINIMO_USUARIO || !clave.matches("[a-z0-9._]+")) {
            throw new OperacionInvalidaException("El usuario debe tener al menos " + MINIMO_USUARIO
                    + " caracteres: letras, numeros, punto o guion bajo");
        }
        if (usuarios.contiene(clave)) {
            throw new OperacionInvalidaException("El usuario " + clave + " ya existe");
        }
        if (contrasena == null || contrasena.isBlank() || contrasena.length() < MINIMO_CONTRASENA) {
            throw new OperacionInvalidaException("La contrasena debe tener al menos " + MINIMO_CONTRASENA + " caracteres");
        }
    }

    public Usuario registrar(String nombreUsuario, String contrasena, Rol rol, String clienteId) {
        validarNuevo(nombreUsuario, contrasena);
        byte[] bytesSal = new byte[16];
        aleatorio.nextBytes(bytesSal);
        String sal = Base64.getEncoder().encodeToString(bytesSal);
        Usuario usuario = new Usuario(normalizar(nombreUsuario), rol, clienteId, sal, hash(sal, contrasena));
        usuarios.poner(usuario.getNombreUsuario(), usuario);
        return usuario;
    }

    /** @throws AutenticacionException con el mismo mensaje si falla el usuario o la contrasena */
    public Usuario iniciarSesion(String nombreUsuario, String contrasena) {
        Usuario usuario = usuarios.obtener(normalizar(nombreUsuario));
        if (usuario == null || contrasena == null) {
            hash("", String.valueOf(contrasena)); // mismo trabajo que con un usuario real: no se nota si existe
            throw new AutenticacionException();
        }
        byte[] esperado = usuario.getHashContrasena().getBytes(StandardCharsets.UTF_8);
        byte[] recibido = hash(usuario.getSal(), contrasena).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperado, recibido)) {
            throw new AutenticacionException();
        }
        return usuario;
    }

    public boolean existe(String nombreUsuario) {
        return usuarios.contiene(normalizar(nombreUsuario));
    }

    public int cantidad() {
        return usuarios.tamanio();
    }

    /** Usuario ligado a un cliente, o null. O(n): solo lo usa la interfaz para mostrarlo. */
    public Usuario usuarioDeCliente(String clienteId) {
        for (Usuario u : usuarios.valores()) {
            if (clienteId.equals(u.getClienteId())) {
                return u;
            }
        }
        return null;
    }

    private static String normalizar(String nombreUsuario) {
        return nombreUsuario == null ? "" : nombreUsuario.trim().toLowerCase();
    }

    static String hash(String sal, String contrasena) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] resumen = sha.digest((sal + contrasena).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e); // todo Java lo trae
        }
    }
}
