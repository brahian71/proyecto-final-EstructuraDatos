package monedero.ui;

import monedero.modelo.Beneficio;
import monedero.modelo.BeneficioActivo;
import monedero.modelo.Categoria;
import monedero.modelo.Frecuencia;
import monedero.modelo.OperacionProgramada;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.MovimientoPuntos;
import monedero.modelo.Rango;
import monedero.modelo.Notificacion;
import monedero.modelo.TipoMonedero;
import monedero.modelo.Transaccion;
import monedero.estructuras.Coleccion;
import monedero.servicio.AnalisisGastos;
import monedero.servicio.FilaReporte;
import monedero.servicio.SistemaMonedero;
import monedero.servicio.VerificadorIntegridad;

import java.util.Scanner;

/**
 * Interfaz de consola para la fase de desarrollo. Solo llama a
 * SistemaMonedero; la interfaz grafica (fase 6) reemplaza esta clase sin
 * tocar los servicios.
 */
public class MenuConsola {

    private final SistemaMonedero sistema;
    private final Scanner entrada;

    public MenuConsola(SistemaMonedero sistema, Scanner entrada) {
        this.sistema = sistema;
        this.entrada = entrada;
    }

    public void iniciar() {
        int opcion;
        do {
            mostrarMenu();
            opcion = leerEntero("Opcion: ");
            try {
                ejecutar(opcion);
            } catch (RuntimeException e) {
                // reglas de negocio (saldo insuficiente, id inexistente, monto invalido)
                System.out.println("  ERROR: " + e.getMessage());
            }
        } while (opcion != 0);
    }

    private void mostrarMenu() {
        System.out.println("\n===== MONEDERO VIRTUAL  (" + sistema.getReloj().ahora().toLocalDate() + ") =====");
        System.out.println(" 1. Listar clientes y monederos");
        System.out.println(" 2. Registrar cliente");
        System.out.println(" 3. Crear monedero");
        System.out.println(" 4. Depositar");
        System.out.println(" 5. Retirar");
        System.out.println(" 6. Transferir");
        System.out.println(" 7. Consultar saldo");
        System.out.println(" 8. Ver historial de un monedero");
        System.out.println(" 9. Verificar integridad");
        System.out.println("10. Avanzar dias (reloj simulado)");
        System.out.println("11. Revertir ultima transaccion de un cliente");
        System.out.println("12. Ver notificaciones de un cliente");
        System.out.println("13. Canjear puntos");
        System.out.println("14. Historial de puntos y beneficios de un cliente");
        System.out.println("15. Ranking y clientes por rango (arbol AVL)");
        System.out.println("16. Programar transaccion recurrente");
        System.out.println("17. Ver transacciones programadas (cola de prioridad)");
        System.out.println("18. Cancelar transaccion programada");
        System.out.println("19. Reportes de patrones de gasto (grafos)");
        System.out.println(" 0. Salir");
    }

    private void ejecutar(int opcion) {
        switch (opcion) {
            case 1 -> listar();
            case 2 -> {
                Cliente c = sistema.registrarCliente(leerTexto("Nombre: "));
                System.out.println("  Creado " + c + " con monedero principal");
            }
            case 3 -> {
                String clienteId = leerTexto("Id cliente: ");
                String nombre = leerTexto("Nombre del monedero: ");
                TipoMonedero tipo = elegir("Tipo", TipoMonedero.values());
                System.out.println("  Creado " + sistema.crearMonedero(clienteId, nombre, tipo));
            }
            case 4 -> {
                String id = leerTexto("Id monedero: ");
                long monto = leerMonto();
                informar(sistema.depositar(id, monto, leerTexto("Descripcion: ")));
            }
            case 5 -> {
                String id = leerTexto("Id monedero: ");
                long monto = leerMonto();
                Categoria categoria = elegir("Categoria", Categoria.values());
                informar(sistema.retirar(id, monto, categoria, leerTexto("Descripcion: ")));
            }
            case 6 -> {
                String origen = leerTexto("Id monedero origen: ");
                String destino = leerTexto("Id monedero destino: ");
                long monto = leerMonto();
                Categoria categoria = elegir("Categoria", Categoria.values());
                informar(sistema.transferir(origen, destino, monto, categoria, leerTexto("Descripcion: ")));
            }
            case 7 -> {
                String id = leerTexto("Id monedero: ");
                System.out.println("  Saldo: $" + sistema.consultarSaldo(id));
            }
            case 8 -> {
                String id = leerTexto("Id monedero: ");
                for (Transaccion t : sistema.historial(id)) {
                    System.out.println("  " + t + "   efecto: " + t.efectoSobre(id));
                }
            }
            case 9 -> System.out.println(new VerificadorIntegridad(sistema).verificar());
            case 10 -> {
                int procesadas = sistema.avanzarDias(leerEntero("Dias: "));
                System.out.println("  Fecha actual: " + sistema.getReloj().ahora().toLocalDate()
                        + ". Programadas procesadas: " + procesadas);
            }
            case 11 -> revertir();
            case 12 -> verNotificaciones();
            case 13 -> canjear();
            case 14 -> verPuntos();
            case 15 -> verRanking();
            case 16 -> programar();
            case 17 -> {
                for (OperacionProgramada op : sistema.getProgramador().pendientes()) {
                    System.out.println("  " + op);
                }
            }
            case 18 -> {
                sistema.getProgramador().cancelar(leerTexto("Id programada: "));
                System.out.println("  Cancelada");
            }
            case 19 -> reportes();
            case 0 -> System.out.println("Hasta luego");
            default -> System.out.println("  Opcion no valida");
        }
    }

