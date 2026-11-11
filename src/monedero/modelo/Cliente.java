package monedero.modelo;

import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;
import monedero.estructuras.Pila;
import monedero.estructuras.PilaEnlazada;

import java.time.LocalDateTime;

public class Cliente {

    private final String id;
    private final String nombre;
    private final Coleccion<Monedero> monederos = new ListaEnlazada<>();
    private final Pila<Transaccion> reversibles = new PilaEnlazada<>();
    private final Coleccion<MovimientoPuntos> historialPuntos = new ListaEnlazada<>();
    private final Coleccion<BeneficioActivo> beneficios = new ListaEnlazada<>();
    private int puntos;
    private int puntosCanjeados;

    public Cliente(String id, String nombre) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id no puede ser nulo ni vacio");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio");
        }
        this.id = id.trim();
        this.nombre = nombre.trim();
    }

    public void agregarMonedero(Monedero monedero) {
        if (!id.equals(monedero.getClienteId())) {
            throw new IllegalArgumentException("El monedero " + monedero.getId() + " no pertenece a " + id);
        }
        monederos.agregar(monedero);
    }

    public void ganarPuntos(int cantidad, LocalDateTime fecha, String detalle) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa: " + cantidad);
        }
        if (cantidad == 0) {
            return;
        }
        puntos = Math.addExact(puntos, cantidad);
        historialPuntos.agregar(new MovimientoPuntos(MovimientoPuntos.Tipo.GANADOS, cantidad, fecha, detalle,
                getPuntosDisponibles()));
    }

    public void perderPuntos(int cantidad, LocalDateTime fecha, String detalle) {
        validarDisponibles(cantidad);
        if (cantidad == 0) {
            return;
        }
        puntos -= cantidad;
        historialPuntos.agregar(new MovimientoPuntos(MovimientoPuntos.Tipo.REVERTIDOS, -cantidad, fecha, detalle,
                getPuntosDisponibles()));
    }

    public void canjear(Beneficio beneficio, LocalDateTime fecha) {
        validarDisponibles(beneficio.getCostoPuntos());
        puntosCanjeados += beneficio.getCostoPuntos();
        historialPuntos.agregar(new MovimientoPuntos(MovimientoPuntos.Tipo.CANJEADOS, -beneficio.getCostoPuntos(),
                fecha, beneficio.name(), getPuntosDisponibles()));
        if (beneficio.getDuracionDias() > 0) {
            beneficios.agregar(new BeneficioActivo(beneficio, fecha.plusDays(beneficio.getDuracionDias())));
        }
    }

    public boolean tieneBeneficio(Beneficio beneficio, LocalDateTime fecha) {
        for (BeneficioActivo activo : beneficios) {
            if (activo.getBeneficio() == beneficio && activo.vigenteEn(fecha)) {
                return true;
            }
        }
        return false;
    }

    private void validarDisponibles(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa: " + cantidad);
        }
        if (cantidad > getPuntosDisponibles()) {
            throw new IllegalStateException("Puntos insuficientes: tiene " + getPuntosDisponibles()
                    + " disponibles, se piden " + cantidad);
        }
    }

    public void registrarReversible(Transaccion transaccion) {
        reversibles.apilar(transaccion);
    }

    public Transaccion ultimaReversible() {
        return reversibles.cima();
    }

    public Transaccion sacarReversible() {
        return reversibles.desapilar();
    }

    public boolean tieneReversibles() {
        return !reversibles.estaVacia();
    }

    public Rango getRango() {
        return Rango.desdePuntos(puntos);
    }

    public long saldoTotal() {
        long total = 0;
        for (Monedero monedero : monederos) {
            total += monedero.getSaldo();
        }
        return total;
    }

    public Iterable<Monedero> getMonederos() {
        return monederos;
    }

    public int cantidadMonederos() {
        return monederos.tamanio();
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPuntos() {
        return puntos;
    }

    public int getPuntosDisponibles() {
        return puntos - puntosCanjeados;
    }

    public int getPuntosCanjeados() {
        return puntosCanjeados;
    }

    public Iterable<MovimientoPuntos> getHistorialPuntos() {
        return historialPuntos;
    }

    public int cantidadMovimientosPuntos() {
        return historialPuntos.tamanio();
    }

    public Iterable<BeneficioActivo> getBeneficios() {
        return beneficios;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cliente otro)) {
            return false;
        }
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + nombre + " - " + puntos + " pts (" + getRango() + ")"
                + (puntosCanjeados > 0 ? ", " + getPuntosDisponibles() + " disponibles" : "");
    }
}
