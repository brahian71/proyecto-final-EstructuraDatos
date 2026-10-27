package monedero.estructuras;

/**
 * Pila LIFO: el ultimo en entrar es el primero en salir. El iterador
 * recorre de la cima hacia el fondo.
 */
public interface Pila<T> extends Iterable<T> {

    void apilar(T elemento);

    /** @throws java.util.NoSuchElementException si esta vacia */
    T desapilar();

    /** Consulta la cima sin sacarla. @throws java.util.NoSuchElementException si esta vacia */
    T cima();

    int tamanio();

    boolean estaVacia();
}
