package monedero.servicio;

import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;

/** Lista de inconsistencias encontradas por el VerificadorIntegridad. */
public class ResultadoIntegridad {

    private final Coleccion<String> errores = new ListaEnlazada<>();
    private int monederosRevisados;
    private int clientesRevisados;

    void agregarError(String error) {
        errores.agregar(error);
    }

    void contarMonedero() {
        monederosRevisados++;
    }

    void contarCliente() {
        clientesRevisados++;
    }

    public boolean esValido() {
        return errores.estaVacia();
    }

    public Iterable<String> getErrores() {
        return errores;
    }

    public int cantidadErrores() {
        return errores.tamanio();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Integridad: ").append(esValido() ? "OK" : cantidadErrores() + " error(es)")
                .append(" (").append(clientesRevisados).append(" clientes, ")
                .append(monederosRevisados).append(" monederos)");
        for (String error : errores) {
            sb.append("\n  - ").append(error);
        }
        return sb.toString();
    }
}
