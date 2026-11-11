package monedero.estructuras;

public interface Cola<T> extends Iterable<T> {

    void encolar(T elemento);

    T desencolar();

    T frente();

    int tamanio();

    boolean estaVacia();
}
