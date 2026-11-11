package monedero;

import monedero.datos.DatosIniciales;
import monedero.excepciones.AutenticacionException;
import monedero.excepciones.OperacionInvalidaException;
import monedero.modelo.Cliente;
import monedero.modelo.Rol;
import monedero.modelo.Usuario;
import monedero.servicio.ServicioUsuarios;
import monedero.servicio.SistemaMonedero;

/**
 * Pruebas del inicio de sesion: registro de usuarios, contrasenas con hash y
 * sal, credenciales invalidas y roles. Sin JUnit, cada prueba imprime OK o FALLO.
 */
public class PruebasFase6 {

    private static int exitos = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        registrarEIniciar();
        credencialesInvalidas();
        contrasenaNoSeGuarda();
        usuarioNoDistingueMayusculas();
        validaciones();
        clienteConUsuario();
        clienteConUsuarioInvalidoNoCreaCliente();
        datosDeEjemplo();

        System.out.println("\nResultado: " + exitos + " OK, " + fallos + " FALLO");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    private static void registrarEIniciar() {
        ServicioUsuarios u = new ServicioUsuarios();
        u.registrar("admin", "secreto1", Rol.ADMINISTRADOR, null);
        Usuario sesion = u.iniciarSesion("admin", "secreto1");
        verificar("login: credenciales correctas", sesion.getNombreUsuario().equals("admin") && sesion.esAdministrador());
    }

    private static void credencialesInvalidas() {
        ServicioUsuarios u = new ServicioUsuarios();
        u.registrar("ana", "ana123", Rol.CLIENTE, "C001");
        lanza("login: contrasena incorrecta", AutenticacionException.class, () -> u.iniciarSesion("ana", "otra12"));
        lanza("login: usuario inexistente", AutenticacionException.class, () -> u.iniciarSesion("nadie", "ana123"));
        lanza("login: contrasena vacia", AutenticacionException.class, () -> u.iniciarSesion("ana", ""));
        lanza("login: contrasena nula", AutenticacionException.class, () -> u.iniciarSesion("ana", null));
        String m1 = mensaje(() -> u.iniciarSesion("ana", "otra12"));
        String m2 = mensaje(() -> u.iniciarSesion("nadie", "ana123"));
        verificar("login: mismo mensaje si falla usuario o contrasena", m1.equals(m2));
    }

    private static void contrasenaNoSeGuarda() {
        ServicioUsuarios u = new ServicioUsuarios();
        Usuario a = u.registrar("ana", "igual123", Rol.CLIENTE, "C001");
        Usuario b = u.registrar("bruno", "igual123", Rol.CLIENTE, "C002");
        verificar("hash: no es la contrasena", !a.getHashContrasena().contains("igual123"));
        verificar("hash: SHA-256 en hexadecimal (64 caracteres)", a.getHashContrasena().matches("[0-9a-f]{64}"));
        verificar("hash: misma contrasena, sal y hash distintos",
                !a.getSal().equals(b.getSal()) && !a.getHashContrasena().equals(b.getHashContrasena()));
    }

    private static void usuarioNoDistingueMayusculas() {
        ServicioUsuarios u = new ServicioUsuarios();
        u.registrar("  Ana ", "ana123", Rol.CLIENTE, "C001");
        verificar("usuario: se guarda en minusculas y sin espacios", u.existe("ana") && u.iniciarSesion("ANA", "ana123") != null);
        lanza("usuario: no se repite con otras mayusculas", OperacionInvalidaException.class,
                () -> u.registrar("ANA", "otra123", Rol.CLIENTE, "C002"));
        lanza("contrasena: si distingue mayusculas", AutenticacionException.class, () -> u.iniciarSesion("ana", "ANA123"));
    }

    private static void validaciones() {
        ServicioUsuarios u = new ServicioUsuarios();
        lanza("validar: usuario corto", OperacionInvalidaException.class, () -> u.registrar("ab", "123456", Rol.ADMINISTRADOR, null));
        lanza("validar: usuario con espacios", OperacionInvalidaException.class, () -> u.registrar("ana gomez", "123456", Rol.ADMINISTRADOR, null));
        lanza("validar: contrasena corta", OperacionInvalidaException.class, () -> u.registrar("ana", "12345", Rol.ADMINISTRADOR, null));
        lanza("validar: cliente sin clienteId", IllegalArgumentException.class, () -> u.registrar("ana", "123456", Rol.CLIENTE, null));
        verificar("validar: no quedo ningun usuario", u.cantidad() == 0);
    }

    private static void clienteConUsuario() {
        SistemaMonedero s = new SistemaMonedero();
        Cliente ana = s.registrarClienteConUsuario("Ana Gomez", "ana", "ana123");
        Usuario sesion = s.getUsuarios().iniciarSesion("ana", "ana123");
        verificar("cliente: el usuario queda ligado al cliente", sesion.getRol() == Rol.CLIENTE
                && sesion.getClienteId().equals(ana.getId()));
        verificar("cliente: se encuentra el usuario de un cliente", s.getUsuarios().usuarioDeCliente(ana.getId()) == sesion);
    }

    private static void clienteConUsuarioInvalidoNoCreaCliente() {
        SistemaMonedero s = new SistemaMonedero();
        s.registrarClienteConUsuario("Ana", "ana", "ana123");
        lanza("cliente: usuario repetido lanza", OperacionInvalidaException.class,
                () -> s.registrarClienteConUsuario("Otra Ana", "ana", "otra123"));
        lanza("cliente: contrasena corta lanza", OperacionInvalidaException.class,
                () -> s.registrarClienteConUsuario("Bruno", "bruno", "123"));
        int clientes = 0;
        for (Cliente ignorado : s.getClientes()) {
            clientes++;
        }
        verificar("cliente: con usuario invalido no se crea el cliente", clientes == 1);
    }

    private static void datosDeEjemplo() {
        SistemaMonedero s = new SistemaMonedero();
        DatosIniciales.cargar(s);
        verificar("ejemplo: admin entra como administrador",
                s.getUsuarios().iniciarSesion(DatosIniciales.USUARIO_ADMIN, DatosIniciales.CONTRASENA_ADMIN).esAdministrador());
        verificar("ejemplo: ana entra como su cliente", s.getUsuarios().iniciarSesion("ana", "ana123").getClienteId().equals("C001"));
        verificar("ejemplo: 5 usuarios", s.getUsuarios().cantidad() == 5);
    }

    // ------------------------------------------------------------------ apoyo

    private static String mensaje(Runnable accion) {
        try {
            accion.run();
            return "";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    private static void lanza(String nombre, Class<? extends Throwable> esperada, Runnable accion) {
        try {
            accion.run();
            verificar(nombre + " (no lanzo nada)", false);
        } catch (Throwable e) {
            verificar(nombre, esperada.isInstance(e));
        }
    }

    private static void verificar(String nombre, boolean condicion) {
        if (condicion) {
            exitos++;
            System.out.println("OK    " + nombre);
        } else {
            fallos++;
            System.out.println("FALLO " + nombre);
        }
    }
}
