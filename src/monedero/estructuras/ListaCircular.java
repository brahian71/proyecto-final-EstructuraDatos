package monedero.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Lista circular simplemente enlazada de capacidad fija: el siguiente del
 * ultimo nodo es el primero. Se usa para las notificaciones de un cliente:
 * se guardan las N mas recientes y, cuando esta llena, la nueva sobrescribe
 * a la mas vieja reutilizando su nodo (no crece sin limite ni crea basura).
 *
 * Solo se guarda la referencia al ULTIMO nodo: el primero es ultimo.siguiente,
 * asi que los dos extremos quedan a un paso.
 *
 * Costos:
 *   agregar (con o sin sobrescritura)  O(1)
 *   primero / ultimo                   O(1)
 *   tamanio / estaLlena                O(1)
 *   recorrido                          O(n)  de la mas vieja a la mas nueva
 */
public class ListaCircular<T> implements Iterable<T> {

    private static class Nodo<T> {
        private T valor;
        private Nodo<T> siguiente;

        private Nodo(T valor) {
            this.valor = valor;
        }
    }

    private final int capacidad;
    private Nodo<T> ultimo;
    private int tamanio;
    private int sobrescritos;

    public ListaCircular(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero: " + capacidad);
        }
        this.capacidad = capacidad;
    }

    /**
     * Agrega al final. Si esta llena, el nodo mas viejo (ultimo.siguiente)
     * pasa a ser el ultimo con el valor nuevo: basta con avanzar el puntero.
     */
    public void agregar(T elemento) {
        if (estaVacia()) {
            ultimo = new Nodo<>(elemento);
            ultimo.siguiente = ultimo;
            tamanio = 1;
            return;
        }
        if (estaLlena()) {
            ultimo = ultimo.siguiente;
            ultimo.valor = elemento;
            sobrescritos++;
            return;
        }
        Nodo<T> nuevo = new Nodo<>(elemento);
        nuevo.siguiente = ultimo.siguiente;
        ultimo.siguiente = nuevo;
        ultimo = nuevo;
        tamanio++;
    }

    /** El mas viejo. */
    public T primero() {
        if (estaVacia()) {
            throw new NoSuchElementException("La lista esta vacia");
        }
        return ultimo.siguiente.valor;
    }

    /** El mas reciente. */
    public T ultimo() {
        if (estaVacia()) {
            throw new NoSuchElementException("La lista esta vacia");
        }
        return ultimo.valor;
    }

    public int tamanio() {
        return tamanio;
    }

    public int capacidad() {
        return capacidad;
    }

    public boolean estaVacia() {
        return tamanio == 0;
    }

    public boolean estaLlena() {
        return tamanio == capacidad;
    }

    /** Cuantos elementos se han perdido por sobrescritura desde que se creo. */
    public int sobrescritos() {
        return sobrescritos;
    }

    /**
     * Recorre exactamente tamanio nodos empezando por el primero. En una
     * lista circular no hay null al final: se para contando, no buscando null.
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = estaVacia() ? null : ultimo.siguiente;
            private int restantes = tamanio;

            @Override
            public boolean hasNext() {
                return restantes > 0;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No hay mas elementos");
                }
                T valor = actual.valor;
                actual = actual.siguiente;
                restantes--;
                return valor;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(" -> ");
            }
        }
        return sb.append(" -> ...)").toString();
    }
}
