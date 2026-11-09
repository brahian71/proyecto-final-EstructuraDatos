package monedero.ui.gui;

import monedero.estructuras.ArbolAVL;
import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.servicio.SistemaMonedero;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.Border;
import java.awt.Font;

/**
 * Estado compartido por todas las vistas: el sistema, el cliente con el que
 * se esta trabajando y la lista de vistas a refrescar despues de cada
 * operacion. Ninguna vista llama a otra directamente.
 */
final class Contexto {

    private final SistemaMonedero sistema;
    private final Coleccion<Refrescable> vistas = new ListaEnlazada<>();
    private JFrame ventana;
    private String clienteActual;

    Contexto(SistemaMonedero sistema) {
        this.sistema = sistema;
        Cliente primero = clientesOrdenados().estaVacia() ? null : clientesOrdenados().obtener(0);
        this.clienteActual = primero == null ? null : primero.getId();
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

    // ------------------------------------------------------------------ cliente actual

    Cliente cliente() {
        return clienteActual == null ? null : sistema.buscarCliente(clienteActual);
    }

    void seleccionarCliente(String clienteId) {
        if (clienteId != null && !clienteId.equals(clienteActual)) {
            clienteActual = clienteId;
            refrescarTodo();
        }
    }

    /**
     * Clientes ordenados por id (la tabla hash no guarda orden). Se usa un AVL
     * y no un BST simple: los ids salen de la tabla casi en orden (C001, C002...)
     * y con claves ordenadas un BST simple quedaria como una lista.
     */
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
        return m.getId() + " " + m.getNombre() + " (" + sistema.buscarCliente(m.getClienteId()).getNombre()
                + ") " + Formato.pesos(m.getSaldo());
    }

    /** Recarga el combo con los monederos del cliente actual (o todos) conservando la seleccion. */
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

    // ------------------------------------------------------------------ acciones y mensajes

    /**
     * Ejecuta una accion del usuario: si funciona refresca todo y muestra el
     * mensaje de exito (si hay); si falla muestra el error de negocio
     * (saldo insuficiente, monto invalido...) sin cerrar nada.
     *
     * La accion y el refresco se manejan por separado: si la accion ya se hizo
     * y solo falla el refresco, no se le dice al usuario que la operacion fallo.
     * @return true si la accion se hizo
     */
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

    /** Para acciones que no cambian datos (consultas) y para los listeners: muestra el error en vez de perderlo. */
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

    // ------------------------------------------------------------------ estilo

    static Border tarjeta(String titulo) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Formato.BORDE), titulo),
                BorderFactory.createEmptyBorder(6, 8, 8, 8));
    }

    static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 15f));
        l.setForeground(Formato.PRIMARIO);
        return l;
    }

    static <T extends JComponent> T conBorde(T componente, String titulo) {
        componente.setBorder(tarjeta(titulo));
        return componente;
    }
}
