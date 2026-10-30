package monedero.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Cola con nodos enlazados: se encola por la cola (final) y se desencola por
 * el frente (inicio), asi las dos operaciones son O(1). La usa el recorrido
 * en anchura (BFS) del grafo.
 *
 * Costos:
 *   encolar / desencolar / frente   O(1)
 *   recorrido                       O(n) del frente al final
 */
public class ColaEnlazada<T> implements Cola<T> {

    private static class Nodo<T> {
        private final T valor;
        private Nodo<T> siguiente;

        private Nodo(T valor) {
            this.valor = valor;
        }
    }

    private Nodo<T> frente;
    private Nodo<T> fin;
    private int tamanio;

    @Override
    public void encolar(T elemento) {
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (estaVacia()) {
            frente = nuevo;
        } else {
            fin.siguiente = nuevo;
        }
        fin = nuevo;
        tamanio++;
    }

    @Override
    public T desencolar() {
        T valor = frente();
        frente = frente.siguiente;
        if (frente == null) {
            fin = null;
        }
        tamanio--;
        return valor;
    }

    @Override
    public T frente() {
        if (estaVacia()) {
            throw new NoSuchElementException("La cola esta vacia");
        }
        return frente.valor;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    @Override
    public boolean estaVacia() {
        return tamanio == 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Nodo<T> actual = frente;

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No hay mas elementos");
                }
                T valor = actual.valor;
                actual = actual.siguiente;
                return valor;
            }
        };
    }
}
