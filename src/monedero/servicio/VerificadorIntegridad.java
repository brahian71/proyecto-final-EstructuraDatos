package monedero.servicio;

import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.MovimientoPuntos;
import monedero.modelo.Transaccion;

/**
 * Verificacion recursiva de la integridad de las transacciones.
 *
 * Por cada monedero se pasa el historial a un arreglo y se aplica
 * DIVIDE Y VENCERAS (partir a la mitad, resolver cada mitad, combinar):
 *   1. cada transaccion es valida (lo involucra, existen sus monederos,
 *      los puntos coinciden con la regla)
 *   2. las fechas estan en orden cronologico
 *   3. saldo guardado == suma recursiva de los efectos del historial
 *   4. el saldo nunca fue negativo en ningun punto de la historia
 *      (minimo de las sumas parciales, tambien por divide y venceras)
 * Por cada cliente:
 *   5. puntos acumulados == suma recursiva de los puntos de sus transacciones
 *   6. puntos disponibles == suma recursiva de su historial de puntos, y >= 0
 *   7. esta en el arbol de puntos con su clave actual
 * Arbol AVL de puntos:
 *   8. orden, alturas y balance correctos (ArbolBST.esValido, recursivo) y
 *      tiene exactamente un nodo por cliente
 *
 * Las transacciones revertidas siguen en el historial pero cuentan 0 en
 * saldo y puntos (efectoVigenteSobre / puntosVigentes).
 *
 * Costo: O(n) por monedero (cada transaccion se visita una vez por chequeo)
 * y profundidad de recursion O(log n), asi que no hay StackOverflow aunque
 * el historial sea grande. Recorrer la lista nodo a nodo recursivamente
 * tendria profundidad O(n).
 */
public class VerificadorIntegridad {

    private final SistemaMonedero sistema;

    public VerificadorIntegridad(SistemaMonedero sistema) {
        this.sistema = sistema;
    }

    public ResultadoIntegridad verificar() {
        ResultadoIntegridad resultado = new ResultadoIntegridad();
        for (Monedero monedero : sistema.getMonederos()) {
            verificarMonedero(monedero, resultado);
        }
        int clientes = 0;
        for (Cliente cliente : sistema.getClientes()) {
            verificarPuntosCliente(cliente, resultado);
            clientes++;
        }
        verificarArbolPuntos(clientes, resultado);
        return resultado;
    }

    private void verificarArbolPuntos(int clientes, ResultadoIntegridad resultado) {
        RegistroPuntos registro = sistema.getRegistroPuntos();
        if (!registro.arbolValido()) {
            resultado.agregarError("Arbol de puntos: el orden, las alturas o el balance AVL no son correctos");
        }
        if (registro.tamanio() != clientes) {
            resultado.agregarError("Arbol de puntos: tiene " + registro.tamanio() + " nodos y hay " + clientes + " clientes");
        }
    }

    // ------------------------------------------------------------------ monedero

    private void verificarMonedero(Monedero monedero, ResultadoIntegridad resultado) {
        resultado.contarMonedero();
        Transaccion[] historial = aArreglo(monedero.getHistorial(), monedero.cantidadTransacciones());
        String id = monedero.getId();

        validarTransacciones(historial, id, 0, historial.length - 1, resultado);

        int desorden = primeraFueraDeOrden(historial, 0, historial.length - 1);
        if (desorden >= 0) {
            resultado.agregarError(id + ": la transaccion " + historial[desorden].getId()
                    + " tiene fecha anterior a la que la precede");
        }

        Tramo tramo = sumarEfectos(historial, id, 0, historial.length - 1);
        if (tramo.suma != monedero.getSaldo()) {
            resultado.agregarError(id + ": saldo guardado $" + monedero.getSaldo()
                    + " pero el historial suma $" + tramo.suma);
        }
        if (tramo.minimoParcial < 0) {
            resultado.agregarError(id + ": el saldo llego a $" + tramo.minimoParcial + " en algun punto del historial");
        }
    }

    /** Recorre el arreglo partiendolo a la mitad y valida cada transaccion. */
    private void validarTransacciones(Transaccion[] arr, String monederoId, int ini, int fin,
                                      ResultadoIntegridad resultado) {
        if (ini > fin) {
            return;
        }
        if (ini == fin) {
            validarUna(arr[ini], monederoId, resultado);
            return;
        }
        int medio = (ini + fin) / 2;
        validarTransacciones(arr, monederoId, ini, medio, resultado);
        validarTransacciones(arr, monederoId, medio + 1, fin, resultado);
    }

    private void validarUna(Transaccion t, String monederoId, ResultadoIntegridad resultado) {
        if (!t.involucra(monederoId)) {
            resultado.agregarError(monederoId + ": la transaccion " + t.getId() + " no lo involucra");
        }
        if (t.getOrigenId() != null && !sistema.existeMonedero(t.getOrigenId())) {
            resultado.agregarError(t.getId() + ": el monedero origen " + t.getOrigenId() + " no existe");
        }
        if (t.getDestinoId() != null && !sistema.existeMonedero(t.getDestinoId())) {
            resultado.agregarError(t.getId() + ": el monedero destino " + t.getDestinoId() + " no existe");
        }
        int esperados = CalculadoraPuntos.calcular(t.getTipo(), t.getMonto());
        if (t.getPuntos() != esperados) {
            resultado.agregarError(t.getId() + ": tiene " + t.getPuntos() + " puntos y le corresponden " + esperados);
        }
    }

