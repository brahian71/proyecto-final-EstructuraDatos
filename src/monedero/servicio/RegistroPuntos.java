package monedero.servicio;

import monedero.estructuras.ArbolAVL;
import monedero.estructuras.Coleccion;
import monedero.modelo.Beneficio;
import monedero.modelo.Cliente;
import monedero.modelo.Rango;

import java.time.LocalDateTime;

/**
 * Mantiene a todos los clientes en un ARBOL AVL ordenado por puntos
 * acumulados. Cada vez que los puntos de un cliente cambian, se saca del
 * arbol con su clave vieja y se vuelve a insertar con la nueva: O(log n).
 *
 * Con el arbol se responde, sin recorrer a todos los clientes:
 *   clientesDeRango(ORO)  -> rango [1001, 5000]          O(log n + k)
 *   ranking(10)           -> inorden invertido cortado    O(log n + 10)
 *
 * Por que AVL y no un BST simple: los clientes nuevos entran con 0 puntos y
 * los puntos crecen con el uso; un BST simple con claves que llegan casi en
 * orden se degenera en una lista (O(n) por operacion). El AVL rota para
 * mantener la altura en O(log n).
 */
public class RegistroPuntos {

    private final ArbolAVL<ClavePuntos, Cliente> arbol = new ArbolAVL<>();

    public void agregar(Cliente cliente) {
        arbol.insertar(clave(cliente), cliente);
    }

    public void ganar(Cliente cliente, int cantidad, LocalDateTime fecha, String detalle) {
        if (cantidad == 0) {
            return;
        }
        arbol.eliminar(clave(cliente));
        cliente.ganarPuntos(cantidad, fecha, detalle);
        arbol.insertar(clave(cliente), cliente);
    }

    public void perder(Cliente cliente, int cantidad, LocalDateTime fecha, String detalle) {
        if (cantidad == 0) {
            return;
        }
        arbol.eliminar(clave(cliente));
        try {
            cliente.perderPuntos(cantidad, fecha, detalle);
        } finally {
            // si perderPuntos falla, el cliente vuelve al arbol con sus puntos de siempre
            arbol.insertar(clave(cliente), cliente);
        }
    }

    /** Canjear no cambia los puntos acumulados, asi que la posicion en el arbol no cambia. */
    public void canjear(Cliente cliente, Beneficio beneficio, LocalDateTime fecha) {
        cliente.canjear(beneficio, fecha);
    }

    public Coleccion<Cliente> clientesDeRango(Rango rango) {
        return clientesEntre(rango.getPuntosMinimos(), rango.getPuntosMaximos());
    }

    public Coleccion<Cliente> clientesEntre(int minimo, int maximo) {
        return arbol.rango(ClavePuntos.desde(minimo), ClavePuntos.hasta(maximo));
    }

    /** Los n clientes con mas puntos, de mayor a menor. */
    public Coleccion<Cliente> ranking(int n) {
        return arbol.mayores(n);
    }

    public boolean contiene(Cliente cliente) {
        return arbol.buscar(clave(cliente)) == cliente;
    }

    public int tamanio() {
        return arbol.tamanio();
    }

    public int altura() {
        return arbol.altura();
    }

    public int rotaciones() {
        return arbol.rotaciones();
    }

    /** Orden, alturas y balance del AVL (lo usa la integridad). */
    public boolean arbolValido() {
        return arbol.esValido();
    }

    private static ClavePuntos clave(Cliente cliente) {
        return new ClavePuntos(cliente.getPuntos(), cliente.getId());
    }
}
