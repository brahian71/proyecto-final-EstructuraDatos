package monedero.estructuras;

/**
 * Diccionario clave -> valor. Los servicios solo conocen esta interfaz; la
 * implementacion es TablaHash (propia).
 */
public interface Mapa<K, V> {

    /** Guarda o reemplaza el valor asociado a la clave. */
    void poner(K clave, V valor);

    /** @return el valor de la clave, o null si no existe. */
    V obtener(K clave);

    boolean contiene(K clave);

    /** @return el valor eliminado, o null si la clave no existia. */
    V eliminar(K clave);

    int tamanio();

    boolean estaVacio();

    /** Recorre los valores (sin orden garantizado). */
    Iterable<V> valores();
}
