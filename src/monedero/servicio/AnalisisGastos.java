package monedero.servicio;

import monedero.estructuras.Coleccion;
import monedero.estructuras.GrafoDirigido;
import monedero.estructuras.ListaEnlazada;
import monedero.estructuras.Mapa;
import monedero.estructuras.MonticuloBinario;
import monedero.estructuras.TablaHash;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Transaccion;

import java.util.Comparator;

/**
 * Analisis de patrones de gasto con GRAFOS DIRIGIDOS. Los tres grafos se
 * arman desde el registro global de transacciones (las revertidas no
 * cuentan), cada vez que se pide un reporte: O(T) con T transacciones.
 *
 *   transferencias  vertices = clientes,   A -> B si A le envio dinero a B
 *   monederos       vertices = monederos,  A -> B si paso dinero de A a B
 *                   (incluye movimientos entre monederos del mismo cliente)
 *   categorias      vertices = categorias, X -> Y si despues de gastar en X
 *                   el mismo cliente gasto en Y (secuencias de gasto)
 *
 * Gasto = retiro o transferencia (enviada a otro cliente) con categoria.
 * Los rankings se ordenan con el MonticuloBinario propio.
 */
public class AnalisisGastos {

    private final SistemaMonedero sistema;

    public AnalisisGastos(SistemaMonedero sistema) {
        this.sistema = sistema;
    }

    // ------------------------------------------------------------------ grafos

    public GrafoDirigido<String> grafoTransferencias() {
        GrafoDirigido<String> grafo = new GrafoDirigido<>();
        for (Cliente c : sistema.getClientes()) {
            grafo.agregarVertice(c.getId());
        }
        for (Transaccion t : sistema.getTransacciones()) {
            if (!t.isRevertida() && t.getTipo() == TipoTransaccion.TRANSFERENCIA) {
                grafo.agregarArista(clienteDe(t.getOrigenId()), clienteDe(t.getDestinoId()), t.getMonto());
            }
        }
        return grafo;
    }

    public GrafoDirigido<String> grafoMonederos() {
        GrafoDirigido<String> grafo = new GrafoDirigido<>();
        for (Monedero m : sistema.getMonederos()) {
            grafo.agregarVertice(m.getId());
        }
        for (Transaccion t : sistema.getTransacciones()) {
            if (!t.isRevertida() && t.getOrigenId() != null && t.getDestinoId() != null) {
                grafo.agregarArista(t.getOrigenId(), t.getDestinoId(), t.getMonto());
            }
        }
        return grafo;
    }

    /**
     * @param clienteId null para todos los clientes; las secuencias nunca
     *                  unen gastos de clientes distintos.
     */
    public GrafoDirigido<Categoria> grafoCategorias(String clienteId) {
        GrafoDirigido<Categoria> grafo = new GrafoDirigido<>();
        Mapa<String, Categoria> ultimaDeCadaCliente = new TablaHash<>();
        for (Transaccion t : sistema.getTransacciones()) { // ya estan en orden cronologico
            String quien = clienteQueGasta(t);
            if (quien == null || (clienteId != null && !clienteId.equals(quien))) {
                continue;
            }
            Categoria anterior = ultimaDeCadaCliente.obtener(quien);
            if (anterior == null) {
                grafo.agregarVertice(t.getCategoria());
            } else {
                grafo.agregarArista(anterior, t.getCategoria(), t.getMonto());
            }
            ultimaDeCadaCliente.poner(quien, t.getCategoria());
        }
        return grafo;
    }

    // ------------------------------------------------------------------ reportes

    /** 1. Cuanto gasto el cliente en cada categoria, de mayor a menor. */
    public Coleccion<FilaReporte> gastoPorCategoria(String clienteId) {
        sistema.buscarCliente(clienteId);
        Mapa<Categoria, long[]> totales = new TablaHash<>(); // [dinero, veces]
        for (Transaccion t : sistema.getTransacciones()) {
            if (clienteId.equals(clienteQueGasta(t))) {
                long[] acumulado = totales.obtener(t.getCategoria());
                if (acumulado == null) {
                    acumulado = new long[2];
                    totales.poner(t.getCategoria(), acumulado);
                }
                acumulado[0] += t.getMonto();
                acumulado[1]++;
            }
        }
        Coleccion<FilaReporte> filas = new ListaEnlazada<>();
        for (Categoria c : Categoria.values()) {
            long[] acumulado = totales.obtener(c);
            if (acumulado != null) {
                filas.agregar(new FilaReporte(c.name(), acumulado[0], (int) acumulado[1]));
            }
        }
        return mayores(filas, filas.tamanio(), POR_VALOR);
    }

