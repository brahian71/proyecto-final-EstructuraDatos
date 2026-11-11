package monedero.ui.gui;

import monedero.excepciones.AutenticacionException;
import monedero.modelo.Usuario;
import monedero.servicio.SistemaMonedero;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

public class VentanaLogin extends JFrame {

    private final SistemaMonedero sistema;
    private final JTextField campoUsuario = new JTextField(16);
    private final JPasswordField campoContrasena = new JPasswordField(16);
    private final JLabel mensaje = new JLabel(" ", SwingConstants.CENTER);

    public VentanaLogin(SistemaMonedero sistema) {
        super("Monedero Virtual - Iniciar sesion");
        this.sistema = sistema;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(Estilo.FONDO);

        JPanel tarjeta = new JPanel(new BorderLayout(0, 6));
        tarjeta.setBackground(Estilo.TARJETA);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(new Estilo.BordeRedondeado(Estilo.BORDE, 16),
                BorderFactory.createEmptyBorder(22, 28, 18, 28)));
        add(tarjeta);

        JPanel cabecera = new JPanel(new BorderLayout(0, 4));
        cabecera.setOpaque(false);
        JLabel titulo = new JLabel("Monedero Virtual", SwingConstants.CENTER);
        titulo.setFont(Estilo.fuente(Font.BOLD, 24));
        titulo.setForeground(Estilo.CABECERA);
        JLabel subtitulo = new JLabel("Inicie sesion para continuar", SwingConstants.CENTER);
        subtitulo.setFont(Estilo.fuente(Font.PLAIN, 13));
        subtitulo.setForeground(Estilo.TEXTO_SUAVE);
        cabecera.add(titulo, BorderLayout.CENTER);
        cabecera.add(subtitulo, BorderLayout.SOUTH);
        tarjeta.add(cabecera, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setOpaque(false);
        formulario.setBorder(BorderFactory.createEmptyBorder(14, 0, 4, 0));
        campoUsuario.setFont(Estilo.fuente(Font.PLAIN, 14));
        campoContrasena.setFont(Estilo.fuente(Font.PLAIN, 14));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = 0;
        formulario.add(new JLabel("Usuario:"), g);
        g.gridx = 1;
        formulario.add(campoUsuario, g);
        g.gridx = 0;
        g.gridy = 1;
        formulario.add(new JLabel("Contrasena:"), g);
        g.gridx = 1;
        formulario.add(campoContrasena, g);

        JButton ingresar = Estilo.botonPrimario("Ingresar");
        ingresar.addActionListener(e -> ingresar());
        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        formulario.add(ingresar, g);
        mensaje.setForeground(Formato.ERROR);
        g.gridy = 3;
        formulario.add(mensaje, g);
        tarjeta.add(formulario, BorderLayout.CENTER);

        JLabel ayuda = new JLabel("<html><center>Usuarios de demostracion:<br>"
                + "admin / admin123 (administrador)<br>ana / ana123, bruno / bruno123 (clientes)</center></html>",
                SwingConstants.CENTER);
        ayuda.setFont(Estilo.fuente(Font.ITALIC, 11));
        ayuda.setForeground(Formato.APAGADO);
        tarjeta.add(ayuda, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(ingresar);
        setMinimumSize(new Dimension(460, 420));
        pack();
        setLocationRelativeTo(null);
    }

    private void ingresar() {
        char[] contrasena = campoContrasena.getPassword();
        try {
            Usuario usuario = sistema.getUsuarios().iniciarSesion(campoUsuario.getText(), new String(contrasena));
            dispose();
            new VentanaPrincipal(sistema, usuario).setVisible(true);
        } catch (AutenticacionException e) {
            mensaje.setText(e.getMessage());
            campoContrasena.setText("");
            campoContrasena.requestFocusInWindow();
        } finally {
            Arrays.fill(contrasena, '\0');
        }
    }
}
