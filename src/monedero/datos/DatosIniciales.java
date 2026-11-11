package monedero.datos;

import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Frecuencia;
import monedero.modelo.Monedero;
import monedero.modelo.OperacionProgramada;
import monedero.modelo.Rol;
import monedero.modelo.TipoMonedero;
import monedero.modelo.TipoTransaccion;
import monedero.servicio.SistemaMonedero;

/**
 * Datos de ejemplo para arrancar el sistema con informacion (no hay
 * persistencia). Pasan por el servicio, asi que saldos, puntos e historial
 * quedan consistentes y la verificacion de integridad da OK.
 *
 * Montos en pesos colombianos reales. Con la escala de puntos en pesos
 * (unidad = $100, puntos por cada $10.000) quedan:
 *   Diego Oro (1.200 pts), Ana Plata (559, con 3 de bono por rango), Bruno Bronce (322),
 *   Carla Bronce (152).
 */
public final class DatosIniciales {

    public static final String USUARIO_ADMIN = "admin";
    public static final String CONTRASENA_ADMIN = "admin123";

    private DatosIniciales() {
    }

    public static void cargar(SistemaMonedero sistema) {
        // usuarios de demostracion (ver README): admin / admin123 y cada cliente con su nombre + 123
        sistema.getUsuarios().registrar(USUARIO_ADMIN, CONTRASENA_ADMIN, Rol.ADMINISTRADOR, null);
        Cliente ana = sistema.registrarClienteConUsuario("Ana Gomez", "ana", "ana123");
        Cliente bruno = sistema.registrarClienteConUsuario("Bruno Diaz", "bruno", "bruno123");
        Cliente carla = sistema.registrarClienteConUsuario("Carla Rios", "carla", "carla123");
        Cliente diego = sistema.registrarClienteConUsuario("Diego Mora", "diego", "diego123");

        String anaPrincipal = principal(ana);
        String brunoPrincipal = principal(bruno);
        String carlaPrincipal = principal(carla);
        Monedero anaAhorros = sistema.crearMonedero(ana.getId(), "Ahorros", TipoMonedero.AHORROS);
        Monedero brunoGastos = sistema.crearMonedero(bruno.getId(), "Gastos diarios", TipoMonedero.GASTOS_DIARIOS);

        sistema.depositar(anaPrincipal, 4_500_000, "Salario");
        sistema.getReloj().avanzarDias(1);
        sistema.depositar(brunoPrincipal, 2_800_000, "Pago freelance");
        sistema.depositar(carlaPrincipal, 1_200_000, "Mesada");
        sistema.depositar(principal(diego), 12_000_000, "Venta de moto");
        sistema.getReloj().avanzarDias(1);

        sistema.transferir(anaPrincipal, anaAhorros.getId(), 1_000_000, Categoria.AHORRO, "Ahorro del mes");
        sistema.transferir(brunoPrincipal, brunoGastos.getId(), 600_000, Categoria.OTROS, "Plata de la semana");
        sistema.getReloj().avanzarDias(1);

        sistema.retirar(anaPrincipal, 350_000, Categoria.ALIMENTACION, "Mercado");
        sistema.retirar(brunoGastos.getId(), 90_000, Categoria.TRANSPORTE, "Recarga tarjeta de bus");
        sistema.transferir(anaPrincipal, carlaPrincipal, 120_000, Categoria.EDUCACION, "Libros");
        sistema.getReloj().avanzarDias(1);

        sistema.transferir(carlaPrincipal, brunoPrincipal, 60_000, Categoria.ENTRETENIMIENTO, "Cine");
        sistema.retirar(carlaPrincipal, 75_000, Categoria.SALUD, "Farmacia");
        sistema.transferir(brunoGastos.getId(), anaPrincipal, 85_000, Categoria.ALIMENTACION, "Almuerzo compartido");

        // programadas: se ejecutan al avanzar el reloj desde el menu
        var programador = sistema.getProgramador();
        var hoy = sistema.getReloj().ahora();
        programador.programar(TipoTransaccion.TRANSFERENCIA, anaPrincipal, anaAhorros.getId(), 300_000,
                Categoria.AHORRO, "Ahorro mensual", Frecuencia.MENSUAL, hoy.plusDays(1), OperacionProgramada.SIN_LIMITE);
        programador.programar(TipoTransaccion.DEPOSITO, null, principal(diego), 1_500_000, null,
                "Arriendo recibido", Frecuencia.MENSUAL, hoy.plusDays(5), 6);
        programador.programar(TipoTransaccion.TRANSFERENCIA, brunoPrincipal, carlaPrincipal, 80_000,
                Categoria.EDUCACION, "Clases de guitarra", Frecuencia.SEMANAL, hoy.plusDays(2), 4);
    }

    /** El primer monedero de cada cliente es el principal (lo crea registrarCliente). */
    private static String principal(Cliente cliente) {
        return cliente.getMonederos().iterator().next().getId();
    }
}
