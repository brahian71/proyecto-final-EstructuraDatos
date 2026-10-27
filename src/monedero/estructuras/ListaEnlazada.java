package monedero.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Costos:
 *   agregar         O(1)
 *   eliminar(T)     O(n)  (hay que buscarlo)
 *   obtener(i)      O(n)  (hay que caminar i nodos)
 *   tamanio/vacia   O(1)
 *   recorrido total O(n)  con el iterador (con obtener(i) en un for seria O(n^2))
 */
public class ListaEnlazada<T> implements Coleccion<T> {

    private static class Nodo<T> {
        private final T valor;
        private Nodo<T> siguiente;

        private Nodo(T valor) {
            this.valor = valor;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamanio;

    @Override
    public void agregar(T elemento) {
        Nodo<T> nuevo = new Nodo<>(elemento);
        if (estaVacia()) {
            cabeza = nuevo;
        } else {
            cola.siguiente = nuevo;
        }
        cola = nuevo;
        tamanio++;
    }

    @Override
    public boolean eliminar(T elemento) {
        if (estaVacia()) {
            return false;
        }

        if (Objects.equals(cabeza.valor, elemento)) {
            cabeza = cabeza.siguiente;
            if (cabeza == null) {
                cola = null;
            }
            tamanio--;
            return true;
        }

        Nodo<T> anterior = cabeza;
        Nodo<T> actual = cabeza.siguiente;
        while (actual != null) {
            if (Objects.equals(actual.valor, elemento)) {
                anterior.siguiente = actual.siguiente;
                if (actual == cola) {
                    cola = anterior;
                }
                tamanio--;
                return true;
            }
            anterior = actual;
            actual = actual.siguiente;
        }
        return false;
    }

    @Override
    public T obtener(int posicion) {
        if (posicion < 0 || posicion >= tamanio) {
            throw new IndexOutOfBoundsException("Posicion " + posicion + " fuera de rango [0, " + tamanio + ")");
        }
        Nodo<T> actual = cabeza;
        for (int i = 0; i < posicion; i++) {
            actual = actual.siguiente;
        }
        return actual.valor;
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
        return new IteradorLista();
    }


    private class IteradorLista implements Iterator<T> {

        private Nodo<T> actual = cabeza;

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
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(" -> ");
            }
        }
        return sb.append("]").toString();
    }
}
