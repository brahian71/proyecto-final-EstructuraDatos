package monedero.estructuras;

import java.util.HashMap;
import java.util.Map;

/**
 * TEMPORAL: adaptador sobre java.util.HashMap mientras se ve tabla hash en
 * clase. Cuando exista TablaHash propia se cambia el new en SistemaMonedero.
 *
 * Costos (promedio): poner/obtener/contiene/eliminar O(1).
 */
public class MapaJava<K, V> implements Mapa<K, V> {

    private final Map<K, V> mapa = new HashMap<>();

    @Override
    public void poner(K clave, V valor) {
        mapa.put(clave, valor);
    }

    @Override
    public V obtener(K clave) {
        return mapa.get(clave);
    }

    @Override
    public boolean contiene(K clave) {
        return mapa.containsKey(clave);
    }

    @Override
    public V eliminar(K clave) {
        return mapa.remove(clave);
    }

    @Override
    public int tamanio() {
        return mapa.size();
    }

    @Override
    public boolean estaVacio() {
        return mapa.isEmpty();
    }

    @Override
    public Iterable<V> valores() {
        return mapa.values();
    }
}
