package monedero.ui.gui;

import monedero.modelo.Cliente;
import monedero.modelo.Usuario;
import monedero.servicio.ResultadoIntegridad;
import monedero.servicio.SistemaMonedero;
import monedero.servicio.VerificadorIntegridad;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

public class VentanaPrincipal extends JFrame implements Refrescable {

    private final Contexto contexto;
    private final JComboBox<String> comboCliente = new JComboBox<>();
    private final JLabel etiquetaFecha = new JLabel();
    private final JLabel etiquetaAvisos = new JLabel();
    private final JTabbedPane pestanias = new JTabbedPane();
    private boolean llenandoCombo;

    public VentanaPrincipal(SistemaMonedero sistema, Usuario usuario) {
        super("Monedero Virtual con Sistema de Puntos - " + usuario.getNombreUsuario());
        this.contexto = new Contexto(sistema, usuario);
        contexto.setVentana(this);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setLayout(new BorderLayout());
        add(crearBarra(), BorderLayout.NORTH);

        agregarPestania("Resumen", new PanelResumen(contexto));
        agregarPestania("Operaciones", new PanelOperaciones(contexto));
        agregarPestania("Historial", new PanelHistorial(contexto));
        agregarPestania("Puntos y canje", new PanelPuntos(contexto));
        agregarPestania("Programadas", new PanelProgramadas(contexto));
        agregarPestania("Reportes y grafos", new PanelReportes(contexto));
        agregarPestania(usuario.esAdministrador() ? "Clientes" : "Mis monederos", new PanelClientes(contexto));
        add(pestanias, BorderLayout.CENTER);

        pestanias.setFont(Estilo.fuente(java.awt.Font.BOLD, 13));
        pestanias.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        getContentPane().setBackground(Estilo.FONDO);
        contexto.registrar(this);
        contexto.refrescarTodo();
        Estilo.ajustarFondos(getContentPane(), false);
        pack();
        setLocationRelativeTo(null);
    }

    public static void aplicarAspecto() {
        Estilo.aplicarAspecto();
    }

    public JTabbedPane getPestanias() {
        return pestanias;
    }

    private void agregarPestania(String titulo, JPanel panel) {
        pestanias.addTab(titulo, panel);
        contexto.registrar((Refrescable) panel);
    }

    private JPanel crearBarra() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Estilo.CABECERA);
        barra.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel titulo = new JLabel("Monedero Virtual");
        titulo.setFont(Estilo.fuente(Font.BOLD, 18));
        titulo.setForeground(java.awt.Color.WHITE);
        barra.add(titulo, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        derecha.setOpaque(false);
        boolean admin = contexto.esAdmin();
        derecha.add(blanco(new JLabel(admin ? "Cliente:" : contexto.cliente().getNombre())));
        comboCliente.setVisible(admin);
        comboCliente.setPreferredSize(new Dimension(190, 28));
        comboCliente.addActionListener(e -> {
            if (!llenandoCombo && comboCliente.getSelectedItem() != null) {
                contexto.conManejo(() -> contexto.seleccionarCliente(Formato.idDe(comboCliente.getSelectedItem())));
            }
        });
        derecha.add(comboCliente);
        derecha.add(blanco(etiquetaAvisos));
        derecha.add(blanco(etiquetaFecha));

        JSpinner dias = new JSpinner(new SpinnerNumberModel(1, 1, 365, 1));
        dias.setPreferredSize(new Dimension(60, 28));
        dias.setVisible(admin);
        derecha.add(dias);
        JButton avanzar = Estilo.botonCabecera("Avanzar dias");
        avanzar.setToolTipText("Mueve el reloj simulado y ejecuta las transacciones programadas que venzan");
        avanzar.addActionListener(e -> {
            int[] procesadas = new int[1];
            if (contexto.ejecutar(null, () -> procesadas[0] = contexto.sistema().avanzarDias((Integer) dias.getValue()))) {
                contexto.informar("Reloj simulado", "Fecha actual: " + Formato.fecha(contexto.sistema().getReloj().ahora())
                        + "\nTransacciones programadas procesadas: " + procesadas[0]
                        + (procesadas[0] > 0 ? "\n(las que no se pudieron hacer aparecen en las notificaciones)" : ""));
            }
        });
        avanzar.setVisible(admin);
        derecha.add(avanzar);

        JButton integridad = Estilo.botonCabecera("Verificar integridad");
        integridad.addActionListener(e -> contexto.conManejo(this::mostrarIntegridad));
        integridad.setVisible(admin);
        derecha.add(integridad);

        JLabel sesion = blanco(new JLabel(contexto.usuario().getNombreUsuario()
                ));
        derecha.add(sesion);
        JButton salir = Estilo.botonCabecera("Cerrar sesion");
        salir.addActionListener(e -> {
            dispose();
            new VentanaLogin(contexto.sistema()).setVisible(true);
        });
        derecha.add(salir);

        barra.add(derecha, BorderLayout.EAST);
        return barra;
    }

    private void mostrarIntegridad() {
        ResultadoIntegridad resultado = new VerificadorIntegridad(contexto.sistema()).verificar();
        JTextArea texto = new JTextArea(resultado.toString(), 12, 60);
        texto.setEditable(false);
        texto.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        javax.swing.JOptionPane.showMessageDialog(this, new JScrollPane(texto), "Verificacion de integridad (recursiva)",
                resultado.esValido() ? javax.swing.JOptionPane.INFORMATION_MESSAGE : javax.swing.JOptionPane.WARNING_MESSAGE);
    }

    private static JLabel blanco(JLabel l) {
        l.setForeground(java.awt.Color.WHITE);
        return l;
    }

    @Override
    public void refrescar() {
        llenandoCombo = true;
        comboCliente.removeAllItems();
        Cliente actual = contexto.cliente();
        for (Cliente c : contexto.clientesOrdenados()) {
            comboCliente.addItem(Contexto.etiqueta(c));
            if (actual != null && c.getId().equals(actual.getId())) {
                comboCliente.setSelectedIndex(comboCliente.getItemCount() - 1);
            }
        }
        llenandoCombo = false;
        etiquetaFecha.setText("Hoy: " + Formato.fecha(contexto.sistema().getReloj().ahora()));
        int avisos = actual == null ? 0 : contexto.sistema().getNotificador().noLeidas(actual.getId());
        etiquetaAvisos.setText(avisos > 0 ? avisos + " sin leer" : "");
    }
}
