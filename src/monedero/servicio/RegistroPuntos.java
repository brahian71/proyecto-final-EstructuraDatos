package monedero.servicio;

import monedero.estructuras.ArbolAVL;
import monedero.estructuras.Coleccion;
import monedero.modelo.Beneficio;
import monedero.modelo.Cliente;
import monedero.modelo.Rango;

import java.time.LocalDateTime;

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
        try {
            cliente.ganarPuntos(cantidad, fecha, detalle);
        } finally {
            arbol.insertar(clave(cliente), cliente);
        }
    }

    public void perder(Cliente cliente, int cantidad, LocalDateTime fecha, String detalle) {
        if (cantidad == 0) {
            return;
        }
        arbol.eliminar(clave(cliente));
        try {
            cliente.perderPuntos(cantidad, fecha, detalle);
        } finally {
            arbol.insertar(clave(cliente), cliente);
        }
    }

    public void canjear(Cliente cliente, Beneficio beneficio, LocalDateTime fecha) {
        cliente.canjear(beneficio, fecha);
    }

    public Coleccion<Cliente> clientesDeRango(Rango rango) {
        return clientesEntre(rango.getPuntosMinimos(), rango.getPuntosMaximos());
    }

    public Coleccion<Cliente> clientesEntre(int minimo, int maximo) {
        return arbol.rango(ClavePuntos.desde(minimo), ClavePuntos.hasta(maximo));
    }

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

    public boolean arbolValido() {
        return arbol.esValido();
    }

    private static ClavePuntos clave(Cliente cliente) {
        return new ClavePuntos(cliente.getPuntos(), cliente.getId());
    }
}
