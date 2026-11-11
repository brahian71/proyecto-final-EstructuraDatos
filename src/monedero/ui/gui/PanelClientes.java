package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.TipoMonedero;
import monedero.modelo.Usuario;
import monedero.servicio.ServicioUsuarios;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Arrays;

/**
 * Administrador: registrar clientes (con su usuario y contrasena), crear
 * monederos y ver a todos los clientes.
 * Cliente: crear monederos propios y ver sus monederos.
 */
final class PanelClientes extends JPanel implements Refrescable {

    private final Contexto contexto;
    private final ModeloTabla modelo;
    private final JLabel duenioMonedero = new JLabel();

    PanelClientes(Contexto contexto) {
        super(new BorderLayout(10, 10));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        boolean admin = contexto.esAdmin();
        modelo = admin
                ? new ModeloTabla("Id", "Nombre", "Usuario", "Rango", "Puntos", "Disponibles", "Saldo total", "Monederos")
                : new ModeloTabla("Id", "Nombre", "Tipo", "Saldo", "Transacciones");

        JPanel formularios = new JPanel(new GridLayout(admin ? 2 : 1, 1, 0, 10));
        if (admin) {
            formularios.add(formularioCliente());
        }
        formularios.add(formularioMonedero());
        add(formularios, BorderLayout.NORTH);
        add(Contexto.conBorde(new JScrollPane(modelo.crearTabla()), admin
                ? "Todos los clientes (tabla hash por id; aqui ordenados por id con un AVL)"
                : "Mis monederos"), BorderLayout.CENTER);
    }

    private JPanel formularioCliente() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(Contexto.tarjeta("Registrar cliente (se crea con un monedero Principal y su usuario para iniciar sesion)"));
        JTextField nombre = new JTextField(14);
        JTextField usuario = new JTextField(10);
        JPasswordField contrasena = new JPasswordField(10);
        JButton registrar = new JButton("Registrar");
        registrar.addActionListener(e -> {
            Cliente[] creado = new Cliente[1];
            char[] clave = contrasena.getPassword();
            try {
                if (contexto.ejecutar(null, () -> creado[0] = contexto.sistema()
                        .registrarClienteConUsuario(nombre.getText(), usuario.getText(), new String(clave)))) {
                    contexto.seleccionarCliente(creado[0].getId());
                    contexto.informar("Listo", "Cliente " + creado[0].getId() + " registrado. Puede iniciar sesion como "
                            + usuario.getText().trim().toLowerCase() + ".");
                    nombre.setText("");
                    usuario.setText("");
                }
            } finally {
                contrasena.setText("");
                Arrays.fill(clave, '\0');
            }
        });
        panel.add(new JLabel("Nombre:"));
        panel.add(nombre);
        panel.add(new JLabel("Usuario:"));
        panel.add(usuario);
        panel.add(new JLabel("Contrasena (min. " + ServicioUsuarios.MINIMO_CONTRASENA + "):"));
        panel.add(contrasena);
        panel.add(registrar);
        return panel;
    }

    private JPanel formularioMonedero() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(Contexto.tarjeta(contexto.esAdmin() ? "Crear monedero para el cliente actual" : "Crear un monedero"));
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
        panel.add(duenioMonedero);
        panel.add(new JLabel("Nombre:"));
        panel.add(nombreMonedero);
        panel.add(tipo);
        panel.add(crear);
        return panel;
    }

    @Override
    public void refrescar() {
        Cliente actual = contexto.cliente();
        duenioMonedero.setText(actual == null || !contexto.esAdmin() ? "" : actual.getNombre() + " -");
        Coleccion<Object[]> filas = ModeloTabla.nuevasFilas();
        if (contexto.esAdmin()) {
            for (Cliente c : contexto.clientesOrdenados()) {
                Usuario u = contexto.sistema().getUsuarios().usuarioDeCliente(c.getId());
                filas.agregar(new Object[]{c.getId(), c.getNombre(), u == null ? "-" : u.getNombreUsuario(), c.getRango(),
                        c.getPuntos(), c.getPuntosDisponibles(), Formato.pesos(c.saldoTotal()), c.cantidadMonederos()});
            }
        } else if (actual != null) {
            for (Monedero m : actual.getMonederos()) {
                filas.agregar(new Object[]{m.getId(), m.getNombre(), m.getTipo(), Formato.pesos(m.getSaldo()),
                        m.cantidadTransacciones()});
            }
        }
        modelo.setFilas(filas);
    }
}