    private void listar() {
        for (Cliente cliente : sistema.getClientes()) {
            int noLeidas = sistema.getNotificador().noLeidas(cliente.getId());
            System.out.println(cliente + "  saldo total $" + cliente.saldoTotal()
                    + (noLeidas > 0 ? "  [" + noLeidas + " notificaciones nuevas]" : ""));
            for (Monedero monedero : cliente.getMonederos()) {
                System.out.println("    " + monedero);
            }
        }
    }

    private void revertir() {
        String clienteId = leerTexto("Id cliente: ");
        Transaccion proxima = sistema.proximaReversible(clienteId);
        if (proxima == null) {
            System.out.println("  No tiene transacciones para revertir");
            return;
        }
        System.out.println("  Se revertiria: " + proxima);
        if (leerTexto("  Confirmar (s/n): ").equalsIgnoreCase("s")) {
            Transaccion t = sistema.revertirUltima(clienteId);
            System.out.println("  Revertida " + t.getId() + ". " + sistema.buscarCliente(clienteId));
        }
    }

    private void verNotificaciones() {
        String clienteId = leerTexto("Id cliente: ");
        sistema.buscarCliente(clienteId);
        boolean alguna = false;
        for (Notificacion n : sistema.getNotificador().buzon(clienteId)) {
            System.out.println("  " + n);
            alguna = true;
        }
        if (!alguna) {
            System.out.println("  Sin notificaciones");
        }
        sistema.getNotificador().marcarLeidas(clienteId);
    }

    private void programar() {
        TipoTransaccion tipo = elegir("Tipo", new TipoTransaccion[]{
                TipoTransaccion.DEPOSITO, TipoTransaccion.RETIRO, TipoTransaccion.TRANSFERENCIA});
        String origen = tipo == TipoTransaccion.DEPOSITO ? null : leerTexto("Id monedero origen: ");
        String destino = tipo == TipoTransaccion.RETIRO ? null : leerTexto("Id monedero destino: ");
        long monto = leerMonto();
        Categoria categoria = tipo == TipoTransaccion.DEPOSITO ? null : elegir("Categoria", Categoria.values());
        Frecuencia frecuencia = elegir("Frecuencia", Frecuencia.values());
        int enDias = leerEntero("Primera ejecucion en cuantos dias (0 = hoy): ");
        int veces = frecuencia == Frecuencia.UNA_VEZ ? 1 : leerEntero("Cuantas veces (0 = sin limite): ");
        OperacionProgramada op = sistema.getProgramador().programar(tipo, origen, destino, monto, categoria,
                leerTexto("Descripcion: "), frecuencia, sistema.getReloj().ahora().plusDays(enDias),
                veces == 0 ? OperacionProgramada.SIN_LIMITE : veces);
        System.out.println("  Programada " + op);
    }

