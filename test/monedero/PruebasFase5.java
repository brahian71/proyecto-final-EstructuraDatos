package monedero;

import monedero.estructuras.Cola;
import monedero.estructuras.ColaEnlazada;
import monedero.estructuras.Coleccion;
import monedero.estructuras.GrafoDirigido;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.TipoMonedero;
import monedero.servicio.AnalisisGastos;
import monedero.servicio.FilaReporte;
import monedero.servicio.PoliticaComision;
import monedero.servicio.Reloj;
import monedero.servicio.SistemaMonedero;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Pruebas de la fase 5: cola FIFO, grafo dirigido (BFS, DFS, ciclos) y los
 * reportes de patrones de gasto. Sin JUnit, cada prueba imprime OK o FALLO.
 */
public class PruebasFase5 {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 1, 8, 0);

    private static int exitos = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        colaFifo();
        colaVacia();

        grafoAcumulaPesos();
        grafoGrados();
        bfsCaminoMasCorto();
        bfsSinCamino();
        dfsAlcanzables();
        ciclosEncontrados();
        sinCiclos();

        reporteGastoPorCategoria();
        reporteSecuencias();
        reporteSecuenciasNoMezclaClientes();
        reporteMayoresReceptores();
        reporteCiclosYConexion();
        reporteFlujoMonederos();
        reportesIgnoranRevertidas();

        System.out.println("\nResultado: " + exitos + " OK, " + fallos + " FALLO");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ cola

    private static void colaFifo() {
        Cola<String> c = new ColaEnlazada<>();
        c.encolar("A");
        c.encolar("B");
        c.encolar("C");
        verificar("cola: frente es el primero", "A".equals(c.frente()) && c.tamanio() == 3);
        verificar("cola: sale en orden de llegada",
                "A".equals(c.desencolar()) && "B".equals(c.desencolar()) && "C".equals(c.desencolar()));
        c.encolar("D");
        verificar("cola: se reutiliza tras vaciarse", "D".equals(c.frente()) && c.tamanio() == 1);
    }

    private static void colaVacia() {
        Cola<Integer> c = new ColaEnlazada<>();
        lanza("cola vacia: desencolar lanza", NoSuchElementException.class, c::desencolar);
        verificar("cola vacia: iterador sin elementos", !c.iterator().hasNext());
    }

    // ------------------------------------------------------------------ grafo

    private static void grafoAcumulaPesos() {
        GrafoDirigido<String> g = new GrafoDirigido<>();
        g.agregarArista("A", "B", 100);
        g.agregarArista("A", "B", 50);
        g.agregarArista("B", "A", 10);
        verificar("grafo: suma pesos en la misma arista", g.arista("A", "B").getPeso() == 150
                && g.arista("A", "B").getVeces() == 2);
        verificar("grafo: es dirigido", g.arista("B", "A").getPeso() == 10);
        verificar("grafo: no duplica aristas", g.cantidadAristas() == 2 && g.cantidadVertices() == 2);
        verificar("grafo: arista inexistente es null", g.arista("A", "Z") == null);
    }

    private static void grafoGrados() {
        GrafoDirigido<String> g = new GrafoDirigido<>();
        g.agregarArista("A", "C", 10);
        g.agregarArista("B", "C", 20);
        g.agregarArista("C", "D", 5);
        verificar("grafo: grado de entrada", g.gradoEntrada("C") == 2 && g.gradoEntrada("A") == 0);
        verificar("grafo: grado de salida", g.gradoSalida("C") == 1 && g.gradoSalida("D") == 0);
        verificar("grafo: pesos entrante y saliente", g.pesoEntrante("C") == 30 && g.pesoSaliente("C") == 5);
    }

    private static void bfsCaminoMasCorto() {
        GrafoDirigido<String> g = new GrafoDirigido<>();
        // largo: A-B-C-D-E ; corto: A-X-E
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 1);
        g.agregarArista("C", "D", 1);
        g.agregarArista("D", "E", 1);
        g.agregarArista("A", "X", 1);
        g.agregarArista("X", "E", 1);
        verificar("bfs: el de menos pasos", texto(g.caminoMasCorto("A", "E")).equals("A X E"));
        verificar("bfs: camino a si mismo", texto(g.caminoMasCorto("A", "A")).equals("A"));
    }

    private static void bfsSinCamino() {
        GrafoDirigido<String> g = new GrafoDirigido<>();
        g.agregarArista("A", "B", 1);
        g.agregarVertice("Z");
        verificar("bfs: respeta la direccion", g.caminoMasCorto("B", "A").estaVacia());
        verificar("bfs: vertice aislado", g.caminoMasCorto("A", "Z").estaVacia());
        verificar("bfs: vertice inexistente", g.caminoMasCorto("A", "Q").estaVacia());
    }

    private static void dfsAlcanzables() {
        GrafoDirigido<String> g = new GrafoDirigido<>();
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 1);
        g.agregarArista("C", "A", 1);
        g.agregarArista("D", "A", 1);
        verificar("dfs: alcanzables desde A", texto(g.alcanzables("A")).equals("B C"));
        verificar("dfs: desde D llega a todos", g.alcanzables("D").tamanio() == 3);
    }

    private static void ciclosEncontrados() {
        GrafoDirigido<String> g = new GrafoDirigido<>();
        g.agregarArista("A", "B", 1);
        g.agregarArista("B", "C", 1);
        g.agregarArista("C", "A", 1);
        g.agregarArista("C", "D", 1);
        Coleccion<Coleccion<String>> ciclos = g.ciclos();
        verificar("ciclos: encuentra A -> B -> C -> A", ciclos.tamanio() == 1
                && texto(ciclos.obtener(0)).equals("A B C A"));
    }

    private static void sinCiclos() {
        GrafoDirigido<String> g = new GrafoDirigido<>();
        g.agregarArista("A", "B", 1);
        g.agregarArista("A", "C", 1);
        g.agregarArista("B", "D", 1);
        g.agregarArista("C", "D", 1); // diamante: dos caminos a D pero ningun ciclo
        verificar("ciclos: diamante no es ciclo", g.ciclos().estaVacia());
    }

    // ------------------------------------------------------------------ reportes

    private static void reporteGastoPorCategoria() {
        Escenario e = new Escenario();
        e.s.retirar(e.a, 1_000, Categoria.TRANSPORTE, "");
        e.s.retirar(e.a, 5_000, Categoria.ALIMENTACION, "");
        e.s.retirar(e.a, 2_000, Categoria.TRANSPORTE, "");
        e.s.transferir(e.a, e.ahorrosAna, 9_000, Categoria.AHORRO, ""); // interno: no es gasto
        Coleccion<FilaReporte> filas = e.analisis.gastoPorCategoria(e.ana.getId());
        verificar("gasto: ordenado de mayor a menor", filas.tamanio() == 2
                && filas.obtener(0).etiqueta().equals("ALIMENTACION") && filas.obtener(1).valor() == 3_000);
        verificar("gasto: cuenta veces", filas.obtener(1).veces() == 2);
    }

    private static void reporteSecuencias() {
        Escenario e = new Escenario();
        for (int i = 0; i < 3; i++) {
            e.s.retirar(e.a, 1_000, Categoria.TRANSPORTE, "");
            e.s.retirar(e.a, 1_000, Categoria.ALIMENTACION, "");
        }
        Coleccion<FilaReporte> top = e.analisis.secuenciasFrecuentes(e.ana.getId(), 2);
        verificar("secuencias: la mas frecuente primero", top.obtener(0).etiqueta().equals("TRANSPORTE -> ALIMENTACION")
                && top.obtener(0).veces() == 3);
        verificar("secuencias: limita a n", top.tamanio() == 2 && top.obtener(1).veces() == 2);
    }

    private static void reporteSecuenciasNoMezclaClientes() {
        Escenario e = new Escenario();
        e.s.retirar(e.a, 1_000, Categoria.SALUD, "");
        e.s.retirar(e.b, 1_000, Categoria.EDUCACION, ""); // de otro cliente: no forma SALUD -> EDUCACION
        verificar("secuencias: no une gastos de clientes distintos",
                e.analisis.secuenciasFrecuentes(null, 10).estaVacia());
    }

    private static void reporteMayoresReceptores() {
        Escenario e = new Escenario();
        e.s.transferir(e.a, e.c, 4_000, Categoria.OTROS, "");
        e.s.transferir(e.b, e.c, 3_000, Categoria.OTROS, "");
        e.s.transferir(e.a, e.b, 1_000, Categoria.OTROS, "");
        Coleccion<FilaReporte> top = e.analisis.mayoresReceptores(5);
        verificar("receptores: Carla primero con $7.000 de 2 clientes", top.obtener(0).etiqueta().equals("Carla")
                && top.obtener(0).valor() == 7_000 && top.obtener(0).veces() == 2);
        verificar("receptores: Ana no recibio, no aparece", top.tamanio() == 2);
    }

    private static void reporteCiclosYConexion() {
        Escenario e = new Escenario();
        e.s.transferir(e.a, e.b, 1_000, Categoria.OTROS, "");
        e.s.transferir(e.b, e.c, 1_000, Categoria.OTROS, "");
        verificar("conexion: Ana llega a Carla por Bruno",
                texto(e.analisis.conexion(e.ana.getId(), e.carla.getId())).equals("C001 C002 C003"));
        verificar("conexion: al reves no hay", e.analisis.conexion(e.carla.getId(), e.ana.getId()).estaVacia());
        verificar("ciclos: aun no hay", e.analisis.ciclosTransferencias().estaVacia());
        e.s.transferir(e.c, e.a, 1_000, Categoria.OTROS, "");
        verificar("ciclos: se cierra Ana -> Bruno -> Carla -> Ana", e.analisis.ciclosTransferencias().tamanio() == 1);
    }

    private static void reporteFlujoMonederos() {
        Escenario e = new Escenario();
        e.s.transferir(e.a, e.ahorrosAna, 5_000, Categoria.AHORRO, "");
        e.s.transferir(e.a, e.ahorrosAna, 2_000, Categoria.AHORRO, "");
        e.s.transferir(e.b, e.a, 1_000, Categoria.OTROS, "");
        Coleccion<FilaReporte> flujo = e.analisis.flujoMonederos(e.ana.getId());
        verificar("flujo: interno acumulado primero", flujo.obtener(0).valor() == 7_000 && flujo.obtener(0).veces() == 2);
        verificar("flujo: incluye lo que entra de otro cliente", flujo.tamanio() == 2);
    }

    private static void reportesIgnoranRevertidas() {
        Escenario e = new Escenario();
        e.s.transferir(e.a, e.b, 1_000, Categoria.OTROS, "");
        e.s.revertirUltima(e.ana.getId());
        verificar("revertidas: no cuentan en el grafo", e.analisis.grafoTransferencias().cantidadAristas() == 0);
        verificar("revertidas: no cuentan como gasto", e.analisis.gastoPorCategoria(e.ana.getId()).estaVacia());
    }

    // ------------------------------------------------------------------ apoyo

    /** Tres clientes con saldo; Ana tiene ademas un monedero de ahorros. */
    private static class Escenario {
        final SistemaMonedero s = new SistemaMonedero(new Reloj(INICIO), PoliticaComision.SIN_COMISION);
        final AnalisisGastos analisis = new AnalisisGastos(s);
        final Cliente ana = s.registrarCliente("Ana");
        final Cliente bruno = s.registrarCliente("Bruno");
        final Cliente carla = s.registrarCliente("Carla");
        final String a = principal(ana);
        final String b = principal(bruno);
        final String c = principal(carla);
        final String ahorrosAna;

        Escenario() {
            Monedero ahorros = s.crearMonedero(ana.getId(), "Ahorros", TipoMonedero.AHORROS);
            ahorrosAna = ahorros.getId();
            s.depositar(a, 100_000, "");
            s.depositar(b, 100_000, "");
            s.depositar(c, 100_000, "");
        }
    }

    private static String principal(Cliente cliente) {
        return cliente.getMonederos().iterator().next().getId();
    }

    private static String texto(Iterable<String> elementos) {
        return String.join(" ", elementos);
    }

    private static void lanza(String nombre, Class<? extends Throwable> esperada, Runnable accion) {
        try {
            accion.run();
            verificar(nombre + " (no lanzo nada)", false);
        } catch (Throwable e) {
            verificar(nombre, esperada.isInstance(e));
        }
    }

    private static void verificar(String nombre, boolean condicion) {
        if (condicion) {
            exitos++;
            System.out.println("OK    " + nombre);
        } else {
            fallos++;
            System.out.println("FALLO " + nombre);
        }
    }
}
