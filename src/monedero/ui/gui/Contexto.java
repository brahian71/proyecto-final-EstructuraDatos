package monedero.ui.gui;

import monedero.estructuras.ArbolAVL;
import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.Usuario;
import monedero.servicio.SistemaMonedero;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

final class Contexto {

    private final SistemaMonedero sistema;
    private final Usuario usuario;
    private final Coleccion<Refrescable> vistas = new ListaEnlazada<>();
    private JFrame ventana;
    private String clienteActual;

    Contexto(SistemaMonedero sistema, Usuario usuario) {
        this.sistema = sistema;
        this.usuario = usuario;
        if (usuario.esAdministrador()) {
            Cliente primero = clientesOrdenados().estaVacia() ? null : clientesOrdenados().obtener(0);
            this.clienteActual = primero == null ? null : primero.getId();
        } else {
            this.clienteActual = usuario.getClienteId();
        }
    }

    Usuario usuario() {
        return usuario;
    }

    boolean esAdmin() {
        return usuario.esAdministrador();
    }

    SistemaMonedero sistema() {
        return sistema;
    }

    void setVentana(JFrame ventana) {
        this.ventana = ventana;
    }

    void registrar(Refrescable vista) {
        vistas.agregar(vista);
    }

    void refrescarTodo() {
        for (Refrescable vista : vistas) {
            vista.refrescar();
        }
    }

    Cliente cliente() {
        return clienteActual == null ? null : sistema.buscarCliente(clienteActual);
    }

    void seleccionarCliente(String clienteId) {
        if (!esAdmin()) {
            return;
        }
        if (clienteId != null && !clienteId.equals(clienteActual)) {
            clienteActual = clienteId;
            refrescarTodo();
        }
    }

    Coleccion<Cliente> clientesOrdenados() {
        ArbolAVL<String, Cliente> orden = new ArbolAVL<>();
        for (Cliente c : sistema.getClientes()) {
            orden.insertar(c.getId(), c);
        }
        return orden.enOrden();
    }

    Coleccion<Monedero> monederosOrdenados() {
        ArbolAVL<String, Monedero> orden = new ArbolAVL<>();
        for (Monedero m : sistema.getMonederos()) {
            orden.insertar(m.getId(), m);
        }
        return orden.enOrden();
    }

    static String etiqueta(Cliente c) {
        return c.getId() + " - " + c.getNombre();
    }

    String etiqueta(Monedero m) {
        boolean propio = m.getClienteId().equals(clienteActual);
        return m.getId() + " " + m.getNombre() + " (" + sistema.buscarCliente(m.getClienteId()).getNombre() + ")"
                + (esAdmin() || propio ? " " + Formato.pesos(m.getSaldo()) : "");
    }

    void llenarMonederos(JComboBox<String> combo, boolean todos) {
        String antes = combo.getSelectedItem() == null ? null : Formato.idDe(combo.getSelectedItem());
        combo.removeAllItems();
        Iterable<Monedero> fuente = todos ? monederosOrdenados()
                : cliente() == null ? new ListaEnlazada<>() : cliente().getMonederos();
        for (Monedero m : fuente) {
            combo.addItem(etiqueta(m));
            if (m.getId().equals(antes)) {
                combo.setSelectedIndex(combo.getItemCount() - 1);
            }
        }
    }

    boolean ejecutar(String exito, Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException e) {
            error(e.getMessage());
            return false;
        }
        conManejo(this::refrescarTodo);
        if (exito != null) {
            JOptionPane.showMessageDialog(ventana, exito, "Listo", JOptionPane.INFORMATION_MESSAGE);
        }
        return true;
    }

    void conManejo(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException e) {
            error(e.getMessage());
        }
    }

    void error(String mensaje) {
        JOptionPane.showMessageDialog(ventana, mensaje == null ? "Error inesperado" : mensaje,
                "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    boolean confirmar(String pregunta) {
        return JOptionPane.showConfirmDialog(ventana, pregunta, "Confirmar",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    void informar(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(ventana, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }
}
