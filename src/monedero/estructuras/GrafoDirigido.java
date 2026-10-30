package monedero.estructuras;

import java.util.Objects;

/**
 * Grafo dirigido y ponderado con LISTA DE ADYACENCIA: cada vertice guarda
 * sus aristas salientes en una tabla hash (destino -> arista), asi agregar
 * peso a una arista que ya existe es O(1) y no se duplican aristas.
 *
 * Cada arista acumula dos cosas: peso (ej. dinero total enviado) y veces
 * (cuantas transacciones la formaron).
 *
 * Algoritmos:
 *   caminoMasCorto(a, b)  BFS con ColaEnlazada; camino con menos aristas  O(V + E)
 *   ciclos()              DFS recursivo; cada arista de retroceso
 *                         (a un vertice que sigue en la pila de la
 *                         recursion) cierra un ciclo                     O(V + E)
 *   alcanzables(a)        DFS recursivo                                  O(V + E)
 * Tamanio: O(V + E) en memoria (una matriz de adyacencia seria O(V^2)).
 */
public class GrafoDirigido<V> {

    /** Arista hacia destino con su peso acumulado. */
    public static class Arista<V> {
        private final V origen;
        private final V destino;
        private long peso;
        private int veces;

        private Arista(V origen, V destino) {
            this.origen = origen;
            this.destino = destino;
        }

        public V getOrigen() {
            return origen;
        }

        public V getDestino() {
            return destino;
        }

        public long getPeso() {
            return peso;
        }

        public int getVeces() {
            return veces;
        }

        @Override
        public String toString() {
            return origen + " -> " + destino + " (" + peso + ", " + veces + " veces)";
        }
    }

    private static class Vertice<V> {
        private final V valor;
        private final Mapa<V, Arista<V>> salientes = new TablaHash<>();
        private final Coleccion<Arista<V>> listaSalientes = new ListaEnlazada<>();
        private long pesoEntrante;
        private int gradoEntrada;

        private Vertice(V valor) {
            this.valor = valor;
        }
    }

    private final Mapa<V, Vertice<V>> vertices = new TablaHash<>();
    /** Para recorrer los vertices en el orden en que se agregaron (resultados reproducibles). */
    private final Coleccion<V> ordenVertices = new ListaEnlazada<>();
    private int cantidadAristas;

    public void agregarVertice(V valor) {
        Objects.requireNonNull(valor, "El vertice no puede ser nulo");
        if (!vertices.contiene(valor)) {
            vertices.poner(valor, new Vertice<>(valor));
            ordenVertices.agregar(valor);
        }
    }

    /** Suma peso a la arista origen -> destino (la crea si no existe). Crea los vertices si faltan. */
    public void agregarArista(V origen, V destino, long peso) {
        agregarVertice(origen);
        agregarVertice(destino);
        Vertice<V> o = vertices.obtener(origen);
        Arista<V> arista = o.salientes.obtener(destino);
        if (arista == null) {
            arista = new Arista<>(origen, destino);
            o.salientes.poner(destino, arista);
            o.listaSalientes.agregar(arista);
            vertices.obtener(destino).gradoEntrada++;
            cantidadAristas++;
        }
        arista.peso += peso;
        arista.veces++;
        vertices.obtener(destino).pesoEntrante += peso;
    }

    public boolean contieneVertice(V valor) {
        return vertices.contiene(valor);
    }

    /** @return la arista, o null si no existe. */
    public Arista<V> arista(V origen, V destino) {
        Vertice<V> o = vertices.obtener(origen);
        return o == null ? null : o.salientes.obtener(destino);
    }

    public Iterable<Arista<V>> salientes(V valor) {
        return vertice(valor).listaSalientes;
    }

    public Iterable<V> getVertices() {
        return ordenVertices;
    }

    /** Todas las aristas del grafo. */
    public Coleccion<Arista<V>> aristas() {
        Coleccion<Arista<V>> todas = new ListaEnlazada<>();
        for (V v : ordenVertices) {
            for (Arista<V> a : vertices.obtener(v).listaSalientes) {
                todas.agregar(a);
            }
        }
        return todas;
    }

    public int gradoSalida(V valor) {
        return vertice(valor).listaSalientes.tamanio();
    }

    public int gradoEntrada(V valor) {
        return vertice(valor).gradoEntrada;
    }

    public long pesoSaliente(V valor) {
        long total = 0;
        for (Arista<V> a : vertice(valor).listaSalientes) {
            total += a.peso;
        }
        return total;
    }

    public long pesoEntrante(V valor) {
        return vertice(valor).pesoEntrante;
    }

    public int cantidadVertices() {
        return vertices.tamanio();
    }

    public int cantidadAristas() {
        return cantidadAristas;
    }

    // ------------------------------------------------------------------ BFS

