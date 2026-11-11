package monedero.estructuras;

public interface Pila<T> extends Iterable<T> {

    void apilar(T elemento);

    T desapilar();

    T cima();

    int tamanio();

    boolean estaVacia();
}
