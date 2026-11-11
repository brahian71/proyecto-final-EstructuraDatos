package monedero.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;

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

    public T primero() {
        if (estaVacia()) {
            throw new NoSuchElementException("La lista esta vacia");
        }
        return ultimo.siguiente.valor;
    }

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

    public int sobrescritos() {
        return sobrescritos;
    }

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
