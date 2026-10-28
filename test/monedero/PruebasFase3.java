package monedero;

import monedero.datos.DatosIniciales;
import monedero.estructuras.ArbolAVL;
import monedero.estructuras.ArbolBST;
import monedero.estructuras.TablaHash;
import monedero.excepciones.OperacionInvalidaException;
import monedero.modelo.Beneficio;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.MovimientoPuntos;
import monedero.modelo.Rango;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Transaccion;
import monedero.servicio.ComisionEstandar;
import monedero.servicio.Reloj;
import monedero.servicio.SistemaMonedero;
import monedero.servicio.VerificadorIntegridad;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

/**
 * Pruebas de la fase 3: tabla hash, BST, AVL, puntos acumulados/disponibles,
 * canje, comisiones y ranking. Sin JUnit, cada prueba imprime OK o FALLO.
 */
public class PruebasFase3 {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 1, 8, 0);

    private static int exitos = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        hashBasico();
        hashReemplazaYElimina();
        hashRedimensiona();
        hashColisiones();

        bstInsertarYBuscar();
        bstEliminarTresCasos();
        bstRangoYMayores();
        bstVacio();
        bstSeDegeneraConClavesOrdenadas();
        avlSeMantieneBalanceado();
        avlEliminarMantieneBalance();

        comisionTransferencia();
        comisionDescuentoPorRango();
        comisionRetiro();
        comisionSeDevuelveAlRevertir();

        canjeGastaDisponiblesNoRango();
        canjeSinPuntos();
        canjeDescuentoTransferencias();
        canjeRetirosSinCargoVence();
        canjeBono();
        noRevertirSiPuntosCanjeados();
        historialPuntosCuadra();

        rankingYRangos();
        arbolSigueAlCambiarPuntos();
        integridadConTodo();

        System.out.println("\nResultado: " + exitos + " OK, " + fallos + " FALLO");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ tabla hash

    private static void hashBasico() {
        TablaHash<String, Integer> t = new TablaHash<>();
        verificar("hash: vacia", t.estaVacio() && t.obtener("x") == null && !t.contiene("x"));
        t.poner("a", 1);
        t.poner("b", 2);
        verificar("hash: obtener", t.obtener("a") == 1 && t.obtener("b") == 2 && t.tamanio() == 2);
        lanza("hash: clave nula lanza", NullPointerException.class, () -> t.poner(null, 3));
    }

    private static void hashReemplazaYElimina() {
        TablaHash<String, Integer> t = new TablaHash<>();
        t.poner("a", 1);
        t.poner("a", 9);
        verificar("hash: reemplaza sin duplicar", t.obtener("a") == 9 && t.tamanio() == 1);
        verificar("hash: eliminar devuelve valor", t.eliminar("a") == 9 && t.estaVacio());
        verificar("hash: eliminar inexistente", t.eliminar("zzz") == null);
    }

    private static void hashRedimensiona() {
        TablaHash<Integer, Integer> t = new TablaHash<>(4);
        for (int i = 0; i < 1000; i++) {
            t.poner(i, i * 2);
        }
        boolean todos = true;
        for (int i = 0; i < 1000; i++) {
            todos &= t.obtener(i) == i * 2;
        }
        int recorridos = 0;
        for (Integer ignorado : t.valores()) {
            recorridos++;
        }
        verificar("hash: 1000 claves tras crecer", todos && t.tamanio() == 1000 && recorridos == 1000);
        verificar("hash: capacidad crecio (" + t.capacidad() + ")", t.capacidad() >= 1000 / 0.75);
        verificar("hash: cubetas cortas (" + t.cubetaMasLarga() + ")", t.cubetaMasLarga() <= 8);
    }

    /** Claves distintas con el mismo hashCode caen en la misma cubeta. */
    private static void hashColisiones() {
        record Mismo(int id) {
            @Override
            public int hashCode() {
                return 42;
            }
        }
        TablaHash<Mismo, String> t = new TablaHash<>();
        for (int i = 0; i < 20; i++) {
            t.poner(new Mismo(i), "v" + i);
        }
        t.eliminar(new Mismo(7));
        verificar("hash: colisiones se resuelven", "v13".equals(t.obtener(new Mismo(13)))
                && t.obtener(new Mismo(7)) == null && t.tamanio() == 19);
    }

    // ------------------------------------------------------------------ BST y AVL

    private static void bstInsertarYBuscar() {
        ArbolBST<Integer, String> a = new ArbolBST<>();
        for (int k : new int[]{50, 30, 70, 20, 40, 60, 80}) {
            a.insertar(k, "v" + k);
        }
        verificar("bst: buscar", "v40".equals(a.buscar(40)) && a.buscar(45) == null);
        verificar("bst: inorden ordenado", contenido(a.enOrden()).equals("v20 v30 v40 v50 v60 v70 v80"));
        verificar("bst: min/max/altura", a.minimo() == 20 && a.maximo() == 80 && a.altura() == 3);
        a.insertar(40, "nuevo");
        verificar("bst: clave repetida reemplaza", "nuevo".equals(a.buscar(40)) && a.tamanio() == 7);
        verificar("bst: valido", a.esValido());
    }

    private static void bstEliminarTresCasos() {
        ArbolBST<Integer, String> a = new ArbolBST<>();
        for (int k : new int[]{50, 30, 70, 20, 40, 60, 80, 65}) {
            a.insertar(k, "v" + k);
        }
        verificar("bst eliminar: hoja", a.eliminar(20) && !a.contiene(20));
        verificar("bst eliminar: un hijo", a.eliminar(60) && a.contiene(65));
        verificar("bst eliminar: dos hijos (raiz)", a.eliminar(50) && !a.contiene(50));
        verificar("bst eliminar: inexistente", !a.eliminar(999));
        verificar("bst eliminar: sigue ordenado y valido",
                contenido(a.enOrden()).equals("v30 v40 v65 v70 v80") && a.esValido() && a.tamanio() == 5);
    }

    private static void bstRangoYMayores() {
        ArbolBST<Integer, Integer> a = new ArbolAVL<>();
        for (int k = 1; k <= 20; k++) {
            a.insertar(k, k);
        }
        verificar("rango: [5, 9]", contenido(a.rango(5, 9)).equals("5 6 7 8 9"));
        verificar("rango: fuera de limites", a.rango(30, 40).estaVacia());
        verificar("mayores: top 3", contenido(a.mayores(3)).equals("20 19 18"));
        verificar("mayores: pide mas de los que hay", a.mayores(50).tamanio() == 20);
    }

    private static void bstVacio() {
        ArbolBST<Integer, String> a = new ArbolBST<>();
        verificar("bst vacio: altura 0 y valido", a.altura() == 0 && a.esValido() && a.enOrden().estaVacia());
        lanza("bst vacio: minimo lanza", NoSuchElementException.class, a::minimo);
    }

    /** El argumento del informe: con claves en orden el BST simple queda como lista. */
    private static void bstSeDegeneraConClavesOrdenadas() {
        ArbolBST<Integer, Integer> bst = new ArbolBST<>();
        ArbolAVL<Integer, Integer> avl = new ArbolAVL<>();
        for (int k = 1; k <= 1000; k++) {
            bst.insertar(k, k);
            avl.insertar(k, k);
        }
        System.out.println("      1000 claves ordenadas: altura BST = " + bst.altura() + ", altura AVL = " + avl.altura());
        verificar("degenerado: BST altura 1000", bst.altura() == 1000);
        verificar("degenerado: AVL altura <= 1.44 log2(n)", avl.altura() <= 15);
    }

    private static void avlSeMantieneBalanceado() {
        ArbolAVL<Integer, Integer> a = new ArbolAVL<>();
        for (int k = 1000; k >= 1; k--) {
            a.insertar(k, k);
        }
        for (int k = 0; k < 500; k++) {
            a.insertar(2000 + (k * 7919) % 1000, k);
        }
        verificar("avl: valido tras 1500 inserciones", a.esValido() && a.tamanio() == 1500);
        verificar("avl: hizo rotaciones", a.rotaciones() > 0);
    }

    private static void avlEliminarMantieneBalance() {
        ArbolAVL<Integer, Integer> a = new ArbolAVL<>();
        for (int k = 1; k <= 200; k++) {
            a.insertar(k, k);
        }
        for (int k = 1; k <= 150; k++) {
            a.eliminar(k);
        }
        verificar("avl: valido tras eliminar 150", a.esValido() && a.tamanio() == 50 && a.minimo() == 151);
        verificar("avl: altura baja (" + a.altura() + ")", a.altura() <= 8);
    }

    // ------------------------------------------------------------------ comisiones

    private static void comisionTransferencia() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        String a = principal(ana);
        s.depositar(a, 40_000, "x"); // 400 pts: Bronce, sin descuento
        Transaccion chica = s.transferir(a, principal(bruno), 10_000, Categoria.OTROS, "x");
        verificar("comision: minimo $500", chica.getComision() == 500);
        Transaccion grande = s.transferir(a, principal(bruno), 20_000, Categoria.OTROS, "x");
        // antes de esta tiene 400 + 300 = 700 pts (Plata): 1% de 20.000 = 200 -> minimo 500 -> -10% = 450.
        // Los 600 pts de esta transferencia se suman despues de cobrarla.
        verificar("comision: minimo con descuento Plata = 450", grande.getComision() == 450);
        verificar("comision: se descuenta del origen", s.consultarSaldo(a) == 40_000 - 10_500 - 20_450);
        verificar("comision: el destino recibe el monto completo", s.consultarSaldo(principal(bruno)) == 30_000);
    }

    private static void comisionDescuentoPorRango() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        String a = principal(ana);
        s.depositar(a, 600_000, "x"); // 6000 pts: Platino
        long comision = s.cotizarComision(TipoTransaccion.TRANSFERENCIA, 200_000, a);
        verificar("comision: Platino 30% de 2.000 = 1.400", comision == 1_400);
        verificar("comision: interno gratis", s.cotizarComision(TipoTransaccion.MOVIMIENTO_INTERNO, 200_000, a) == 0);
        verificar("comision: deposito gratis", s.transferir(a, principal(bruno), 200_000, Categoria.OTROS, "x")
                .getComision() == 1_400);
    }

    private static void comisionRetiro() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        s.depositar(m, 10_000, "x");
        Transaccion t = s.retirar(m, 3_000, Categoria.OTROS, "x");
        verificar("comision: retiro $2.000", t.getComision() == 2_000 && s.consultarSaldo(m) == 5_000);
        lanza("comision: saldo debe cubrir monto + comision", monedero.excepciones.SaldoInsuficienteException.class,
                () -> s.retirar(m, 4_000, Categoria.OTROS, "x"));
    }

    private static void comisionSeDevuelveAlRevertir() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        s.depositar(m, 10_000, "x");
        s.retirar(m, 3_000, Categoria.OTROS, "x");
        s.revertirUltima(ana.getId());
        verificar("comision: revertir devuelve monto y comision", s.consultarSaldo(m) == 10_000);
    }

    // ------------------------------------------------------------------ canje

    private static void canjeGastaDisponiblesNoRango() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        s.depositar(principal(ana), 60_000, "x"); // 600 pts: Plata
        s.canjear(ana.getId(), Beneficio.RETIROS_SIN_CARGO);
        verificar("canje: descuenta disponibles", ana.getPuntosDisponibles() == 100 && ana.getPuntosCanjeados() == 500);
        verificar("canje: acumulados y rango no cambian", ana.getPuntos() == 600 && ana.getRango() == Rango.PLATA);
    }

    private static void canjeSinPuntos() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        s.depositar(principal(ana), 9_900, "x"); // 99 pts
        lanza("canje: sin puntos suficientes lanza", OperacionInvalidaException.class,
                () -> s.canjear(ana.getId(), Beneficio.DESCUENTO_TRANSFERENCIAS));
        verificar("canje: no cambio nada", ana.getPuntosDisponibles() == 99 && ana.getPuntosCanjeados() == 0);
    }

    private static void canjeDescuentoTransferencias() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        String a = principal(ana);
        s.depositar(a, 300_000, "x"); // 3000 pts: Oro (20%)
        long antes = s.cotizarComision(TipoTransaccion.TRANSFERENCIA, 100_000, a);
        s.canjear(ana.getId(), Beneficio.DESCUENTO_TRANSFERENCIAS);
        long despues = s.cotizarComision(TipoTransaccion.TRANSFERENCIA, 100_000, a);
        verificar("canje 100: 1.000 -20% = 800", antes == 800);
        verificar("canje 100: 1.000 -30% = 700", despues == 700);
    }

    private static void canjeRetirosSinCargoVence() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        s.depositar(m, 50_000, "x");
        s.canjear(ana.getId(), Beneficio.RETIROS_SIN_CARGO);
        verificar("canje 500: retiro gratis", s.retirar(m, 1_000, Categoria.OTROS, "x").getComision() == 0);
        s.getReloj().avanzarDias(29);
        verificar("canje 500: dia 29 sigue gratis", s.cotizarComision(TipoTransaccion.RETIRO, 1_000, m) == 0);
        s.getReloj().avanzarDias(1);
        verificar("canje 500: dia 30 ya cobra", s.cotizarComision(TipoTransaccion.RETIRO, 1_000, m) == ComisionEstandar.TARIFA_RETIRO);
    }

    private static void canjeBono() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        s.depositar(m, 100_000, "x"); // 1000 pts
        s.canjear(ana.getId(), Beneficio.BONO_SALDO);
        verificar("canje 1000: bono en el principal", s.consultarSaldo(m) == 100_000 + Beneficio.MONTO_BONO);
        verificar("canje 1000: el bono no da puntos", ana.getPuntos() == 1000 && ana.getPuntosDisponibles() == 0);
        verificar("canje 1000: el bono no se puede revertir",
                s.proximaReversible(ana.getId()).getTipo() == TipoTransaccion.DEPOSITO);
    }

    private static void noRevertirSiPuntosCanjeados() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        s.depositar(principal(ana), 10_000, "x"); // 100 pts
        s.canjear(ana.getId(), Beneficio.DESCUENTO_TRANSFERENCIAS);
        lanza("revertir: puntos ya canjeados lanza", OperacionInvalidaException.class,
                () -> s.revertirUltima(ana.getId()));
    }

    private static void historialPuntosCuadra() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        s.depositar(m, 50_000, "x");
        s.canjear(ana.getId(), Beneficio.DESCUENTO_TRANSFERENCIAS);
        s.depositar(m, 10_000, "x");
        s.revertirUltima(ana.getId());
        StringBuilder tipos = new StringBuilder();
        int suma = 0;
        for (MovimientoPuntos mov : s.historialPuntos(ana.getId())) {
            tipos.append(mov.getTipo().name().charAt(0));
            suma += mov.getCantidad();
        }
        verificar("historial puntos: ganar, canjear, ganar, revertir", tipos.toString().equals("GCGR"));
        verificar("historial puntos: suma = disponibles", suma == ana.getPuntosDisponibles() && suma == 400);
    }

    // ------------------------------------------------------------------ arbol de clientes

    private static void rankingYRangos() {
        SistemaMonedero s = nuevo();
        DatosIniciales.cargar(s);
        verificar("ranking: primero el de mas puntos", s.ranking(1).obtener(0).getNombre().equals("Diego Mora"));
        verificar("ranking: top 2", s.ranking(2).obtener(1).getNombre().equals("Ana Gomez"));
        verificar("rango: dos Bronce", s.clientesDeRango(Rango.BRONCE).tamanio() == 2);
        verificar("rango: ningun Platino", s.clientesDeRango(Rango.PLATINO).estaVacia());
    }

    private static void arbolSigueAlCambiarPuntos() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        s.depositar(principal(bruno), 20_000, "x");
        s.depositar(principal(ana), 10_000, "x");
        verificar("arbol: Bruno primero", s.ranking(1).obtener(0).equals(bruno));
        s.depositar(principal(ana), 15_000, "x");
        verificar("arbol: Ana pasa a Bruno", s.ranking(1).obtener(0).equals(ana));
        s.revertirUltima(ana.getId());
        verificar("arbol: al revertir vuelve Bruno", s.ranking(1).obtener(0).equals(bruno));
        verificar("arbol: un nodo por cliente", s.getRegistroPuntos().tamanio() == 2);
    }

    private static void integridadConTodo() {
        SistemaMonedero s = nuevo();
        DatosIniciales.cargar(s);
        String diego = "C004";
        s.canjear(diego, Beneficio.BONO_SALDO);
        s.canjear("C001", Beneficio.DESCUENTO_TRANSFERENCIAS);
        s.revertirUltima("C003");
        var r = new VerificadorIntegridad(s).verificar();
        verificar("integridad: OK con comisiones, canjes y reversion", r.esValido());
        if (!r.esValido()) {
            System.out.println(r);
        }
    }

    // ------------------------------------------------------------------ apoyo

    private static SistemaMonedero nuevo() {
        return new SistemaMonedero(new Reloj(INICIO), new ComisionEstandar());
    }

    private static String principal(Cliente cliente) {
        return cliente.getMonederos().iterator().next().getId();
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
