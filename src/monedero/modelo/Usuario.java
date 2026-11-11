package monedero.modelo;

import java.util.Objects;

public class Usuario {

    private final String nombreUsuario;
    private final Rol rol;
    private final String clienteId;
    private final String sal;
    private final String hashContrasena;

    public Usuario(String nombreUsuario, Rol rol, String clienteId, String sal, String hashContrasena) {
        this.nombreUsuario = Objects.requireNonNull(nombreUsuario);
        this.rol = Objects.requireNonNull(rol);
        if ((rol == Rol.CLIENTE) != (clienteId != null)) {
            throw new IllegalArgumentException("Un usuario cliente debe tener cliente y un administrador no");
        }
        this.clienteId = clienteId;
        this.sal = Objects.requireNonNull(sal);
        this.hashContrasena = Objects.requireNonNull(hashContrasena);
    }

    public boolean esAdministrador() {
        return rol == Rol.ADMINISTRADOR;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public Rol getRol() {
        return rol;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getSal() {
        return sal;
    }

    public String getHashContrasena() {
        return hashContrasena;
    }

    @Override
    public String toString() {
        return nombreUsuario + " (" + rol + (clienteId == null ? "" : ", " + clienteId) + ")";
    }
}
