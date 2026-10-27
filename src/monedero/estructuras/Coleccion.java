package monedero.estructuras;

/**
 * Extiende Iterable<T> para que cualquier coleccion se pueda recorrer con
 * for-each sin que el codigo externo conozca los nodos.
 */
public interface Coleccion<T> extends Iterable<T> {

    /** Agrega el elemento al final. */
    void agregar(T elemento);

    /**
     * Elimina la primera aparicion del elemento (segun equals).
     * @return true si lo encontro y lo elimino; false si no existia.
     */
    boolean eliminar(T elemento);

    /**
     * @throws IndexOutOfBoundsException si la posicion no es valida
     */
    T obtener(int posicion);

    int tamanio();

    boolean estaVacia();
}