    /** 2. Las n secuencias "categoria -> categoria" mas repetidas. */
    public Coleccion<FilaReporte> secuenciasFrecuentes(String clienteId, int n) {
        Coleccion<FilaReporte> filas = new ListaEnlazada<>();
        for (GrafoDirigido.Arista<Categoria> a : grafoCategorias(clienteId).aristas()) {
            filas.agregar(new FilaReporte(a.getOrigen() + " -> " + a.getDestino(), a.getPeso(), a.getVeces()));
        }
        return mayores(filas, n, POR_VECES);
    }

    /** 3. Los n clientes que mas dinero reciben por transferencias (grado de entrada ponderado). */
    public Coleccion<FilaReporte> mayoresReceptores(int n) {
        GrafoDirigido<String> grafo = grafoTransferencias();
        Coleccion<FilaReporte> filas = new ListaEnlazada<>();
        for (String id : grafo.getVertices()) {
            if (grafo.pesoEntrante(id) > 0) {
                filas.agregar(new FilaReporte(nombre(id), grafo.pesoEntrante(id), grafo.gradoEntrada(id)));
            }
        }
        return mayores(filas, n, POR_VALOR);
    }

    /** 4. Ciclos de dinero entre clientes (A -> B -> C -> A), con DFS. Ids de cliente. */
    public Coleccion<Coleccion<String>> ciclosTransferencias() {
        return grafoTransferencias().ciclos();
    }

    /**
     * 5. Como esta conectado el cliente a con el b siguiendo transferencias
     * (BFS: el camino con menos intermediarios). Vacio si no hay conexion.
     */
    public Coleccion<String> conexion(String clienteA, String clienteB) {
        sistema.buscarCliente(clienteA);
        sistema.buscarCliente(clienteB);
        return grafoTransferencias().caminoMasCorto(clienteA, clienteB);
    }

    /** 6. Por donde entra y sale el dinero de los monederos del cliente, de mayor a menor. */
    public Coleccion<FilaReporte> flujoMonederos(String clienteId) {
        Cliente cliente = sistema.buscarCliente(clienteId);
        GrafoDirigido<String> grafo = grafoMonederos();
        Coleccion<FilaReporte> filas = new ListaEnlazada<>();
        for (GrafoDirigido.Arista<String> a : grafo.aristas()) {
            boolean sale = esDe(cliente, a.getOrigen());
            boolean entra = esDe(cliente, a.getDestino());
            if (sale || entra) {
                filas.agregar(new FilaReporte(etiquetaMonedero(a.getOrigen()) + " -> "
                        + etiquetaMonedero(a.getDestino()), a.getPeso(), a.getVeces()));
            }
        }
        return mayores(filas, filas.tamanio(), POR_VALOR);
    }

    // ------------------------------------------------------------------ apoyo

    private static final Comparator<FilaReporte> POR_VALOR =
            Comparator.comparingLong(FilaReporte::valor).reversed().thenComparing(FilaReporte::etiqueta);
    private static final Comparator<FilaReporte> POR_VECES =
            Comparator.comparingInt(FilaReporte::veces).reversed()
                    .thenComparing(Comparator.comparingLong(FilaReporte::valor).reversed())
                    .thenComparing(FilaReporte::etiqueta);

    /** Los n primeros segun el comparador, usando el monticulo: O(k log k). */
    private static Coleccion<FilaReporte> mayores(Coleccion<FilaReporte> filas, int n, Comparator<FilaReporte> orden) {
        MonticuloBinario<FilaReporte> monticulo = new MonticuloBinario<>(orden);
        for (FilaReporte f : filas) {
            monticulo.insertar(f);
        }
        Coleccion<FilaReporte> resultado = new ListaEnlazada<>();
        while (!monticulo.estaVacia() && resultado.tamanio() < n) {
            resultado.agregar(monticulo.extraer());
        }
        return resultado;
    }

    /** Cliente que hizo el gasto, o null si la transaccion no es un gasto. */
    private String clienteQueGasta(Transaccion t) {
        if (t.isRevertida()) {
            return null;
        }
        if (t.getTipo() == TipoTransaccion.RETIRO || t.getTipo() == TipoTransaccion.TRANSFERENCIA) {
            return clienteDe(t.getOrigenId());
        }
        return null;
    }

    private String clienteDe(String monederoId) {
        return sistema.buscarMonedero(monederoId).getClienteId();
    }

    private String nombre(String clienteId) {
        return sistema.buscarCliente(clienteId).getNombre();
    }

    private boolean esDe(Cliente cliente, String monederoId) {
        return cliente.getId().equals(clienteDe(monederoId));
    }

    private String etiquetaMonedero(String monederoId) {
        Monedero m = sistema.buscarMonedero(monederoId);
        return m.getId() + " " + m.getNombre() + " (" + nombre(m.getClienteId()) + ")";
    }
}
