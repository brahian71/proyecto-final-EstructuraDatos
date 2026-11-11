package monedero.ui.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class Estilo {

    static final Color FONDO = new Color(0xF3F5F9);
    static final Color TARJETA = Color.WHITE;
    static final Color BORDE = new Color(0xE2E8F0);
    static final Color TEXTO = new Color(0x1F2937);
    static final Color TEXTO_SUAVE = new Color(0x6B7280);
    static final Color PRIMARIO = new Color(0x2563EB);
    static final Color PRIMARIO_OSCURO = new Color(0x1E40AF);
    static final Color CABECERA = new Color(0x1E3A8A);
    static final Color EXITO = new Color(0x16A34A);
    static final Color ERROR = new Color(0xDC2626);
    static final Color FILA_ALTERNA = new Color(0xF8FAFC);
    static final Color SELECCION = new Color(0xDBEAFE);

    static final String FUENTE = elegirFuente();

    private Estilo() {
    }

    private static String elegirFuente() {
        for (String nombre : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (nombre.equals("Segoe UI")) {
                return nombre;
            }
        }
        return Font.SANS_SERIF;
    }

    static Font fuente(int estilo, float tamanio) {
        return new Font(FUENTE, estilo, Math.round(tamanio));
    }

    static void aplicarAspecto() {
        UIManager.put("control", FONDO);
        UIManager.put("nimbusBase", new Color(0x3B5BA9));
        UIManager.put("nimbusBlueGrey", new Color(0xC5CEDB));
        UIManager.put("nimbusFocus", new Color(0x93C5FD));
        UIManager.put("nimbusSelectionBackground", PRIMARIO);
        UIManager.put("text", TEXTO);
        UIManager.put("nimbusOrange", PRIMARIO);
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            return;
        }
        UIManager.put("defaultFont", fuente(Font.PLAIN, 13));
        UIManager.getLookAndFeelDefaults().put("defaultFont", fuente(Font.PLAIN, 13));
        UIManager.put("Panel.background", FONDO);
        UIManager.put("OptionPane.background", TARJETA);
        UIManager.put("Table.alternateRowColor", FILA_ALTERNA);
    }

    static <T extends JComponent> T comoTarjeta(T componente, String titulo) {
        TitledBorder conTitulo = BorderFactory.createTitledBorder(new BordeRedondeado(BORDE, 12), " " + titulo + " ",
                TitledBorder.LEFT, TitledBorder.TOP, fuente(Font.BOLD, 13), TEXTO);
        componente.setBorder(BorderFactory.createCompoundBorder(conTitulo, BorderFactory.createEmptyBorder(6, 10, 10, 10)));
        componente.setBackground(TARJETA);
        componente.setOpaque(true);
        componente.putClientProperty("tarjeta", Boolean.TRUE);
        if (componente instanceof JScrollPane scroll) {
            scroll.getViewport().setBackground(TARJETA);
        }
        return componente;
    }

    static Border tarjetaSinTitulo() {
        return BorderFactory.createCompoundBorder(new BordeRedondeado(BORDE, 12), BorderFactory.createEmptyBorder(12, 14, 12, 14));
    }

    static void ajustarFondos(Container raiz, boolean dentroDeTarjeta) {
        for (Component c : raiz.getComponents()) {
            boolean esTarjeta = c instanceof JComponent jc && Boolean.TRUE.equals(jc.getClientProperty("tarjeta"));
            if (dentroDeTarjeta && !esTarjeta && c instanceof JPanel panel) {
                panel.setOpaque(false);
            }
            if (c instanceof Container hijo) {
                ajustarFondos(hijo, dentroDeTarjeta || esTarjeta);
            }
        }
    }

    static JButton botonPrimario(String texto) {
        return new BotonPlano(texto, PRIMARIO, PRIMARIO_OSCURO, Color.WHITE);
    }

    static JButton botonSecundario(String texto) {
        return new BotonPlano(texto, new Color(0xEEF2F7), new Color(0xE2E8F0), TEXTO);
    }

    static JButton botonCabecera(String texto) {
        return new BotonPlano(texto, new Color(0x2F4FA3), new Color(0x3B5FC0), Color.WHITE);
    }

    static JLabel titulo(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(fuente(Font.BOLD, 18));
        l.setForeground(TEXTO);
        return l;
    }

    static void estilizarTabla(JTable tabla) {
        tabla.setRowHeight(28);
        tabla.setShowVerticalLines(false);
        tabla.setShowHorizontalLines(true);
        tabla.setGridColor(new Color(0xEEF1F5));
        tabla.setIntercellSpacing(new Dimension(0, 1));
        tabla.setSelectionBackground(SELECCION);
        tabla.setSelectionForeground(TEXTO);
        tabla.setFont(fuente(Font.PLAIN, 13));
        tabla.setDefaultRenderer(Object.class, new RenderizadorCelda());
        JTableHeader cabecera = tabla.getTableHeader();
        cabecera.setFont(fuente(Font.BOLD, 12));
        cabecera.setPreferredSize(new Dimension(cabecera.getPreferredSize().width, 30));
        cabecera.setDefaultRenderer(new RenderizadorCabecera());
    }

    static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length && i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    static class RenderizadorCelda extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada, boolean foco,
                                                       int fila, int columna) {
            super.getTableCellRendererComponent(t, valor, seleccionada, false, fila, columna);
            String texto = valor == null ? "" : valor.toString();
            boolean dinero = texto.startsWith("$") || texto.startsWith("-$") || texto.startsWith("+$");
            setHorizontalAlignment(dinero ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            if (!seleccionada) {
                setBackground(fila % 2 == 0 ? TARJETA : FILA_ALTERNA);
                setForeground(texto.startsWith("-$") ? Estilo.ERROR : Estilo.TEXTO);
            }
            return this;
        }
    }

    static class RenderizadorCabecera extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada, boolean foco,
                                                       int fila, int columna) {
            super.getTableCellRendererComponent(t, valor, false, false, fila, columna);
            setFont(fuente(Font.BOLD, 12));
            setForeground(TEXTO_SUAVE);
            setBackground(new Color(0xF1F5F9));
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDE),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            setOpaque(true);
            return this;
        }
    }

    static class BordeRedondeado extends AbstractBorder {
        private final Color color;
        private final int radio;

        BordeRedondeado(Color color, int radio) {
            this.color = color;
            this.radio = radio;
        }

        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int ancho, int alto) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.drawRoundRect(x, y, ancho - 1, alto - 1, radio, radio);
            g.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 6, 4, 6);
        }
    }

    static class BotonPlano extends JButton {
        private final Color fondo;
        private final Color fondoEncima;
        private boolean encima;

        BotonPlano(String texto, Color fondo, Color fondoEncima, Color colorTexto) {
            super(texto);
            this.fondo = fondo;
            this.fondoEncima = fondoEncima;
            setForeground(colorTexto);
            setFont(fuente(Font.BOLD, 13));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    encima = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    encima = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color color = !isEnabled() ? new Color(0xE5E7EB) : (encima || getModel().isPressed() ? fondoEncima : fondo);
            g.setColor(color);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