    /**
     * @return posicion de la primera transaccion con fecha menor que la
     *         anterior, o -1 si el tramo esta en orden.
     */
    private int primeraFueraDeOrden(Transaccion[] arr, int ini, int fin) {
        if (ini >= fin) {
            return -1;
        }
        int medio = (ini + fin) / 2;
        int izquierda = primeraFueraDeOrden(arr, ini, medio);
        if (izquierda >= 0) {
            return izquierda;
        }
        // la frontera entre las dos mitades tambien hay que revisarla
        if (arr[medio + 1].getFecha().isBefore(arr[medio].getFecha())) {
            return medio + 1;
        }
        return primeraFueraDeOrden(arr, medio + 1, fin);
    }

    /**
     * Resumen de un tramo del historial:
     *   suma          efecto total sobre el saldo
     *   minimoParcial menor saldo alcanzado dentro del tramo, empezando en 0
     * Combinar: suma = izq.suma + der.suma
     *           minimo = min(izq.minimo, izq.suma + der.minimo)
     */
    private record Tramo(long suma, long minimoParcial) {
    }

    private Tramo sumarEfectos(Transaccion[] arr, String monederoId, int ini, int fin) {
        if (ini > fin) {
            return new Tramo(0, 0);
        }
        if (ini == fin) {
            long efecto = arr[ini].efectoVigenteSobre(monederoId);
            return new Tramo(efecto, Math.min(0, efecto));
        }
        int medio = (ini + fin) / 2;
        Tramo izq = sumarEfectos(arr, monederoId, ini, medio);
        Tramo der = sumarEfectos(arr, monederoId, medio + 1, fin);
        return new Tramo(izq.suma + der.suma, Math.min(izq.minimoParcial, izq.suma + der.minimoParcial));
    }

    // ------------------------------------------------------------------ cliente

    private void verificarPuntosCliente(Cliente cliente, ResultadoIntegridad resultado) {
        resultado.contarCliente();
        int total = 0;
        for (Monedero monedero : cliente.getMonederos()) {
            Transaccion[] historial = aArreglo(monedero.getHistorial(), monedero.cantidadTransacciones());
            total += sumarPuntos(historial, monedero.getId(), 0, historial.length - 1);
        }
        if (total != cliente.getPuntos()) {
            resultado.agregarError(cliente.getId() + ": tiene " + cliente.getPuntos()
                    + " puntos pero sus transacciones suman " + total);
        }

        MovimientoPuntos[] movimientos = new MovimientoPuntos[cliente.cantidadMovimientosPuntos()];
        int i = 0;
        for (MovimientoPuntos m : cliente.getHistorialPuntos()) {
            movimientos[i++] = m;
        }
        int sumaHistorial = sumarMovimientos(movimientos, 0, movimientos.length - 1);
        if (sumaHistorial != cliente.getPuntosDisponibles()) {
            resultado.agregarError(cliente.getId() + ": tiene " + cliente.getPuntosDisponibles()
                    + " puntos disponibles pero su historial de puntos suma " + sumaHistorial);
        }
        if (cliente.getPuntosDisponibles() < 0) {
            resultado.agregarError(cliente.getId() + ": puntos disponibles negativos (" + cliente.getPuntosDisponibles() + ")");
        }
        if (!sistema.getRegistroPuntos().contiene(cliente)) {
            resultado.agregarError(cliente.getId() + ": no esta en el arbol de puntos con " + cliente.getPuntos() + " puntos");
        }
    }

    /** Suma del historial de puntos por divide y venceras. */
    private int sumarMovimientos(MovimientoPuntos[] arr, int ini, int fin) {
        if (ini > fin) {
            return 0;
        }
        if (ini == fin) {
            return arr[ini].getCantidad();
        }
        int medio = (ini + fin) / 2;
        return sumarMovimientos(arr, ini, medio) + sumarMovimientos(arr, medio + 1, fin);
    }

    /**
     * Suma los puntos que este cliente gano en este monedero. Una
     * transferencia aparece en dos historiales; solo cuenta en el monedero
     * de quien gana los puntos (el origen, o el destino si es deposito),
     * asi no se cuenta dos veces. Los monederos recorridos son del
     * cliente, por eso basta con comparar el monedero.
     */
    private int sumarPuntos(Transaccion[] arr, String monederoId, int ini, int fin) {
        if (ini > fin) {
            return 0;
        }
        if (ini == fin) {
            Transaccion t = arr[ini];
            boolean esDeEsteMonedero = monederoId.equals(
                    t.getTipo().requiereOrigen() ? t.getOrigenId() : t.getDestinoId());
            return esDeEsteMonedero ? t.puntosVigentes() : 0;
        }
        int medio = (ini + fin) / 2;
        return sumarPuntos(arr, monederoId, ini, medio)
                + sumarPuntos(arr, monederoId, medio + 1, fin);
    }

    // ------------------------------------------------------------------ apoyo

    /** Pasa el historial a arreglo para tener acceso O(1) por posicion. O(n). */
    private static Transaccion[] aArreglo(Iterable<Transaccion> historial, int tamanio) {
        Transaccion[] arr = new Transaccion[tamanio];
        int i = 0;
        for (Transaccion t : historial) {
            arr[i++] = t;
        }
        return arr;
    }
}
