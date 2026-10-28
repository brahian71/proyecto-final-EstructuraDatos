package monedero.servicio;

import monedero.estructuras.ListaCircular;
import monedero.estructuras.ListaEnlazada;
import monedero.estructuras.Mapa;
import monedero.estructuras.TablaHash;
import monedero.modelo.Notificacion;
import monedero.modelo.TipoNotificacion;

import java.time.LocalDateTime;

/**
 * Buzon de notificaciones por cliente. Cada buzon es una ListaCircular de
 * capacidad fija: se conservan las CAPACIDAD mas recientes y las viejas se
 * sobrescriben en O(1), sin que la memoria crezca con el tiempo.
 *
 * Costos: notificar O(1) (hash + agregar circular), leer buzon O(CAPACIDAD).
 */
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

    /** De la mas vieja a la mas reciente. */
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
