package monedero.estructuras;

import java.util.NoSuchElementException;
import java.util.Objects;

public class ArbolBST<K extends Comparable<K>, V> {

    protected static class Nodo<K, V> {
        protected final K clave;
        protected V valor;
        protected Nodo<K, V> izquierdo;
        protected Nodo<K, V> derecho;
        protected int altura = 1;

        protected Nodo(K clave, V valor) {
            this.clave = clave;
            this.valor = valor;
        }
    }

    protected Nodo<K, V> raiz;
    private int tamanio;

    public void insertar(K clave, V valor) {
        Objects.requireNonNull(clave, "La clave no puede ser nula");
        raiz = insertar(raiz, clave, valor);
    }

    private Nodo<K, V> insertar(Nodo<K, V> nodo, K clave, V valor) {
        if (nodo == null) {
            tamanio++;
            return new Nodo<>(clave, valor);
        }
        int comparacion = clave.compareTo(nodo.clave);
        if (comparacion < 0) {
            nodo.izquierdo = insertar(nodo.izquierdo, clave, valor);
        } else if (comparacion > 0) {
            nodo.derecho = insertar(nodo.derecho, clave, valor);
        } else {
            nodo.valor = valor;
            return nodo;
        }
        return ajustar(nodo);
    }

    public V buscar(K clave) {
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int comparacion = clave.compareTo(actual.clave);
            if (comparacion == 0) {
                return actual.valor;
            }
            actual = comparacion < 0 ? actual.izquierdo : actual.derecho;
        }
        return null;
    }

    public boolean contiene(K clave) {
        Nodo<K, V> actual = raiz;
        while (actual != null) {
            int comparacion = clave.compareTo(actual.clave);
            if (comparacion == 0) {
                return true;
            }
            actual = comparacion < 0 ? actual.izquierdo : actual.derecho;
        }
        return false;
    }

    public boolean eliminar(K clave) {
        int antes = tamanio;
        raiz = eliminar(raiz, clave);
        return tamanio < antes;
    }

    private Nodo<K, V> eliminar(Nodo<K, V> nodo, K clave) {
        if (nodo == null) {
            return null;
        }
        int comparacion = clave.compareTo(nodo.clave);
        if (comparacion < 0) {
            nodo.izquierdo = eliminar(nodo.izquierdo, clave);
        } else if (comparacion > 0) {
            nodo.derecho = eliminar(nodo.derecho, clave);
        } else {
            if (nodo.izquierdo == null || nodo.derecho == null) {
                tamanio--;
                return nodo.izquierdo != null ? nodo.izquierdo : nodo.derecho;
            }
            Nodo<K, V> sucesor = nodoMinimo(nodo.derecho);
            Nodo<K, V> reemplazo = new Nodo<>(sucesor.clave, sucesor.valor);
            reemplazo.izquierdo = nodo.izquierdo;
            reemplazo.derecho = eliminar(nodo.derecho, sucesor.clave);
            nodo = reemplazo;
        }
        return ajustar(nodo);
    }

    public K minimo() {
        if (estaVacio()) {
            throw new NoSuchElementException("El arbol esta vacio");
        }
        return nodoMinimo(raiz).clave;
    }

    public K maximo() {
        if (estaVacio()) {
            throw new NoSuchElementException("El arbol esta vacio");
        }
        Nodo<K, V> actual = raiz;
        while (actual.derecho != null) {
            actual = actual.derecho;
        }
        return actual.clave;
    }

    public Coleccion<V> enOrden() {
        Coleccion<V> resultado = new ListaEnlazada<>();
        enOrden(raiz, resultado);
        return resultado;
    }

    private void enOrden(Nodo<K, V> nodo, Coleccion<V> resultado) {
        if (nodo == null) {
            return;
        }
        enOrden(nodo.izquierdo, resultado);
        resultado.agregar(nodo.valor);
        enOrden(nodo.derecho, resultado);
    }

    public Coleccion<V> mayores(int n) {
        Coleccion<V> resultado = new ListaEnlazada<>();
        mayores(raiz, n, resultado);
        return resultado;
    }

    private void mayores(Nodo<K, V> nodo, int n, Coleccion<V> resultado) {
        if (nodo == null || resultado.tamanio() >= n) {
            return;
        }
        mayores(nodo.derecho, n, resultado);
        if (resultado.tamanio() < n) {
            resultado.agregar(nodo.valor);
        }
        mayores(nodo.izquierdo, n, resultado);
    }

    public Coleccion<V> rango(K desde, K hasta) {
        Coleccion<V> resultado = new ListaEnlazada<>();
        rango(raiz, desde, hasta, resultado);
        return resultado;
    }

    private void rango(Nodo<K, V> nodo, K desde, K hasta, Coleccion<V> resultado) {
        if (nodo == null) {
            return;
        }
        boolean mayorQueDesde = nodo.clave.compareTo(desde) > 0;
        boolean menorQueHasta = nodo.clave.compareTo(hasta) < 0;
        if (mayorQueDesde) {
            rango(nodo.izquierdo, desde, hasta, resultado);
        }
        if (nodo.clave.compareTo(desde) >= 0 && nodo.clave.compareTo(hasta) <= 0) {
            resultado.agregar(nodo.valor);
        }
        if (menorQueHasta) {
            rango(nodo.derecho, desde, hasta, resultado);
        }
    }

    public int tamanio() {
        return tamanio;
    }

    public boolean estaVacio() {
        return tamanio == 0;
    }

    public int altura() {
        return altura(raiz);
    }

    public boolean esValido() {
        int[] contados = {0};
        return verificar(raiz, null, null, contados) >= 0 && contados[0] == tamanio;
    }

    private int verificar(Nodo<K, V> nodo, K min, K max, int[] contados) {
        if (nodo == null) {
            return 0;
        }
        contados[0]++;
        if ((min != null && nodo.clave.compareTo(min) <= 0) || (max != null && nodo.clave.compareTo(max) >= 0)) {
            return -1;
        }
        int izq = verificar(nodo.izquierdo, min, nodo.clave, contados);
        int der = verificar(nodo.derecho, nodo.clave, max, contados);
        if (izq < 0 || der < 0) {
            return -1;
        }
        int calculada = 1 + Math.max(izq, der);
        if (calculada != nodo.altura || !cumpleBalance(izq, der)) {
            return -1;
        }
        return calculada;
    }

    protected Nodo<K, V> ajustar(Nodo<K, V> nodo) {
        actualizarAltura(nodo);
        return nodo;
    }

    protected boolean cumpleBalance(int alturaIzquierda, int alturaDerecha) {
        return true;
    }

    protected static int altura(Nodo<?, ?> nodo) {
        return nodo == null ? 0 : nodo.altura;
    }

    protected static void actualizarAltura(Nodo<?, ?> nodo) {
        nodo.altura = 1 + Math.max(altura(nodo.izquierdo), altura(nodo.derecho));
    }

    private static <K, V> Nodo<K, V> nodoMinimo(Nodo<K, V> nodo) {
        while (nodo.izquierdo != null) {
            nodo = nodo.izquierdo;
        }
        return nodo;
    }
}
