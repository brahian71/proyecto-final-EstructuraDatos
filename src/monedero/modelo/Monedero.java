package monedero.modelo;

import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;

import java.util.Objects;

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

    public Iterable<Transaccion> getHistorial() {
        return historial;
    }

    public int cantidadTransacciones() {
        return historial.tamanio();
    }

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
        return id + " " + nombre + " (" + tipo + ") " + Moneda.pesos(saldo);
    }
}
