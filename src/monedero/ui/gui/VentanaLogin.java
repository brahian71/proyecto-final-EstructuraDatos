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

/**
 * Pantalla de inicio de sesion. Si las credenciales son correctas abre la
 * ventana principal con los permisos del rol del usuario.
 */
public class VentanaLogin extends JFrame {

    private final SistemaMonedero sistema;
    private final JTextField campoUsuario = new JTextField(16);
    private final JPasswordField campoContrasena = new JPasswordField(16);
    private final JLabel mensaje = new JLabel(" ", SwingConstants.CENTER);

    public VentanaLogin(SistemaMonedero sistema) {
        super("Monedero Virtual - Iniciar sesion");
        this.sistema = sistema;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(Formato.PRIMARIO);
        cabecera.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel titulo = new JLabel("Monedero Virtual", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        titulo.setForeground(java.awt.Color.WHITE);
        cabecera.add(titulo, BorderLayout.CENTER);
        add(cabecera, BorderLayout.NORTH);

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));
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

        JButton ingresar = new JButton("Ingresar");
        ingresar.addActionListener(e -> ingresar());
        g.gridx = 0;
        g.gridy = 2;
        g.gridwidth = 2;
        formulario.add(ingresar, g);
        mensaje.setForeground(Formato.ERROR);
        g.gridy = 3;
        formulario.add(mensaje, g);
        add(formulario, BorderLayout.CENTER);

        JLabel ayuda = new JLabel("<html><center>Usuarios de demostracion:<br>"
                + "admin / admin123 (administrador)<br>ana / ana123, bruno / bruno123 (clientes)</center></html>",
                SwingConstants.CENTER);
        ayuda.setFont(ayuda.getFont().deriveFont(Font.ITALIC, 11f));
        ayuda.setForeground(Formato.APAGADO);
        ayuda.setBorder(BorderFactory.createEmptyBorder(0, 10, 14, 10));
        add(ayuda, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(ingresar); // Enter inicia sesion
        setMinimumSize(new Dimension(380, 300));
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
            Arrays.fill(contrasena, '\0'); // no dejar la contrasena en memoria mas de lo necesario
        }
    }
}
