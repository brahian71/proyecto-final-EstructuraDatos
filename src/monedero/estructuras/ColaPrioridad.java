package monedero.estructuras;

public interface ColaPrioridad<T> {

    void insertar(T elemento);

    T extraer();

    T verPrimero();

    boolean eliminar(T elemento);

    int tamanio();

    boolean estaVacia();
}
