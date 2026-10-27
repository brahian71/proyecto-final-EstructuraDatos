package monedero.modelo;

import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;

import java.util.Objects;

/**
 * Un monedero de un cliente (un cliente puede tener varios).
 *
 * El saldo solo cambia por aplicar(), que al mismo tiempo guarda la
 * transaccion en el historial. Asi saldo == suma de efectos del historial,
 * que es justo lo que comprueba el VerificadorIntegridad.
 *
 * Historial en ListaEnlazada: agregar al final O(1) y se recorre en orden
 * cronologico con el iterador O(n).
 */
public class Monedero {

    private final String id;
    private final String clienteId;
    private final String nombre;
    private final TipoMonedero tipo;
    private long saldo;
    private final Coleccion<Transaccion> historial = new ListaEnlazada<>();

    public Monedero(String id, String clienteId, String nombre, TipoMonedero tipo) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id no puede ser nulo ni vacio");
        }
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El cliente no puede ser nulo ni vacio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio");
        }
        this.id = id;
        this.clienteId = clienteId;
        this.nombre = nombre.trim();
        this.tipo = Objects.requireNonNull(tipo, "El tipo no puede ser nulo");
    }

    /**
     * Aplica la transaccion: ajusta el saldo y la agrega al historial.
     * El servicio ya valido el saldo; esto es la ultima barrera.
     */
    public void aplicar(Transaccion transaccion) {
        if (!transaccion.involucra(id)) {
            throw new IllegalArgumentException("La transaccion " + transaccion.getId() + " no involucra al monedero " + id);
        }
        long nuevoSaldo = saldo + transaccion.efectoSobre(id);
        if (nuevoSaldo < 0) {
            throw new IllegalStateException("El saldo de " + id + " quedaria negativo: " + nuevoSaldo);
        }
        saldo = nuevoSaldo;
        historial.agregar(transaccion);
    }

    /** Solo lectura: quien recorre el historial no puede alterarlo. */
    public Iterable<Transaccion> getHistorial() {
        return historial;
    }

    public int cantidadTransacciones() {
        return historial.tamanio();
    }

    /**
     * Solo para pruebas (mismo paquete): mete una transaccion sin tocar el
     * saldo, para simular un dato corrupto y ver que la integridad lo detecta.
     */
    void registrarSinAfectarSaldo(Transaccion transaccion) {
        historial.agregar(transaccion);
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoMonedero getTipo() {
        return tipo;
    }

    public long getSaldo() {
        return saldo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Monedero otro)) {
            return false;
        }
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + nombre + " (" + tipo + ") $" + saldo;
    }
}
