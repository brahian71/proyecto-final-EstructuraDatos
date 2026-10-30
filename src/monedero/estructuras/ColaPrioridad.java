package monedero.estructuras;

/**
 * Cola de prioridad: siempre sale primero el elemento de MENOR prioridad
 * segun el comparador (para las programadas: la fecha mas cercana).
 */
public interface ColaPrioridad<T> {

    void insertar(T elemento);

    /** Saca y devuelve el de mayor prioridad. @throws java.util.NoSuchElementException si esta vacia */
    T extraer();

    /** Lo consulta sin sacarlo. @throws java.util.NoSuchElementException si esta vacia */
    T verPrimero();

    /** Quita un elemento cualquiera (por equals). @return true si estaba. */
    boolean eliminar(T elemento);

    int tamanio();

    boolean estaVacia();
}
