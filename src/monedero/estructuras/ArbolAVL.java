package monedero.estructuras;

/**
 * Arbol AVL: un BST que despues de cada insercion o eliminacion revisa, en
 * cada nodo del camino, que la diferencia de alturas entre sus dos hijos
 * (factor de balance) sea -1, 0 o 1. Si llega a 2 o -2 se rota.
 *
 *   balance = altura(izquierdo) - altura(derecho)
 *   balance  2 y el hijo izquierdo cargado a la izquierda  -> rotar derecha      (caso LL)
 *   balance  2 y el hijo izquierdo cargado a la derecha    -> izquierda + derecha (caso LR)
 *   balance -2 y el hijo derecho cargado a la derecha      -> rotar izquierda    (caso RR)
 *   balance -2 y el hijo derecho cargado a la izquierda    -> derecha + izquierda (caso RL)
 *
 * Asi la altura queda siempre O(log n) (como maximo ~1.44 log2 n) y todas las
 * operaciones de ArbolBST pasan a ser O(log n) en el peor caso. Cada
 * rotacion es O(1): solo se cambian tres referencias.
 */
public class ArbolAVL<K extends Comparable<K>, V> extends ArbolBST<K, V> {

    private int rotaciones;

    @Override
    protected Nodo<K, V> ajustar(Nodo<K, V> nodo) {
        actualizarAltura(nodo);
        int balance = balance(nodo);
        if (balance > 1) {
            if (balance(nodo.izquierdo) < 0) {
                nodo.izquierdo = rotarIzquierda(nodo.izquierdo);
            }
            return rotarDerecha(nodo);
        }
        if (balance < -1) {
            if (balance(nodo.derecho) > 0) {
                nodo.derecho = rotarDerecha(nodo.derecho);
            }
            return rotarIzquierda(nodo);
        }
        return nodo;
    }

    @Override
    protected boolean cumpleBalance(int alturaIzquierda, int alturaDerecha) {
        return Math.abs(alturaIzquierda - alturaDerecha) <= 1;
    }

    /** Cuantas rotaciones se han hecho; sirve para el informe. */
    public int rotaciones() {
        return rotaciones;
    }

    private static int balance(Nodo<?, ?> nodo) {
        return nodo == null ? 0 : altura(nodo.izquierdo) - altura(nodo.derecho);
    }

    /**
     *       y              x
     *      / \            / \
     *     x   C   ->     A   y
     *    / \                / \
     *   A   B              B   C
     */
    private Nodo<K, V> rotarDerecha(Nodo<K, V> y) {
        Nodo<K, V> x = y.izquierdo;
        y.izquierdo = x.derecho;
        x.derecho = y;
        actualizarAltura(y);
        actualizarAltura(x);
        rotaciones++;
        return x;
    }

    /** Espejo de rotarDerecha. */
    private Nodo<K, V> rotarIzquierda(Nodo<K, V> x) {
        Nodo<K, V> y = x.derecho;
        x.derecho = y.izquierdo;
        y.izquierdo = x;
        actualizarAltura(x);
        actualizarAltura(y);
        rotaciones++;
        return y;
    }
}
