package monedero.modelo;

import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;

/**
 * Cliente del sistema. Tiene uno o varios monederos y un total de puntos.
 *
 * Los puntos viven aqui por ahora; en la fase 3 el arbol de puntos los
 * indexa para consultas ordenadas, pero la fuente de verdad sigue siendo
 * la suma de puntos de sus transacciones (eso lo verifica la integridad).
 */
public class Cliente {

    private final String id;
    private final String nombre;
    private final Coleccion<Monedero> monederos = new ListaEnlazada<>();
    private int puntos;

    public Cliente(String id, String nombre) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id no puede ser nulo ni vacio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio");
        }
        this.id = id.trim();
        this.nombre = nombre.trim();
    }

    public void agregarMonedero(Monedero monedero) {
        if (!id.equals(monedero.getClienteId())) {
            throw new IllegalArgumentException("El monedero " + monedero.getId() + " no pertenece a " + id);
        }
        monederos.agregar(monedero);
    }

    public void sumarPuntos(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa: " + cantidad);
        }
        puntos += cantidad;
    }

    /** Lo usaran el canje y la reversion (fase 2). */
    public void restarPuntos(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa: " + cantidad);
        }
        if (cantidad > puntos) {
            throw new IllegalStateException("Puntos insuficientes: tiene " + puntos + ", se piden " + cantidad);
        }
        puntos -= cantidad;
    }

    public Rango getRango() {
        return Rango.desdePuntos(puntos);
    }

    /** Saldo total sumando todos sus monederos. O(m), m = cantidad de monederos. */
    public long saldoTotal() {
        long total = 0;
        for (Monedero monedero : monederos) {
            total += monedero.getSaldo();
        }
        return total;
    }

    public Iterable<Monedero> getMonederos() {
        return monederos;
    }

    public int cantidadMonederos() {
        return monederos.tamanio();
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPuntos() {
        return puntos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cliente otro)) {
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
        return id + " " + nombre + " - " + puntos + " pts (" + getRango() + ")";
    }
}
