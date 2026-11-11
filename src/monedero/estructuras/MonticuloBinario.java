package monedero.estructuras;

import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;

// Arbol binario completo en un arreglo: padre(i) = (i - 1) / 2, hijos 2i + 1 y 2i + 2.
// Cada padre es <= que sus hijos, asi el minimo siempre esta en la posicion 0.
public class MonticuloBinario<T> implements ColaPrioridad<T> {

    private static final int CAPACIDAD_INICIAL = 16;

    private final Comparator<? super T> comparador;
    private Object[] datos;
    private int tamanio;

    public MonticuloBinario(Comparator<? super T> comparador) {
        this.comparador = Objects.requireNonNull(comparador, "El comparador no puede ser nulo");
        this.datos = new Object[CAPACIDAD_INICIAL];
    }

    @Override
    public void insertar(T elemento) {
        Objects.requireNonNull(elemento, "El elemento no puede ser nulo");
        if (tamanio == datos.length) {
            crecer();
        }
        datos[tamanio] = elemento;
        subir(tamanio);
        tamanio++;
    }

    @Override
    public T extraer() {
        T primero = verPrimero();
        quitarEn(0);
        return primero;
    }

    @Override
    public T verPrimero() {
        if (estaVacia()) {
            throw new NoSuchElementException("La cola de prioridad esta vacia");
        }
        return elemento(0);
    }

    @Override
    public boolean eliminar(T objetivo) {
        for (int i = 0; i < tamanio; i++) {
            if (Objects.equals(datos[i], objetivo)) {
                quitarEn(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public int tamanio() {
        return tamanio;
    }

    @Override
    public boolean estaVacia() {
        return tamanio == 0;
    }

    public Coleccion<T> ordenados() {
        MonticuloBinario<T> copia = new MonticuloBinario<>(comparador);
        copia.datos = datos.clone();
        copia.tamanio = tamanio;
        Coleccion<T> resultado = new ListaEnlazada<>();
        while (!copia.estaVacia()) {
            resultado.agregar(copia.extraer());
        }
        return resultado;
    }

    public boolean esValido() {
        return esValido(0);
    }

    private boolean esValido(int i) {
        if (i >= tamanio) {
            return true;
        }
        int izq = 2 * i + 1;
        int der = 2 * i + 2;
        if (izq < tamanio && menor(izq, i)) {
            return false;
        }
        if (der < tamanio && menor(der, i)) {
            return false;
        }
        return esValido(izq) && esValido(der);
    }

    private void quitarEn(int i) {
        tamanio--;
        if (i < tamanio) {
            datos[i] = datos[tamanio];
            datos[tamanio] = null;
            if (i > 0 && menor(i, (i - 1) / 2)) {
                subir(i);
            } else {
                hundir(i);
            }
        } else {
            datos[tamanio] = null;
        }
    }

    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (!menor(i, padre)) {
                return;
            }
            intercambiar(i, padre);
            i = padre;
        }
    }

    private void hundir(int i) {
        while (true) {
            int izq = 2 * i + 1;
            int der = izq + 1;
            int menorHijo = i;
            if (izq < tamanio && menor(izq, menorHijo)) {
                menorHijo = izq;
            }
            if (der < tamanio && menor(der, menorHijo)) {
                menorHijo = der;
            }
            if (menorHijo == i) {
                return;
            }
            intercambiar(i, menorHijo);
            i = menorHijo;
        }
    }

    private boolean menor(int a, int b) {
        return comparador.compare(elemento(a), elemento(b)) < 0;
    }

    private void intercambiar(int a, int b) {
        Object temporal = datos[a];
        datos[a] = datos[b];
        datos[b] = temporal;
    }

    private void crecer() {
        Object[] nuevo = new Object[datos.length * 2];
        System.arraycopy(datos, 0, nuevo, 0, tamanio);
        datos = nuevo;
    }

    @SuppressWarnings("unchecked")
    private T elemento(int i) {
        return (T) datos[i];
    }
}
