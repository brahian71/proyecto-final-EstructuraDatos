package monedero;

import monedero.datos.DatosIniciales;
import monedero.estructuras.MonticuloBinario;
import monedero.excepciones.OperacionInvalidaException;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Frecuencia;
import monedero.modelo.Notificacion;
import monedero.modelo.OperacionProgramada;
import monedero.modelo.TipoNotificacion;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Transaccion;
import monedero.servicio.PoliticaComision;
import monedero.servicio.ProgramadorTransacciones;
import monedero.servicio.Reloj;
import monedero.servicio.SistemaMonedero;
import monedero.servicio.VerificadorIntegridad;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Pruebas de la fase 4: monticulo binario y transacciones programadas.
 * Sin JUnit, cada prueba imprime OK o FALLO.
 */
public class PruebasFase4 {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 1, 8, 0);

    private static int exitos = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        monticuloOrdena();
        monticuloRepetidosYCrecer();
        monticuloVacio();
        monticuloEliminarCualquiera();
        monticuloOrdenadosNoModifica();

        frecuenciaMensualFinDeMes();
        unaVezSeEjecutaEnSuFecha();
        recurrenteSeEjecutaVariasVeces();
        transaccionQuedaConLaFechaProgramada();
        ordenEntreVariasProgramadas();
        mismoMomentoSaleLaMasAntigua();
        falloPorSaldoAvisaYSigue();
        cancelar();
        recordatorioUnDiaAntes();
        noProgramarEnElPasado();
        integridadDespuesDeProgramadas();

        System.out.println("\nResultado: " + exitos + " OK, " + fallos + " FALLO");
        if (fallos > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ monticulo

    private static void monticuloOrdena() {
        MonticuloBinario<Integer> m = new MonticuloBinario<>(Comparator.naturalOrder());
        for (int v : new int[]{5, 3, 8, 1, 9, 2, 7}) {
            m.insertar(v);
        }
        verificar("monticulo: el minimo arriba", m.verPrimero() == 1 && m.tamanio() == 7);
        StringBuilder sb = new StringBuilder();
        while (!m.estaVacia()) {
            sb.append(m.extraer());
        }
        verificar("monticulo: extrae en orden", sb.toString().equals("1235789"));
    }

    private static void monticuloRepetidosYCrecer() {
        MonticuloBinario<Integer> m = new MonticuloBinario<>(Comparator.naturalOrder());
        for (int i = 0; i < 2000; i++) {
            m.insertar((i * 7919) % 500); // valores repetidos y desordenados
        }
        verificar("monticulo: valido con 2000", m.esValido() && m.tamanio() == 2000);
        int anterior = Integer.MIN_VALUE;
        boolean ordenado = true;
        while (!m.estaVacia()) {
            int v = m.extraer();
            ordenado &= v >= anterior;
            anterior = v;
        }
        verificar("monticulo: 2000 salen en orden", ordenado);
    }

    private static void monticuloVacio() {
        MonticuloBinario<Integer> m = new MonticuloBinario<>(Comparator.naturalOrder());
        lanza("monticulo vacio: extraer lanza", NoSuchElementException.class, m::extraer);
        lanza("monticulo vacio: verPrimero lanza", NoSuchElementException.class, m::verPrimero);
        verificar("monticulo vacio: valido", m.esValido());
    }

    private static void monticuloEliminarCualquiera() {
        MonticuloBinario<Integer> m = new MonticuloBinario<>(Comparator.naturalOrder());
        for (int v = 1; v <= 15; v++) {
            m.insertar(v);
        }
        verificar("monticulo: eliminar del medio", m.eliminar(6) && !m.eliminar(99) && m.tamanio() == 14);
        verificar("monticulo: sigue valido", m.esValido());
        StringBuilder sb = new StringBuilder();
        for (Integer v : m.ordenados()) {
            sb.append(v).append(' ');
        }
        verificar("monticulo: sin el 6", sb.toString().equals("1 2 3 4 5 7 8 9 10 11 12 13 14 15 "));
    }

    private static void monticuloOrdenadosNoModifica() {
        MonticuloBinario<Integer> m = new MonticuloBinario<>(Comparator.naturalOrder());
        m.insertar(3);
        m.insertar(1);
        m.insertar(2);
        m.ordenados();
        verificar("monticulo: ordenados no saca nada", m.tamanio() == 3 && m.verPrimero() == 1);
    }

    // ------------------------------------------------------------------ programadas

    private static void frecuenciaMensualFinDeMes() {
        LocalDateTime enero31 = LocalDateTime.of(2027, 1, 31, 9, 0);
        verificar("mensual: 31 ene -> 28 feb", Frecuencia.MENSUAL.fechaNumero(enero31, 1).getDayOfMonth() == 28);
        verificar("mensual: vuelve al 31 en marzo", Frecuencia.MENSUAL.fechaNumero(enero31, 2).getDayOfMonth() == 31);
        verificar("quincenal: +15 dias", Frecuencia.QUINCENAL.fechaNumero(enero31, 2).equals(enero31.plusDays(30)));
    }

    private static void unaVezSeEjecutaEnSuFecha() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 1_000, null, "x", Frecuencia.UNA_VEZ,
                INICIO.plusDays(3), 99);
        s.avanzarDias(2);
        verificar("una vez: no antes de su fecha", s.consultarSaldo(m) == 0);
        int procesadas = s.avanzarDias(1);
        verificar("una vez: se ejecuta el dia 3", s.consultarSaldo(m) == 1_000 && procesadas == 1);
        s.avanzarDias(30);
        verificar("una vez: no se repite", s.consultarSaldo(m) == 1_000 && prog(s).cantidadPendientes() == 0);
    }

    private static void recurrenteSeEjecutaVariasVeces() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        OperacionProgramada op = prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 1_000, null, "x",
                Frecuencia.SEMANAL, INICIO, 3);
        s.avanzarDias(60);
        verificar("recurrente: 3 ejecuciones y termina", s.consultarSaldo(m) == 3_000
                && op.terminada() && prog(s).cantidadPendientes() == 0);
    }

    private static void transaccionQuedaConLaFechaProgramada() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 1_000, null, "x", Frecuencia.DIARIA,
                INICIO.plusDays(1), 3);
        s.avanzarDias(10);
        StringBuilder dias = new StringBuilder();
        for (Transaccion t : s.historial(m)) {
            dias.append(t.getFecha().getDayOfMonth()).append(' ');
        }
        verificar("fechas: cada transaccion en su dia (2, 3, 4)", dias.toString().equals("2 3 4 "));
        verificar("fechas: reloj al final del salto", s.getReloj().ahora().equals(INICIO.plusDays(10)));
    }

    private static void ordenEntreVariasProgramadas() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 300, null, "C", Frecuencia.UNA_VEZ, INICIO.plusDays(9), 1);
        prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 100, null, "A", Frecuencia.UNA_VEZ, INICIO.plusDays(2), 1);
        prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 200, null, "B", Frecuencia.UNA_VEZ, INICIO.plusDays(5), 1);
        verificar("cola: la mas proxima primero", prog(s).pendientes().obtener(0).getDescripcion().equals("A"));
        s.avanzarDias(15);
        verificar("cola: se ejecutan por fecha, no por orden de creacion", descripciones(s, m).equals("ABC"));
    }

    private static void mismoMomentoSaleLaMasAntigua() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 100, null, "1", Frecuencia.UNA_VEZ, INICIO.plusDays(1), 1);
        prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 100, null, "2", Frecuencia.UNA_VEZ, INICIO.plusDays(1), 1);
        s.avanzarDias(1);
        verificar("empate: en orden de programacion", descripciones(s, m).equals("12"));
    }

    private static void falloPorSaldoAvisaYSigue() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        Cliente bruno = s.registrarCliente("Bruno");
        String a = principal(ana);
        s.depositar(a, 1_500, "x");
        OperacionProgramada op = prog(s).programar(TipoTransaccion.TRANSFERENCIA, a, principal(bruno), 1_000,
                Categoria.OTROS, "x", Frecuencia.SEMANAL, INICIO.plusDays(1), 3);
        s.avanzarDias(30);
        verificar("fallo: solo la primera alcanzo", s.consultarSaldo(principal(bruno)) == 1_000);
        verificar("fallo: cuenta 2 fallos y termina", op.getFallos() == 2 && op.terminada());
        boolean avisado = false;
        for (Notificacion n : s.getNotificador().buzon(ana.getId())) {
            avisado |= n.getMensaje().startsWith("No se pudo ejecutar");
        }
        verificar("fallo: se le avisa al cliente", avisado);
    }

    private static void cancelar() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        OperacionProgramada op = prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 1_000, null, "x",
                Frecuencia.DIARIA, INICIO.plusDays(1), OperacionProgramada.SIN_LIMITE);
        s.avanzarDias(2);
        prog(s).cancelar(op.getId());
        s.avanzarDias(10);
        verificar("cancelar: no se ejecuta mas", s.consultarSaldo(m) == 2_000 && prog(s).cantidadPendientes() == 0);
        lanza("cancelar: dos veces lanza", OperacionInvalidaException.class, () -> prog(s).cancelar(op.getId()));
    }

    private static void recordatorioUnDiaAntes() {
        SistemaMonedero s = nuevo();
        Cliente ana = s.registrarCliente("Ana");
        String m = principal(ana);
        prog(s).programar(TipoTransaccion.DEPOSITO, null, m, 1_000, null, "x", Frecuencia.MENSUAL,
                INICIO.plusDays(10), 3);
        s.avanzarDias(8);
        verificar("recordatorio: no antes de tiempo", contar(s, ana, TipoNotificacion.RECORDATORIO) == 0);
        s.avanzarDias(1);
        verificar("recordatorio: el dia anterior", contar(s, ana, TipoNotificacion.RECORDATORIO) == 1);
        s.avanzarDias(1);
        verificar("recordatorio: no se repite para la misma fecha", contar(s, ana, TipoNotificacion.RECORDATORIO) == 1);
        s.avanzarDias(70);
        // dos ejecuciones mas dentro del salto: cada una con su recordatorio
        verificar("recordatorio: tambien dentro de un salto largo", contar(s, ana, TipoNotificacion.RECORDATORIO) == 3);
    }

    private static void noProgramarEnElPasado() {
        SistemaMonedero s = nuevo();
        String m = principal(s.registrarCliente("Ana"));
        lanza("pasado: lanza", OperacionInvalidaException.class, () -> prog(s).programar(TipoTransaccion.DEPOSITO,
                null, m, 100, null, "x", Frecuencia.UNA_VEZ, INICIO.minusDays(1), 1));
        lanza("monto cero: lanza", OperacionInvalidaException.class, () -> prog(s).programar(TipoTransaccion.DEPOSITO,
                null, m, 0, null, "x", Frecuencia.UNA_VEZ, INICIO, 1));
    }

    private static void integridadDespuesDeProgramadas() {
        SistemaMonedero s = new SistemaMonedero(new Reloj(INICIO));
        DatosIniciales.cargar(s);
        s.avanzarDias(200);
        var r = new VerificadorIntegridad(s).verificar();
        verificar("integridad: OK tras 200 dias de programadas", r.esValido());
        if (!r.esValido()) {
            System.out.println(r);
        }
    }

    // ------------------------------------------------------------------ apoyo

    private static SistemaMonedero nuevo() {
        return new SistemaMonedero(new Reloj(INICIO), PoliticaComision.SIN_COMISION);
    }

    private static ProgramadorTransacciones prog(SistemaMonedero s) {
        return s.getProgramador();
    }

    private static String principal(Cliente cliente) {
        return cliente.getMonederos().iterator().next().getId();
    }

    /** Ultima letra de la descripcion de cada transaccion (las programadas llevan "Programada P00x: ..."). */
    private static String descripciones(SistemaMonedero s, String monedero) {
        StringBuilder sb = new StringBuilder();
        for (Transaccion t : s.historial(monedero)) {
            String d = t.getDescripcion();
            sb.append(d.charAt(d.length() - 1));
        }
        return sb.toString();
    }

    private static int contar(SistemaMonedero s, Cliente cliente, TipoNotificacion tipo) {
        int n = 0;
        for (Notificacion notificacion : s.getNotificador().buzon(cliente.getId())) {
            if (notificacion.getTipo() == tipo) {
                n++;
            }
        }
        return n;
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
