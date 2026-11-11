package monedero.estructuras;

public interface Mapa<K, V> {

    void poner(K clave, V valor);

    V obtener(K clave);

    boolean contiene(K clave);

    V eliminar(K clave);

    int tamanio();

    boolean estaVacio();

    Iterable<V> valores();
}
