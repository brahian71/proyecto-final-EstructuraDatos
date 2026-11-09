package monedero.ui.gui;

import monedero.modelo.Cliente;
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

/**
 * Ventana principal. Arriba: cliente con el que se trabaja, fecha del reloj
 * simulado, avanzar dias (ejecuta las programadas) y verificar integridad.
 * Al centro, una pestania por funcionalidad. Toda la logica esta en
 * SistemaMonedero; esta clase y los paneles solo leen y llaman metodos.
 */
public class VentanaPrincipal extends JFrame implements Refrescable {

    private final Contexto contexto;
    private final JComboBox<String> comboCliente = new JComboBox<>();
    private final JLabel etiquetaFecha = new JLabel();
    private final JLabel etiquetaAvisos = new JLabel();
    private final JTabbedPane pestanias = new JTabbedPane();
    private boolean llenandoCombo;

    public VentanaPrincipal(SistemaMonedero sistema) {
        super("Monedero Virtual con Sistema de Puntos");
        this.contexto = new Contexto(sistema);
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
        agregarPestania("Clientes", new PanelClientes(contexto));
        add(pestanias, BorderLayout.CENTER);

        contexto.registrar(this);
        contexto.refrescarTodo();
        pack();
        setLocationRelativeTo(null);
    }

    /** Usa el aspecto Nimbus (viene con Java); si no esta, se queda con el por defecto. */
    public static void aplicarAspecto() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception e) {
            // aspecto por defecto
        }
    }

    /** Para las capturas del informe. */
    public JTabbedPane getPestanias() {
        return pestanias;
    }

    private void agregarPestania(String titulo, JPanel panel) {
        pestanias.addTab(titulo, panel);
        contexto.registrar((Refrescable) panel);
    }

    private JPanel crearBarra() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Formato.PRIMARIO);
        barra.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel titulo = new JLabel("Monedero Virtual");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));
        titulo.setForeground(java.awt.Color.WHITE);
        barra.add(titulo, BorderLayout.WEST);

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        derecha.setOpaque(false);
        derecha.add(blanco(new JLabel("Cliente:")));
        comboCliente.setPreferredSize(new Dimension(220, 28));
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
        derecha.add(dias);
        JButton avanzar = new JButton("Avanzar dias");
        avanzar.setToolTipText("Mueve el reloj simulado y ejecuta las transacciones programadas que venzan");
        avanzar.addActionListener(e -> {
            int[] procesadas = new int[1];
            if (contexto.ejecutar(null, () -> procesadas[0] = contexto.sistema().avanzarDias((Integer) dias.getValue()))) {
                contexto.informar("Reloj simulado", "Fecha actual: " + Formato.fecha(contexto.sistema().getReloj().ahora())
                        + "\nTransacciones programadas procesadas: " + procesadas[0]
                        + (procesadas[0] > 0 ? "\n(las que no se pudieron hacer aparecen en las notificaciones)" : ""));
            }
        });
        derecha.add(avanzar);

        JButton integridad = new JButton("Verificar integridad");
        integridad.addActionListener(e -> contexto.conManejo(this::mostrarIntegridad));
        derecha.add(integridad);

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
        etiquetaAvisos.setText(avisos > 0 ? "✉ " + avisos + " sin leer" : "");
    }
}
