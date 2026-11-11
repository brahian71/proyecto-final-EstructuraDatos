package monedero.servicio;

import monedero.estructuras.ListaCircular;
import monedero.estructuras.ListaEnlazada;
import monedero.estructuras.Mapa;
import monedero.estructuras.TablaHash;
import monedero.modelo.Notificacion;
import monedero.modelo.TipoNotificacion;

import java.time.LocalDateTime;

public class Notificador {

    public static final int CAPACIDAD = 10;

    private final Mapa<String, ListaCircular<Notificacion>> buzones = new TablaHash<>();

    public void notificar(String clienteId, TipoNotificacion tipo, String mensaje, LocalDateTime fecha) {
        ListaCircular<Notificacion> buzon = buzones.obtener(clienteId);
        if (buzon == null) {
            buzon = new ListaCircular<>(CAPACIDAD);
            buzones.poner(clienteId, buzon);
        }
        buzon.agregar(new Notificacion(tipo, mensaje, fecha));
    }

    public Iterable<Notificacion> buzon(String clienteId) {
        ListaCircular<Notificacion> buzon = buzones.obtener(clienteId);
        return buzon == null ? new ListaEnlazada<>() : buzon;
    }

    public int noLeidas(String clienteId) {
        int cuenta = 0;
        for (Notificacion n : buzon(clienteId)) {
            if (!n.isLeida()) {
                cuenta++;
            }
        }
        return cuenta;
    }

    public void marcarLeidas(String clienteId) {
        for (Notificacion n : buzon(clienteId)) {
            n.marcarLeida();
        }
    }
}
