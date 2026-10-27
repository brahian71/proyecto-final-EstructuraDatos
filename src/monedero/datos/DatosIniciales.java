package monedero.datos;

import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.TipoMonedero;
import monedero.servicio.SistemaMonedero;

/**
 * Datos de ejemplo para arrancar el sistema con informacion (no hay
 * persistencia). Pasan por el servicio, asi que saldos, puntos e historial
 * quedan consistentes y la verificacion de integridad da OK.
 *
 * Montos pequenos a proposito: con 1 punto por cada 100 unidades, montos en
 * pesos reales (500.000) dejarian a todos en Oro/Platino. Asi quedan
 * clientes en Bronce, Plata y Oro para mostrar los rangos.
 */
public final class DatosIniciales {

    private DatosIniciales() {
    }

    public static void cargar(SistemaMonedero sistema) {
        Cliente ana = sistema.registrarCliente("Ana Gomez");
        Cliente bruno = sistema.registrarCliente("Bruno Diaz");
        Cliente carla = sistema.registrarCliente("Carla Rios");
        Cliente diego = sistema.registrarCliente("Diego Mora");

        String anaPrincipal = principal(ana);
        String brunoPrincipal = principal(bruno);
        String carlaPrincipal = principal(carla);
        Monedero anaAhorros = sistema.crearMonedero(ana.getId(), "Ahorros", TipoMonedero.AHORROS);
        Monedero brunoGastos = sistema.crearMonedero(bruno.getId(), "Gastos diarios", TipoMonedero.GASTOS_DIARIOS);

        sistema.depositar(anaPrincipal, 50_000, "Salario");
        sistema.getReloj().avanzarDias(1);
        sistema.depositar(brunoPrincipal, 30_000, "Pago freelance");
        sistema.depositar(carlaPrincipal, 12_000, "Mesada");
        sistema.depositar(principal(diego), 120_000, "Venta de moto");
        sistema.getReloj().avanzarDias(1);

        sistema.transferir(anaPrincipal, anaAhorros.getId(), 10_000, Categoria.AHORRO, "Ahorro del mes");
        sistema.transferir(brunoPrincipal, brunoGastos.getId(), 8_000, Categoria.OTROS, "Plata de la semana");
        sistema.getReloj().avanzarDias(1);

        sistema.retirar(anaPrincipal, 4_500, Categoria.ALIMENTACION, "Mercado");
        sistema.retirar(brunoGastos.getId(), 1_200, Categoria.TRANSPORTE, "Recarga bus");
        sistema.transferir(anaPrincipal, carlaPrincipal, 3_000, Categoria.EDUCACION, "Libros");
        sistema.getReloj().avanzarDias(1);

        sistema.transferir(carlaPrincipal, brunoPrincipal, 1_500, Categoria.ENTRETENIMIENTO, "Cine");
        sistema.retirar(carlaPrincipal, 2_000, Categoria.SALUD, "Farmacia");
        sistema.transferir(brunoGastos.getId(), anaPrincipal, 2_500, Categoria.ALIMENTACION, "Almuerzo compartido");
    }

    /** El primer monedero de cada cliente es el principal (lo crea registrarCliente). */
    private static String principal(Cliente cliente) {
        return cliente.getMonederos().iterator().next().getId();
    }
}
