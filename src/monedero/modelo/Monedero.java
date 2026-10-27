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
        if (transaccion.isRevertida()) {
            throw new IllegalArgumentException("La transaccion " + transaccion.getId() + " ya esta revertida");
        }
        long nuevoSaldo = saldo + transaccion.efectoSobre(id);
        if (nuevoSaldo < 0) {
            throw new IllegalStateException("El saldo de " + id + " quedaria negativo: " + nuevoSaldo);
        }
        saldo = nuevoSaldo;
        historial.agregar(transaccion);
    }

    /**
     * Deshace el efecto de una transaccion que ya esta en el historial. No la
     * saca del historial: el servicio la marca como revertida despues de
     * ajustar los monederos involucrados.
     */
    public void revertir(Transaccion transaccion) {
        if (!transaccion.involucra(id)) {
            throw new IllegalArgumentException("La transaccion " + transaccion.getId() + " no involucra al monedero " + id);
        }
        if (transaccion.isRevertida()) {
            throw new IllegalStateException("La transaccion " + transaccion.getId() + " ya fue revertida");
        }
        long nuevoSaldo = saldo - transaccion.efectoSobre(id);
        if (nuevoSaldo < 0) {
            throw new IllegalStateException("El saldo de " + id + " quedaria negativo: " + nuevoSaldo);
        }
        saldo = nuevoSaldo;
    }

    /**
     * Menor saldo que tuvo el monedero desde la transaccion indicada
     * (incluida) hasta hoy. Si se quita un ingreso de X, todos esos saldos
     * bajan X, asi que solo se puede revertir si este minimo es >= X: si no,
     * esa plata ya se gasto en algun momento. O(n).
     */
    public long saldoMinimoDesde(Transaccion transaccion) {
        long corriente = 0;
        long minimo = Long.MAX_VALUE;
        boolean encontrada = false;
        for (Transaccion t : historial) {
            corriente += t.efectoVigenteSobre(id);
            encontrada |= t.equals(transaccion);
            if (encontrada) {
                minimo = Math.min(minimo, corriente);
            }
        }
        if (!encontrada) {
            throw new IllegalArgumentException("La transaccion " + transaccion.getId() + " no esta en el historial de " + id);
        }
        return minimo;
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
