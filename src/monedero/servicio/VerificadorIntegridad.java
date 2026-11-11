package monedero.servicio;

import monedero.modelo.Cliente;
import monedero.modelo.Moneda;
import monedero.modelo.Monedero;
import monedero.modelo.MovimientoPuntos;
import monedero.modelo.Rango;
import monedero.modelo.Transaccion;

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
        if (!sistema.getProgramador().colaValida()) {
            resultado.agregarError("Cola de programadas: no cumple la propiedad de monticulo");
        }
        if (!sistema.getProgramador().colaConsistente()) {
            resultado.agregarError("Cola de programadas: hay programadas activas fuera de la cola (o al reves)");
        }
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
            resultado.agregarError(id + ": saldo guardado " + Moneda.pesos(monedero.getSaldo())
                    + " pero el historial suma " + Moneda.pesos(tramo.suma));
        }
        if (tramo.minimoParcial < 0) {
            resultado.agregarError(id + ": el saldo llego a " + Moneda.pesos(tramo.minimoParcial) + " en algun punto del historial");
        }
    }

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
        int base = sistema.getCalculadoraPuntos().calcular(t.getTipo(), t.getMonto());
        int bonoMaximo = t.getTipo().recibeBonoPorRango() ? Rango.PLATINO.bonoSobre(base) : 0;
        if (t.getPuntos() - t.getBonoPuntos() != base) {
            resultado.agregarError(t.getId() + ": tiene " + (t.getPuntos() - t.getBonoPuntos())
                    + " puntos base y le corresponden " + base);
        }
        if (t.getBonoPuntos() > bonoMaximo) {
            resultado.agregarError(t.getId() + ": bono de " + t.getBonoPuntos() + " puntos, el maximo es " + bonoMaximo);
        }
    }

    private int primeraFueraDeOrden(Transaccion[] arr, int ini, int fin) {
        if (ini >= fin) {
            return -1;
        }
        int medio = (ini + fin) / 2;
        int izquierda = primeraFueraDeOrden(arr, ini, medio);
        if (izquierda >= 0) {
            return izquierda;
        }
        if (arr[medio + 1].getFecha().isBefore(arr[medio].getFecha())) {
            return medio + 1;
        }
        return primeraFueraDeOrden(arr, medio + 1, fin);
    }

    // Combinar dos mitades: suma = izq.suma + der.suma; minimo = min(izq.minimo, izq.suma + der.minimo)
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

    private static Transaccion[] aArreglo(Iterable<Transaccion> historial, int tamanio) {
        Transaccion[] arr = new Transaccion[tamanio];
        int i = 0;
        for (Transaccion t : historial) {
            arr[i++] = t;
        }
        return arr;
    }
}
