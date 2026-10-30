package monedero.estructuras;

/** Cola FIFO: el primero en entrar es el primero en salir. */
public interface Cola<T> extends Iterable<T> {

    void encolar(T elemento);

    /** @throws java.util.NoSuchElementException si esta vacia */
    T desencolar();

    /** @throws java.util.NoSuchElementException si esta vacia */
    T frente();

    int tamanio();

    boolean estaVacia();
}