    /**
     * Camino con menos aristas de origen a destino (BFS). Se guarda de que
     * vertice se llego a cada uno y al final se reconstruye hacia atras.
     * @return los vertices del camino (incluye ambos extremos), o vacio si no hay.
     */
    public Coleccion<V> caminoMasCorto(V origen, V destino) {
        Coleccion<V> camino = new ListaEnlazada<>();
        if (!vertices.contiene(origen) || !vertices.contiene(destino)) {
            return camino;
        }
        Mapa<V, V> llegoDesde = new TablaHash<>();
        Mapa<V, Boolean> visitado = new TablaHash<>();
        Cola<V> cola = new ColaEnlazada<>();
        cola.encolar(origen);
        visitado.poner(origen, true);
        while (!cola.estaVacia()) {
            V actual = cola.desencolar();
            if (actual.equals(destino)) {
                break;
            }
            for (Arista<V> a : vertices.obtener(actual).listaSalientes) {
                if (!visitado.contiene(a.destino)) {
                    visitado.poner(a.destino, true);
                    llegoDesde.poner(a.destino, actual);
                    cola.encolar(a.destino);
                }
            }
        }
        if (!visitado.contiene(destino)) {
            return camino;
        }
        // reconstruir de destino a origen con una pila para dejarlo en orden
        Pila<V> alReves = new PilaEnlazada<>();
        for (V v = destino; v != null; v = llegoDesde.obtener(v)) {
            alReves.apilar(v);
        }
        for (V v : alReves) {
            camino.agregar(v);
        }
        return camino;
    }

    // ------------------------------------------------------------------ DFS

    /** Vertices a los que se puede llegar desde origen (sin incluirlo). */
    public Coleccion<V> alcanzables(V origen) {
        Coleccion<V> resultado = new ListaEnlazada<>();
        if (vertices.contiene(origen)) {
            Mapa<V, Boolean> visitado = new TablaHash<>();
            visitado.poner(origen, true);
            alcanzables(origen, visitado, resultado);
        }
        return resultado;
    }

    private void alcanzables(V actual, Mapa<V, Boolean> visitado, Coleccion<V> resultado) {
        for (Arista<V> a : vertices.obtener(actual).listaSalientes) {
            if (!visitado.contiene(a.destino)) {
                visitado.poner(a.destino, true);
                resultado.agregar(a.destino);
                alcanzables(a.destino, visitado, resultado);
            }
        }
    }

    /**
     * Ciclos encontrados con DFS. Cada vertice tiene tres estados: sin
     * visitar, EN_PILA (la recursion esta dentro de el) y TERMINADO. Si desde
     * un vertice se llega a otro que esta EN_PILA, esa arista cierra un ciclo
     * y el ciclo es el tramo de la pila desde ese vertice hasta el actual.
     * Encuentra al menos un ciclo si existe alguno (no enumera todos los
     * posibles, que pueden ser exponenciales).
     * @return cada ciclo como lista de vertices (el primero se repite al final)
     */
    public Coleccion<Coleccion<V>> ciclos() {
        Coleccion<Coleccion<V>> ciclos = new ListaEnlazada<>();
        Mapa<V, Integer> estado = new TablaHash<>();
        Coleccion<V> pila = new ListaEnlazada<>();
        for (V v : ordenVertices) {
            if (!estado.contiene(v)) {
                dfsCiclos(v, estado, pila, ciclos);
            }
        }
        return ciclos;
    }

    private static final int EN_PILA = 1;
    private static final int TERMINADO = 2;

    private void dfsCiclos(V actual, Mapa<V, Integer> estado, Coleccion<V> pila, Coleccion<Coleccion<V>> ciclos) {
        estado.poner(actual, EN_PILA);
        pila.agregar(actual);
        for (Arista<V> a : vertices.obtener(actual).listaSalientes) {
            Integer e = estado.obtener(a.destino);
            if (e == null) {
                dfsCiclos(a.destino, estado, pila, ciclos);
            } else if (e == EN_PILA) {
                ciclos.agregar(tramoDesde(pila, a.destino));
            }
        }
        pila.eliminar(actual); // es el ultimo de la pila; eliminar lo busca desde el inicio: O(profundidad)
        estado.poner(actual, TERMINADO);
    }

    private Coleccion<V> tramoDesde(Coleccion<V> pila, V inicio) {
        Coleccion<V> ciclo = new ListaEnlazada<>();
        boolean dentro = false;
        for (V v : pila) {
            dentro |= v.equals(inicio);
            if (dentro) {
                ciclo.agregar(v);
            }
        }
        ciclo.agregar(inicio);
        return ciclo;
    }

    private Vertice<V> vertice(V valor) {
        Vertice<V> v = vertices.obtener(valor);
        if (v == null) {
            throw new IllegalArgumentException("El vertice no existe: " + valor);
        }
        return v;
    }
}
