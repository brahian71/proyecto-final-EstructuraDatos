package monedero.estructuras;

import java.util.Objects;

public class GrafoDirigido<V> {

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
    private final Coleccion<V> ordenVertices = new ListaEnlazada<>();
    private int cantidadAristas;

    public void agregarVertice(V valor) {
        Objects.requireNonNull(valor, "El vertice no puede ser nulo");
        if (!vertices.contiene(valor)) {
            vertices.poner(valor, new Vertice<>(valor));
            ordenVertices.agregar(valor);
        }
    }

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
        Pila<V> alReves = new PilaEnlazada<>();
        for (V v = destino; v != null; v = llegoDesde.obtener(v)) {
            alReves.apilar(v);
        }
        for (V v : alReves) {
            camino.agregar(v);
        }
        return camino;
    }

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

    // DFS con tres estados (sin visitar, EN_PILA, TERMINADO): una arista hacia un vertice EN_PILA cierra un ciclo
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
        pila.eliminar(actual);
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