    private void reportes() {
        AnalisisGastos analisis = new AnalisisGastos(sistema);
        System.out.println("   1. Gasto por categoria de un cliente");
        System.out.println("   2. Secuencias de gasto mas frecuentes");
        System.out.println("   3. Clientes que mas reciben por transferencias");
        System.out.println("   4. Ciclos de transferencias entre clientes (DFS)");
        System.out.println("   5. Conexion entre dos clientes (BFS)");
        System.out.println("   6. Flujo de dinero entre monederos de un cliente");
        switch (leerEntero("Reporte: ")) {
            case 1 -> imprimir(analisis.gastoPorCategoria(leerTexto("Id cliente: ")));
            case 2 -> {
                String id = leerTexto("Id cliente (vacio = todos): ");
                imprimir(analisis.secuenciasFrecuentes(id.isEmpty() ? null : id, 5));
            }
            case 3 -> imprimir(analisis.mayoresReceptores(5));
            case 4 -> {
                Coleccion<Coleccion<String>> ciclos = analisis.ciclosTransferencias();
                if (ciclos.estaVacia()) {
                    System.out.println("  No hay ciclos");
                }
                for (Coleccion<String> ciclo : ciclos) {
                    System.out.println("  " + String.join(" -> ", ciclo));
                }
            }
            case 5 -> {
                Coleccion<String> camino = analisis.conexion(leerTexto("Cliente A: "), leerTexto("Cliente B: "));
                System.out.println(camino.estaVacia() ? "  No estan conectados"
                        : "  " + String.join(" -> ", camino) + " (" + (camino.tamanio() - 1) + " pasos)");
            }
            case 6 -> imprimir(analisis.flujoMonederos(leerTexto("Id cliente: ")));
            default -> System.out.println("  Reporte no valido");
        }
    }

    private static void imprimir(Iterable<FilaReporte> filas) {
        boolean alguna = false;
        for (FilaReporte f : filas) {
            System.out.println("  " + f);
            alguna = true;
        }
        if (!alguna) {
            System.out.println("  Sin datos");
        }
    }

    private void canjear() {
        String clienteId = leerTexto("Id cliente: ");
        Cliente cliente = sistema.buscarCliente(clienteId);
        System.out.println("  Disponibles: " + cliente.getPuntosDisponibles() + " pts");
        for (Beneficio b : Beneficio.values()) {
            System.out.println("   " + (b.ordinal() + 1) + ". " + b.getCostoPuntos() + " pts - " + b.getDescripcion());
        }
        Beneficio beneficio = elegir("Beneficio", Beneficio.values());
        sistema.canjear(clienteId, beneficio);
        System.out.println("  OK. " + cliente);
    }

    private void verPuntos() {
        String clienteId = leerTexto("Id cliente: ");
        Cliente cliente = sistema.buscarCliente(clienteId);
        System.out.println("  " + cliente + "  (canjeados: " + cliente.getPuntosCanjeados() + ")");
        for (MovimientoPuntos m : sistema.historialPuntos(clienteId)) {
            System.out.println("    " + m);
        }
        for (BeneficioActivo b : sistema.beneficios(clienteId)) {
            boolean vigente = b.vigenteEn(sistema.getReloj().ahora());
            System.out.println("  Beneficio " + b + (vigente ? " (vigente)" : " (vencido)"));
        }
    }

    private void verRanking() {
        System.out.println("  Top 5:");
        int puesto = 1;
        for (Cliente c : sistema.ranking(5)) {
            System.out.println("   " + puesto++ + ". " + c);
        }
        for (Rango rango : Rango.values()) {
            System.out.print("  " + rango + ":");
            for (Cliente c : sistema.clientesDeRango(rango)) {
                System.out.print(" " + c.getId() + "(" + c.getPuntos() + ")");
            }
            System.out.println();
        }
        System.out.println("  Arbol AVL: " + sistema.getRegistroPuntos().tamanio() + " nodos, altura "
                + sistema.getRegistroPuntos().altura() + ", " + sistema.getRegistroPuntos().rotaciones() + " rotaciones");
    }

    private void informar(Transaccion t) {
        System.out.println("  OK " + t);
        System.out.println("  Puntos del cliente: " + sistema.buscarCliente(sistema.clienteQueGanaPuntos(t)));
    }

    // ------------------------------------------------------------------ lectura

    private String leerTexto(String mensaje) {
        System.out.print(mensaje);
        return entrada.nextLine().trim();
    }

    private int leerEntero(String mensaje) {
        while (true) {
            try {
                return Integer.parseInt(leerTexto(mensaje));
            } catch (NumberFormatException e) {
                System.out.println("  Escriba un numero entero");
            }
        }
    }

    private long leerMonto() {
        while (true) {
            try {
                return Long.parseLong(leerTexto("Monto: "));
            } catch (NumberFormatException e) {
                System.out.println("  Escriba un monto entero (sin puntos ni comas)");
            }
        }
    }

    private <E extends Enum<E>> E elegir(String titulo, E[] opciones) {
        for (int i = 0; i < opciones.length; i++) {
            System.out.println("   " + (i + 1) + ". " + opciones[i]);
        }
        while (true) {
            int i = leerEntero(titulo + ": ");
            if (i >= 1 && i <= opciones.length) {
                return opciones[i - 1];
            }
            System.out.println("  Elija entre 1 y " + opciones.length);
        }
    }
}
