package monedero.estructuras;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Tabla hash con encadenamiento: un arreglo de cubetas y cada cubeta es una
 * lista enlazada de entradas cuyas claves cayeron en esa posicion.
 *
 *   posicion = (hashCode & 0x7fffffff) % capacidad
 *
 * Cuando el factor de carga (tamanio / capacidad) pasa de 0.75 se duplica la
 * capacidad y se reubican todas las entradas (rehash), asi las cubetas
 * siguen cortas.
 *
 * Costos:
 *   poner / obtener / contiene / eliminar   O(1) promedio, O(n) peor caso
 *                                          (todas las claves en una cubeta)
 *   rehash                                 O(n), pero ocurre cada vez que el
 *                                          tamanio se duplica: O(1) amortizado
 *   recorrer valores                       O(n + capacidad)
 */
public class TablaHash<K, V> implements Mapa<K, V> {

    private static final int CAPACIDAD_INICIAL = 16;
    private static final double FACTOR_CARGA_MAXIMO = 0.75;

    private static class Entrada<K, V> {
        private final K clave;
        private V valor;
        private Entrada<K, V> siguiente;

        private Entrada(K clave, V valor, Entrada<K, V> siguiente) {
            this.clave = clave;
            this.valor = valor;
            this.siguiente = siguiente;
        }
    }

    private Entrada<K, V>[] cubetas;
    private int tamanio;

    public TablaHash() {
        this(CAPACIDAD_INICIAL);
    }

    public TablaHash(int capacidadInicial) {
        if (capacidadInicial <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero: " + capacidadInicial);
        }
        cubetas = crearCubetas(capacidadInicial);
    }

    @Override
    public void poner(K clave, V valor) {
        Objects.requireNonNull(clave, "La clave no puede ser nula");
        int i = posicion(clave, cubetas.length);
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (e.clave.equals(clave)) {
                e.valor = valor;
                return;
            }
        }
        // se inserta al inicio de la cubeta: O(1)
        cubetas[i] = new Entrada<>(clave, valor, cubetas[i]);
        tamanio++;
        if ((double) tamanio / cubetas.length > FACTOR_CARGA_MAXIMO) {
            redimensionar(cubetas.length * 2);
        }
    }

    @Override
    public V obtener(K clave) {
        Entrada<K, V> e = buscar(clave);
        return e == null ? null : e.valor;
    }

    @Override
    public boolean contiene(K clave) {
        return buscar(clave) != null;
    }

    @Override
    public V eliminar(K clave) {
        if (clave == null) {
            return null;
        }
        int i = posicion(clave, cubetas.length);
        Entrada<K, V> anterior = null;
        for (Entrada<K, V> e = cubetas[i]; e != null; e = e.siguiente) {
            if (e.clave.equals(clave)) {
                if (anterior == null) {
                    cubetas[i] = e.siguiente;
                } else {
                    anterior.siguiente = e.siguiente;
                }
                tamanio--;
                return e.valor;
            }
            anterior = e;
        }
        return null;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    @Override
    public boolean estaVacio() {
        return tamanio == 0;
    }

    public int capacidad() {
        return cubetas.length;
    }

    /** Largo de la cubeta mas llena; sirve para mostrar que las colisiones se mantienen bajas. */
    public int cubetaMasLarga() {
        int maximo = 0;
        for (Entrada<K, V> cabeza : cubetas) {
            int largo = 0;
            for (Entrada<K, V> e = cabeza; e != null; e = e.siguiente) {
                largo++;
            }
            maximo = Math.max(maximo, largo);
        }
        return maximo;
    }

    @Override
    public Iterable<V> valores() {
        return () -> new Iterator<>() {
            private int cubeta = -1;
            private Entrada<K, V> actual = null;

            {
                avanzar();
            }

            /** Deja actual en la siguiente entrada no vacia, o null si no quedan. */
            private void avanzar() {
                if (actual != null) {
                    actual = actual.siguiente;
                }
                while (actual == null && ++cubeta < cubetas.length) {
                    actual = cubetas[cubeta];
                }
            }

            @Override
            public boolean hasNext() {
                return actual != null;
            }

            @Override
            public V next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No hay mas elementos");
                }
                V valor = actual.valor;
                avanzar();
                return valor;
            }
        };
    }

    // ------------------------------------------------------------------ apoyo

    private Entrada<K, V> buscar(K clave) {
        if (clave == null) {
            return null;
        }
        for (Entrada<K, V> e = cubetas[posicion(clave, cubetas.length)]; e != null; e = e.siguiente) {
            if (e.clave.equals(clave)) {
                return e;
            }
        }
        return null;
    }

    /** El & quita el signo: hashCode puede ser negativo y % daria posicion negativa. */
    private static int posicion(Object clave, int capacidad) {
        return (clave.hashCode() & 0x7fffffff) % capacidad;
    }

    private void redimensionar(int nuevaCapacidad) {
        Entrada<K, V>[] viejas = cubetas;
        cubetas = crearCubetas(nuevaCapacidad);
        for (Entrada<K, V> cabeza : viejas) {
            Entrada<K, V> e = cabeza;
            while (e != null) {
                Entrada<K, V> siguiente = e.siguiente;
                int i = posicion(e.clave, nuevaCapacidad);
                e.siguiente = cubetas[i];
                cubetas[i] = e;
                e = siguiente;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Entrada<K, V>[] crearCubetas(int capacidad) {
        // Java no permite new Entrada<K, V>[n]; se crea sin tipo y se convierte
        return (Entrada<K, V>[]) new Entrada[capacidad];
    }
}
