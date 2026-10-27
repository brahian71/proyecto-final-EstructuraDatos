package monedero.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Pila con nodos enlazados: la cima es el primer nodo, asi apilar y
 * desapilar solo tocan la cabeza.
 *
 * Costos:
 *   apilar / desapilar / cima   O(1)
 *   tamanio / estaVacia         O(1)
 *   recorrido                   O(n)  de la cima al fondo
 */
public class PilaEnlazada<T> implements Pila<T> {

    private static class Nodo<T> {
        private final T valor;
        private final Nodo<T> siguiente;

        private Nodo(T valor, Nodo<T> siguiente) {
            this.valor = valor;
            this.siguiente = siguiente;
        }
    }

    private Nodo<T> cima;
    private int tamanio;

    @Override
    public void apilar(T elemento) {
        cima = new Nodo<>(elemento, cima);
        tamanio++;
    }

    @Override
    public T desapilar() {
        if (estaVacia()) {
            throw new NoSuchElementException("La pila esta vacia");
        }
        T valor = cima.valor;
        cima = cima.siguiente;
        tamanio--;
        return valor;
    }

    @Override
    public T cima() {
        if (estaVacia()) {
            throw new NoSuchElementException("La pila esta vacia");
        }
        return cima.valor;
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
            private Nodo<T> actual = cima;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("cima -> [");
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.append("]").toString();
    }
}
