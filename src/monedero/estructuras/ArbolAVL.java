package monedero.estructuras;

public class ArbolAVL<K extends Comparable<K>, V> extends ArbolBST<K, V> {

    private int rotaciones;

    // balance = altura(izq) - altura(der). Si llega a 2 o -2 se rota:
    // LL -> rotarDerecha, LR -> izquierda + derecha, RR -> rotarIzquierda, RL -> derecha + izquierda
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

    public int rotaciones() {
        return rotaciones;
    }

    private static int balance(Nodo<?, ?> nodo) {
        return nodo == null ? 0 : altura(nodo.izquierdo) - altura(nodo.derecho);
    }

    private Nodo<K, V> rotarDerecha(Nodo<K, V> y) {
        Nodo<K, V> x = y.izquierdo;
        y.izquierdo = x.derecho;
        x.derecho = y;
        actualizarAltura(y);
        actualizarAltura(x);
        rotaciones++;
        return x;
    }

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
