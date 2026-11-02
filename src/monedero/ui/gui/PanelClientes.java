package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.TipoMonedero;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/** Registrar clientes, crear monederos y ver a todos los clientes. */
final class PanelClientes extends JPanel implements Refrescable {

    private final Contexto contexto;
    private final ModeloTabla modelo = new ModeloTabla("Id", "Nombre", "Rango", "Puntos", "Disponibles",
            "Saldo total", "Monederos");
    private final JLabel duenioMonedero = new JLabel();

    PanelClientes(Contexto contexto) {
        super(new BorderLayout(10, 10));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formularios = new JPanel(new GridLayout(1, 2, 10, 0));

        JPanel nuevoCliente = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nuevoCliente.setBorder(Contexto.tarjeta("Registrar cliente (se crea con un monedero Principal)"));
        JTextField nombre = new JTextField(18);
        JButton registrar = new JButton("Registrar");
        registrar.addActionListener(e -> {
            Cliente[] creado = new Cliente[1];
            if (contexto.ejecutar(null, () -> creado[0] = contexto.sistema().registrarCliente(nombre.getText()))) {
                nombre.setText("");
                contexto.seleccionarCliente(creado[0].getId());
                contexto.informar("Listo", "Cliente " + creado[0].getId() + " registrado.");
            }
        });
        nuevoCliente.add(new JLabel("Nombre:"));
        nuevoCliente.add(nombre);
        nuevoCliente.add(registrar);
        formularios.add(nuevoCliente);

        JPanel nuevoMonedero = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nuevoMonedero.setBorder(Contexto.tarjeta("Crear monedero para el cliente actual"));
        JTextField nombreMonedero = new JTextField(12);
        JComboBox<TipoMonedero> tipo = new JComboBox<>(TipoMonedero.values());
        tipo.setSelectedItem(TipoMonedero.AHORROS);
        JButton crear = new JButton("Crear");
        crear.addActionListener(e -> {
            Cliente c = contexto.cliente();
            Monedero[] creado = new Monedero[1];
            if (c != null && contexto.ejecutar(null, () -> creado[0] = contexto.sistema().crearMonedero(c.getId(),
                    nombreMonedero.getText(), (TipoMonedero) tipo.getSelectedItem()))) {
                nombreMonedero.setText("");
                contexto.informar("Listo", "Monedero " + creado[0].getId() + " creado para " + c.getNombre() + ".");
            }
        });
        nuevoMonedero.add(duenioMonedero);
        nuevoMonedero.add(new JLabel("Nombre:"));
        nuevoMonedero.add(nombreMonedero);
        nuevoMonedero.add(tipo);
        nuevoMonedero.add(crear);
        formularios.add(nuevoMonedero);

        add(formularios, BorderLayout.NORTH);
        add(Contexto.conBorde(new JScrollPane(modelo.crearTabla()),
                "Todos los clientes (tabla hash por id; aqui ordenados por id con un BST)"), BorderLayout.CENTER);
    }

    @Override
    public void refrescar() {
        Cliente actual = contexto.cliente();
        duenioMonedero.setText(actual == null ? "" : actual.getNombre() + " -");
        Coleccion<Object[]> filas = ModeloTabla.nuevasFilas();
        for (Cliente c : contexto.clientesOrdenados()) {
            filas.agregar(new Object[]{c.getId(), c.getNombre(), c.getRango(), c.getPuntos(), c.getPuntosDisponibles(),
                    Formato.pesos(c.saldoTotal()), c.cantidadMonederos()});
        }
        modelo.setFilas(filas);
    }
}
