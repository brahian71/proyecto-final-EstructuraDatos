package monedero;

import monedero.datos.DatosIniciales;
import monedero.excepciones.NoEncontradoException;
import monedero.excepciones.OperacionInvalidaException;
import monedero.excepciones.SaldoInsuficienteException;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Corruptor;
import monedero.modelo.Monedero;
import monedero.modelo.Rango;
import monedero.modelo.TipoMonedero;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Transaccion;
import monedero.servicio.CalculadoraPuntos;
import monedero.servicio.PoliticaComision;
import monedero.servicio.Reloj;
import monedero.servicio.ResultadoIntegridad;
import monedero.servicio.SistemaMonedero;
import monedero.servicio.VerificadorIntegridad;

import java.time.LocalDateTime;

public class PruebasFase1 {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 1, 8, 0);

    private static int exitos = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        puntosPorTipo();
        rangosEnLosLimites();
        depositoYSaldo();
        retiroSinSaldo();
        montoInvalido();
        montoMaximo();
        nombreVacioNoGastaId();
        transferenciaEntreClientes();
        movimientoInternoSinPuntos();
        monederoInexistente();
        historialEnOrden();
        integridadDatosIniciales();
        integridadDetectaSaldoAlterado();
        integridadDetectaSaldoNegativo();
        integridadHistorialGrande();

        System.out.println("\nResultado: " + exitos + " OK, " + fallos + " FALLO");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    private static void puntosPorTipo() {
        CalculadoraPuntos uno = CalculadoraPuntos.UNIDAD_UN_PESO;
        verificar("puntos: deposito 1 x 100", uno.calcular(TipoTransaccion.DEPOSITO, 1000) == 10);
        verificar("puntos: retiro 2 x 100", uno.calcular(TipoTransaccion.RETIRO, 1000) == 20);
        verificar("puntos: transferencia 3 x 100", uno.calcular(TipoTransaccion.TRANSFERENCIA, 1000) == 30);
        verificar("puntos: solo centenas completas", uno.calcular(TipoTransaccion.DEPOSITO, 299) == 2);
        verificar("puntos: menos de 100 da 0", uno.calcular(TipoTransaccion.TRANSFERENCIA, 99) == 0);
        verificar("puntos: movimiento interno 0", uno.calcular(TipoTransaccion.MOVIMIENTO_INTERNO, 5000) == 0);

        CalculadoraPuntos cop = CalculadoraPuntos.PESOS_COLOMBIANOS;
        verificar("cop: salario $2.000.000 = 200 pts", cop.calcular(TipoTransaccion.DEPOSITO, 2_000_000) == 200);
        verificar("cop: retiro $50.000 = 10 pts", cop.calcular(TipoTransaccion.RETIRO, 50_000) == 10);
        verificar("cop: transferencia $25.000 = 6 pts", cop.calcular(TipoTransaccion.TRANSFERENCIA, 25_000) == 6);
        verificar("cop: menos de $10.000 da 0", cop.calcular(TipoTransaccion.DEPOSITO, 9_999) == 0);
        verificar("cop: 50 unidades = $5.000", cop.pesos(50) == 5_000);
        lanza("escala: unidad 0 lanza", IllegalArgumentException.class, () -> new CalculadoraPuntos(0));
    }

    private static void rangosEnLosLimites() {
        verificar("rango: 0 bronce", Rango.desdePuntos(0) == Rango.BRONCE);
        verificar("rango: 500 bronce", Rango.desdePuntos(500) == Rango.BRONCE);
        verificar("rango: 501 plata", Rango.desdePuntos(501) == Rango.PLATA);
        verificar("rango: 1000 plata", Rango.desdePuntos(1000) == Rango.PLATA);
        verificar("rango: 1001 oro", Rango.desdePuntos(1001) == Rango.ORO);
        verificar("rango: 5000 oro", Rango.desdePuntos(5000) == Rango.ORO);
        verificar("rango: 5001 platino", Rango.desdePuntos(5001) == Rango.PLATINO);
        lanza("rango: negativo lanza", IllegalArgumentException.class, () -> Rango.desdePuntos(-1));
    }

    private static void depositoYSaldo() {
        SistemaMonedero s = nuevo();
        Cliente c = s.registrarCliente("Ana");
        String m = principal(c);
        s.depositar(m, 1050, "x");
        verificar("deposito: saldo", s.consultarSaldo(m) == 1050);
        verificar("deposito: puntos 10", c.getPuntos() == 10);
        verificar("deposito: queda en historial", s.historial(m).iterator().next().getTipo() == TipoTransaccion.DEPOSITO);
    }

    private static void retiroSinSaldo() {
        SistemaMonedero s = nuevo();
        Cliente c = s.registrarCliente("Ana");
        String m = principal(c);
        s.depositar(m, 500, "x");
        lanza("retiro: sin saldo lanza", SaldoInsuficienteException.class,
                () -> s.retirar(m, 501, Categoria.OTROS, "x"));
        verificar("retiro: saldo intacto", s.consultarSaldo(m) == 500);
        verificar("retiro: no se registro", s.cantidadTransacciones() == 1);
        s.retirar(m, 500, Categoria.OTROS, "todo");
        verificar("retiro: puede dejar en 0", s.consultarSaldo(m) == 0);
        verificar("retiro: puntos 5 + 10", c.getPuntos() == 15);
    }

    private static void montoInvalido() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        lanza("monto: cero lanza", OperacionInvalidaException.class, () -> s.depositar(m, 0, "x"));
        lanza("monto: negativo lanza", OperacionInvalidaException.class, () -> s.depositar(m, -10, "x"));
    }

    private static void montoMaximo() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        s.depositar(m, SistemaMonedero.MONTO_MAXIMO, "x");
        verificar("monto: el maximo se acepta", s.consultarSaldo(m) == SistemaMonedero.MONTO_MAXIMO);
        lanza("monto: mas del maximo lanza", OperacionInvalidaException.class,
                () -> s.depositar(m, SistemaMonedero.MONTO_MAXIMO + 1, "x"));
        lanza("monto: retiro mayor al maximo lanza", OperacionInvalidaException.class,
                () -> s.retirar(m, 20_000_000_000_000L, Categoria.OTROS, "x"));
        verificar("monto: nada cambio tras rechazar", s.cantidadTransacciones() == 1);
    }

    private static void nombreVacioNoGastaId() {
        SistemaMonedero s = nuevo();
        lanza("cliente: nombre vacio lanza", IllegalArgumentException.class, () -> s.registrarCliente("   "));
        Cliente ana = s.registrarCliente("Ana");
        verificar("cliente: el id no se salta", ana.getId().equals("C001") && principal(ana).equals("M001"));
        lanza("monedero: nombre vacio lanza", IllegalArgumentException.class,
                () -> s.crearMonedero(ana.getId(), "", TipoMonedero.AHORROS));
        verificar("monedero: el id no se salta", s.crearMonedero(ana.getId(), "Ahorros", TipoMonedero.AHORROS)
                .getId().equals("M002"));
    }

    private static void transferenciaEntreClientes() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        String a = principal(ana);
        String b = principal(bruno);
        s.depositar(a, 1000, "x");
        Transaccion t = s.transferir(a, b, 400, Categoria.OTROS, "x");
        verificar("transferencia: tipo", t.getTipo() == TipoTransaccion.TRANSFERENCIA);
        verificar("transferencia: origen baja", s.consultarSaldo(a) == 600);
        verificar("transferencia: destino sube", s.consultarSaldo(b) == 400);
        verificar("transferencia: puntos al que envia (10 + 12)", ana.getPuntos() == 22);
        verificar("transferencia: el que recibe no gana", bruno.getPuntos() == 0);
        verificar("transferencia: en ambos historiales",
                s.historial(b).iterator().next().equals(t) && s.buscarMonedero(a).cantidadTransacciones() == 2);
        lanza("transferencia: al mismo monedero lanza", OperacionInvalidaException.class,
                () -> s.transferir(a, a, 10, Categoria.OTROS, "x"));
    }

    private static void movimientoInternoSinPuntos() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String a = principal(ana);
        Monedero ahorros = s.crearMonedero(ana.getId(), "Ahorros", TipoMonedero.AHORROS);
        s.depositar(a, 1000, "x");
        Transaccion t = s.transferir(a, ahorros.getId(), 700, Categoria.AHORRO, "x");
        verificar("interno: tipo", t.getTipo() == TipoTransaccion.MOVIMIENTO_INTERNO);
        verificar("interno: no suma puntos", ana.getPuntos() == 10);
        verificar("interno: saldo total igual", ana.saldoTotal() == 1000);
        verificar("interno: dos monederos", ana.cantidadMonederos() == 2);
    }

    private static void monederoInexistente() {
        SistemaMonedero s = nuevo();
        lanza("inexistente: depositar lanza", NoEncontradoException.class, () -> s.depositar("M999", 10, "x"));
        lanza("inexistente: cliente lanza", NoEncontradoException.class, () -> s.buscarCliente("C999"));
    }

    private static void historialEnOrden() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        s.depositar(m, 100, "1");
        s.getReloj().avanzarDias(1);
        s.depositar(m, 200, "2");
        s.retirar(m, 50, Categoria.OTROS, "3");
        StringBuilder orden = new StringBuilder();
        for (Transaccion t : s.historial(m)) {
            orden.append(t.getDescripcion());
        }
        verificar("historial: orden cronologico", orden.toString().equals("123"));
    }

    private static void integridadDatosIniciales() {
        SistemaMonedero s = nuevo();
        DatosIniciales.cargar(s);
        ResultadoIntegridad r = new VerificadorIntegridad(s).verificar();
        verificar("integridad: datos iniciales OK", r.esValido());
        if (!r.esValido()) {
            System.out.println(r);
        }
    }

    private static void integridadDetectaSaldoAlterado() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        s.depositar(m, 1000, "x");
        Transaccion falsa = new Transaccion("TX", TipoTransaccion.DEPOSITO, 500, 0, null, m,
                null, s.getReloj().ahora(), "falsa", 5);
        Corruptor.meterSinAfectarSaldo(s.buscarMonedero(m), falsa);
        ResultadoIntegridad r = new VerificadorIntegridad(s).verificar();
        verificar("integridad: detecta saldo y puntos", !r.esValido() && r.cantidadErrores() == 2);
    }

    private static void integridadDetectaSaldoNegativo() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        Monedero monedero = s.buscarMonedero(m);
        LocalDateTime f = s.getReloj().ahora();
        Corruptor.meterSinAfectarSaldo(monedero, new Transaccion("TA", TipoTransaccion.RETIRO, 300, 0, m, null, null, f, "", 0));
        Corruptor.meterSinAfectarSaldo(monedero, new Transaccion("TB", TipoTransaccion.DEPOSITO, 300, 0, null, m, null, f, "", 0));
        ResultadoIntegridad r = new VerificadorIntegridad(s).verificar();
        boolean menciona = false;
        for (String e : r.getErrores()) {
            menciona |= e.contains("-$300");
        }
        verificar("integridad: detecta saldo negativo intermedio", menciona);
    }

    private static void integridadHistorialGrande() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        for (int i = 0; i < 50_000; i++) {
            s.depositar(m, 100, "");
        }
        verificar("integridad: 50.000 transacciones sin desbordar pila",
                new VerificadorIntegridad(s).verificar().esValido());
    }

    private static SistemaMonedero nuevo() {
        return new SistemaMonedero(new Reloj(INICIO), PoliticaComision.SIN_COMISION, CalculadoraPuntos.UNIDAD_UN_PESO);
    }

    private static String principal(Cliente cliente) {
        return cliente.getMonederos().iterator().next().getId();
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
