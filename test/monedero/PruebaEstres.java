package monedero;

import monedero.datos.DatosIniciales;
import monedero.excepciones.NoEncontradoException;
import monedero.excepciones.OperacionInvalidaException;
import monedero.excepciones.SaldoInsuficienteException;
import monedero.modelo.Beneficio;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Frecuencia;
import monedero.modelo.Monedero;
import monedero.modelo.OperacionProgramada;
import monedero.modelo.TipoMonedero;
import monedero.modelo.TipoTransaccion;
import monedero.servicio.AnalisisGastos;
import monedero.servicio.ResultadoIntegridad;
import monedero.servicio.SistemaMonedero;
import monedero.servicio.VerificadorIntegridad;

import java.util.Random;

/**
 * Prueba de estres: 20 escenarios (semillas 1..20) de 3.000 operaciones al
 * azar cada uno (60.000 en total). Cada 100 pasos corre VerificadorIntegridad.
 * Las reglas de negocio (saldo insuficiente, operacion invalida) son normales;
 * cualquier otra excepcion cuenta como inesperada.
 *
 * Resultado esperado: 0 fallos de integridad y 0 excepciones inesperadas.
 */
public class PruebaEstres {

    public static void main(String[] args) {
        int fallosIntegridad = 0;
        int inesperadas = 0;
        long exitosas = 0;
        for (int semilla = 1; semilla <= 20; semilla++) {
            Random r = new Random(semilla);
            SistemaMonedero s = new SistemaMonedero();
            DatosIniciales.cargar(s);
            for (int i = 0; i < 6; i++) {
                Cliente c = s.registrarCliente("Cliente" + i);
                if (r.nextBoolean()) {
                    s.crearMonedero(c.getId(), "Ahorros", TipoMonedero.AHORROS);
                }
            }
            for (int paso = 0; paso < 3000; paso++) {
                Monedero[] ms = monederos(s);
                Cliente[] cs = clientes(s);
                Monedero m = ms[r.nextInt(ms.length)];
                Monedero otro = ms[r.nextInt(ms.length)];
                Cliente c = cs[r.nextInt(cs.length)];
                Categoria cat = Categoria.values()[r.nextInt(Categoria.values().length)];
                long monto = 100 + r.nextInt(r.nextBoolean() ? 5_000 : 200_000);
                try {
                    switch (r.nextInt(12)) {
                        case 0, 1 -> s.depositar(m.getId(), monto, "d");
                        case 2 -> s.retirar(m.getId(), monto, cat, "r");
                        case 3, 4 -> s.transferir(m.getId(), otro.getId(), monto, cat, "t");
                        case 5 -> s.revertirUltima(c.getId());
                        case 6 -> s.canjear(c.getId(), Beneficio.values()[r.nextInt(3)]);
                        case 7 -> s.avanzarDias(r.nextInt(5));
                        case 8 -> s.getProgramador().programar(
                                new TipoTransaccion[]{TipoTransaccion.DEPOSITO, TipoTransaccion.RETIRO, TipoTransaccion.TRANSFERENCIA}[r.nextInt(3)],
                                m.getId(), otro.getId(), monto, cat, "p",
                                Frecuencia.values()[r.nextInt(Frecuencia.values().length)],
                                s.getReloj().ahora().plusDays(r.nextInt(10)),
                                r.nextBoolean() ? OperacionProgramada.SIN_LIMITE : 1 + r.nextInt(5));
                        case 9 -> {
                            var pend = s.getProgramador().pendientes();
                            if (!pend.estaVacia()) {
                                s.getProgramador().cancelar(pend.obtener(r.nextInt(pend.tamanio())).getId());
                            }
                        }
                        case 10 -> {
                            AnalisisGastos a = new AnalisisGastos(s);
                            a.ciclosTransferencias();
                            a.conexion(c.getId(), cs[r.nextInt(cs.length)].getId());
                            a.secuenciasFrecuentes(null, 5);
                            a.flujoMonederos(c.getId());
                            a.gastoPorCategoria(c.getId());
                            a.mayoresReceptores(5);
                        }
                        default -> {
                            s.ranking(5);
                            s.clientesDeRango(c.getRango());
                        }
                    }
                    exitosas++;
                } catch (SaldoInsuficienteException | OperacionInvalidaException e) {
                    // regla de negocio
                } catch (NoEncontradoException | IllegalArgumentException | IllegalStateException e) {
                    inesperadas++;
                    if (inesperadas <= 5) {
                        System.out.println("INESPERADA semilla " + semilla + " paso " + paso + ": " + e);
                    }
                }
                if (paso % 100 == 0) {
                    ResultadoIntegridad res = new VerificadorIntegridad(s).verificar();
                    if (!res.esValido()) {
                        fallosIntegridad++;
                        if (fallosIntegridad <= 3) {
                            System.out.println(res);
                        }
                    }
                }
            }
        }
        System.out.println("Operaciones exitosas: " + exitosas);
        System.out.println("Fallos de integridad: " + fallosIntegridad + ", excepciones inesperadas: " + inesperadas);
    }

    private static Monedero[] monederos(SistemaMonedero s) {
        int n = 0;
        for (Monedero ignorado : s.getMonederos()) {
            n++;
        }
        Monedero[] arr = new Monedero[n];
        int i = 0;
        for (Monedero m : s.getMonederos()) {
            arr[i++] = m;
        }
        return arr;
    }

    private static Cliente[] clientes(SistemaMonedero s) {
        int n = 0;
        for (Cliente ignorado : s.getClientes()) {
            n++;
        }
        Cliente[] arr = new Cliente[n];
        int i = 0;
        for (Cliente c : s.getClientes()) {
            arr[i++] = c;
        }
        return arr;
    }
}
