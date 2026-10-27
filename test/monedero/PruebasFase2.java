package monedero;

import monedero.datos.DatosIniciales;
import monedero.estructuras.ListaCircular;
import monedero.estructuras.Pila;
import monedero.estructuras.PilaEnlazada;
import monedero.excepciones.OperacionInvalidaException;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.Notificacion;
import monedero.modelo.Rango;
import monedero.modelo.TipoMonedero;
import monedero.modelo.TipoNotificacion;
import monedero.modelo.Transaccion;
import monedero.servicio.Notificador;
import monedero.servicio.Reloj;
import monedero.servicio.SistemaMonedero;
import monedero.servicio.VerificadorIntegridad;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Pruebas de la fase 2: pila, lista circular, reversion con recalculo de
 * puntos y notificaciones. Sin JUnit, cada prueba imprime OK o FALLO.
 */
public class PruebasFase2 {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 1, 8, 0);

    private static int exitos = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        pilaLifo();
        pilaVacia();
        circularSinLlenar();
        circularSobrescribeLaMasVieja();
        circularCapacidadUno();
        circularVacia();

        revertirDeposito();
        revertirRetiroDevuelvePlata();
        revertirTransferencia();
        revertirEsLifo();
        revertirSinTransacciones();
        noRevertirPlataYaGastada();
        revertirBajaDeRango();
        integridadDespuesDeRevertir();

        notificaSaldoBajo();
        notificaTransferenciaRecibida();
        notificaSubidaDeRango();
        buzonGuardaSoloLasUltimas();
        marcarLeidas();

        System.out.println("\nResultado: " + exitos + " OK, " + fallos + " FALLO");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ pila

    private static void pilaLifo() {
        Pila<String> p = new PilaEnlazada<>();
        p.apilar("A");
        p.apilar("B");
        p.apilar("C");
        verificar("pila: cima es la ultima", "C".equals(p.cima()) && p.tamanio() == 3);
        verificar("pila: recorre de cima a fondo", contenido(p).equals("C B A"));
        verificar("pila: desapila en orden inverso",
                "C".equals(p.desapilar()) && "B".equals(p.desapilar()) && "A".equals(p.desapilar()));
        verificar("pila: queda vacia", p.estaVacia());
    }

    private static void pilaVacia() {
        Pila<Integer> p = new PilaEnlazada<>();
        lanza("pila vacia: desapilar lanza", NoSuchElementException.class, p::desapilar);
        lanza("pila vacia: cima lanza", NoSuchElementException.class, p::cima);
        p.apilar(1);
        p.desapilar();
        p.apilar(2);
        verificar("pila: se reutiliza tras vaciar", p.cima() == 2 && p.tamanio() == 1);
    }

    // ------------------------------------------------------------------ lista circular

    private static void circularSinLlenar() {
        ListaCircular<String> l = new ListaCircular<>(5);
        l.agregar("A");
        l.agregar("B");
        l.agregar("C");
        verificar("circular: orden de llegada", contenido(l).equals("A B C"));
        verificar("circular: primero y ultimo", "A".equals(l.primero()) && "C".equals(l.ultimo()));
        verificar("circular: no llena", !l.estaLlena() && l.tamanio() == 3 && l.sobrescritos() == 0);
    }

    private static void circularSobrescribeLaMasVieja() {
        ListaCircular<String> l = new ListaCircular<>(3);
        for (String s : new String[]{"A", "B", "C", "D", "E"}) {
            l.agregar(s);
        }
        verificar("circular: quedan las 3 mas nuevas", contenido(l).equals("C D E"));
        verificar("circular: tamanio no pasa la capacidad", l.tamanio() == 3 && l.estaLlena());
        verificar("circular: cuenta sobrescritos", l.sobrescritos() == 2);
        verificar("circular: primero es la mas vieja que queda", "C".equals(l.primero()) && "E".equals(l.ultimo()));
    }

    private static void circularCapacidadUno() {
        ListaCircular<Integer> l = new ListaCircular<>(1);
        l.agregar(1);
        l.agregar(2);
        verificar("circular cap 1: solo la ultima", contenido(l).equals("2") && l.primero() == 2);
    }

    private static void circularVacia() {
        ListaCircular<String> l = new ListaCircular<>(2);
        verificar("circular vacia: iterador sin elementos", !l.iterator().hasNext());
        lanza("circular vacia: primero lanza", NoSuchElementException.class, l::primero);
        lanza("circular: capacidad 0 lanza", IllegalArgumentException.class, () -> new ListaCircular<String>(0));
    }

    // ------------------------------------------------------------------ reversion

    private static void revertirDeposito() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        Transaccion t = s.depositar(m, 1000, "x");
        s.revertirUltima(ana.getId());
        verificar("revertir deposito: saldo vuelve a 0", s.consultarSaldo(m) == 0);
        verificar("revertir deposito: puntos vuelven a 0", ana.getPuntos() == 0);
        verificar("revertir deposito: sigue en historial marcada", t.isRevertida()
                && s.historial(m).iterator().next().equals(t));
    }

    private static void revertirRetiroDevuelvePlata() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        s.depositar(m, 1000, "x");
        s.retirar(m, 400, Categoria.OTROS, "x");
        s.revertirUltima(ana.getId());
        verificar("revertir retiro: devuelve la plata", s.consultarSaldo(m) == 1000);
        verificar("revertir retiro: quita sus 8 puntos", ana.getPuntos() == 10);
    }

    private static void revertirTransferencia() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        String a = principal(ana);
        String b = principal(bruno);
        s.depositar(a, 1000, "x");
        s.transferir(a, b, 600, Categoria.OTROS, "x");
        s.revertirUltima(ana.getId());
        verificar("revertir transferencia: origen recupera", s.consultarSaldo(a) == 1000);
        verificar("revertir transferencia: destino devuelve", s.consultarSaldo(b) == 0);
        verificar("revertir transferencia: puntos recalculados", ana.getPuntos() == 10);
        verificar("revertir transferencia: avisa al que recibio",
                ultimaDe(s, bruno.getId()).getTipo() == TipoNotificacion.REVERSION);
    }

    private static void revertirEsLifo() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        Transaccion t1 = s.depositar(m, 100, "1");
        Transaccion t2 = s.depositar(m, 200, "2");
        verificar("lifo: la proxima es la ultima", s.proximaReversible(ana.getId()).equals(t2));
        s.revertirUltima(ana.getId());
        verificar("lifo: despues sigue la anterior", s.proximaReversible(ana.getId()).equals(t1));
        verificar("lifo: solo se revirtio la ultima", t2.isRevertida() && !t1.isRevertida());
    }

    private static void revertirSinTransacciones() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        lanza("revertir: sin transacciones lanza", OperacionInvalidaException.class,
                () -> s.revertirUltima(ana.getId()));
        verificar("revertir: proxima es null", s.proximaReversible(ana.getId()) == null);
    }

    /**
     * Bruno le manda 100 a Ana, Ana los gasta y luego deposita otros 100.
     * El saldo actual de Ana alcanza, pero la plata de Bruno ya se uso:
     * revertirla dejaria el historial de Ana en -100.
     */
    private static void noRevertirPlataYaGastada() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        String a = principal(ana);
        String b = principal(bruno);
        s.depositar(b, 100, "x");
        s.transferir(b, a, 100, Categoria.OTROS, "x");
        s.retirar(a, 100, Categoria.OTROS, "x");
        s.depositar(a, 100, "x");
        lanza("plata gastada: no se puede revertir", OperacionInvalidaException.class,
                () -> s.revertirUltima(bruno.getId()));
        verificar("plata gastada: nada cambio", s.consultarSaldo(a) == 100 && s.consultarSaldo(b) == 0
                && bruno.tieneReversibles());
    }

    private static void revertirBajaDeRango() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        s.depositar(m, 40_000, "x");
        s.depositar(m, 20_000, "x");
        verificar("rango: sube a plata con 600 pts", ana.getRango() == Rango.PLATA);
        s.revertirUltima(ana.getId());
        verificar("rango: baja a bronce al revertir", ana.getRango() == Rango.BRONCE && ana.getPuntos() == 400);
        verificar("rango: avisa la bajada", ultimaDe(s, ana.getId()).getTipo() == TipoNotificacion.CAMBIO_RANGO);
    }

    private static void integridadDespuesDeRevertir() {
        SistemaMonedero s = nuevo();
        DatosIniciales.cargar(s);
        int revertidas = 0;
        for (Cliente c : s.getClientes()) {
            try {
                s.revertirUltima(c.getId());
                revertidas++;
            } catch (OperacionInvalidaException e) {
                // puede que la ultima no se pueda revertir; no es lo que se prueba aqui
            }
        }
        verificar("integridad: OK tras " + revertidas + " reversiones",
                revertidas > 0 && new VerificadorIntegridad(s).verificar().esValido());
    }

    // ------------------------------------------------------------------ notificaciones

    private static void notificaSaldoBajo() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        s.depositar(m, 5000, "x");
        s.retirar(m, 1000, Categoria.OTROS, "x");
        verificar("saldo bajo: no avisa con 4000", s.getNotificador().noLeidas(ana.getId()) == 0);
        s.retirar(m, 3500, Categoria.OTROS, "x");
        verificar("saldo bajo: avisa con 500", ultimaDe(s, ana.getId()).getTipo() == TipoNotificacion.SALDO_BAJO);
    }

    private static void notificaTransferenciaRecibida() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        Monedero ahorros = s.crearMonedero(ana.getId(), "Ahorros", TipoMonedero.AHORROS);
        s.depositar(principal(ana), 5000, "x");
        s.transferir(principal(ana), ahorros.getId(), 1000, Categoria.AHORRO, "x");
        verificar("recibida: movimiento interno no avisa", s.getNotificador().noLeidas(ana.getId()) == 0);
        s.transferir(principal(ana), principal(bruno), 1000, Categoria.OTROS, "x");
        verificar("recibida: avisa al destino",
                ultimaDe(s, bruno.getId()).getTipo() == TipoNotificacion.TRANSFERENCIA_RECIBIDA);
    }

    private static void notificaSubidaDeRango() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        s.depositar(principal(ana), 50_100, "x");
        Notificacion n = ultimaDe(s, ana.getId());
        verificar("rango: avisa subida a plata", n.getTipo() == TipoNotificacion.CAMBIO_RANGO
                && n.getMensaje().contains("PLATA"));
    }

    private static void buzonGuardaSoloLasUltimas() {
        Notificador notificador = new Notificador();
        for (int i = 1; i <= Notificador.CAPACIDAD + 5; i++) {
            notificador.notificar("C001", TipoNotificacion.RECORDATORIO, "aviso " + i, INICIO);
        }
        int cuenta = 0;
        String primera = null;
        for (Notificacion n : notificador.buzon("C001")) {
            if (primera == null) {
                primera = n.getMensaje();
            }
            cuenta++;
        }
        verificar("buzon: guarda solo CAPACIDAD", cuenta == Notificador.CAPACIDAD);
        verificar("buzon: se perdieron las mas viejas", "aviso 6".equals(primera));
    }

    private static void marcarLeidas() {
        Notificador notificador = new Notificador();
        notificador.notificar("C001", TipoNotificacion.RECORDATORIO, "a", INICIO);
        notificador.notificar("C001", TipoNotificacion.RECORDATORIO, "b", INICIO);
        verificar("leidas: 2 nuevas", notificador.noLeidas("C001") == 2);
        notificador.marcarLeidas("C001");
        verificar("leidas: 0 tras marcar", notificador.noLeidas("C001") == 0);
        verificar("leidas: cliente sin buzon", notificador.noLeidas("C999") == 0);
    }

    // ------------------------------------------------------------------ apoyo

    private static SistemaMonedero nuevo() {
        return new SistemaMonedero(new Reloj(INICIO));
    }

    private static String principal(Cliente cliente) {
        return cliente.getMonederos().iterator().next().getId();
    }

    private static Notificacion ultimaDe(SistemaMonedero s, String clienteId) {
        Notificacion ultima = null;
        for (Notificacion n : s.getNotificador().buzon(clienteId)) {
            ultima = n;
        }
        return ultima;
    }

    private static String contenido(Iterable<?> elementos) {
        StringBuilder sb = new StringBuilder();
        for (Object e : elementos) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(e);
        }
        return sb.toString();
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
