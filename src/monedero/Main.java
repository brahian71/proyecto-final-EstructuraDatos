package monedero;

import monedero.datos.DatosIniciales;
import monedero.servicio.SistemaMonedero;
import monedero.ui.MenuConsola;
import monedero.ui.gui.VentanaLogin;
import monedero.ui.gui.VentanaPrincipal;

import javax.swing.SwingUtilities;
import java.util.Scanner;

/**
 * Abre la pantalla de inicio de sesion. Con el argumento --consola usa el
 * menu de texto (se dejo para desarrollo y pruebas rapidas).
 */
public class Main {

    public static void main(String[] args) {
        SistemaMonedero sistema = new SistemaMonedero();
        DatosIniciales.cargar(sistema);

        if (args.length > 0 && args[0].equals("--consola")) {
            try (Scanner entrada = new Scanner(System.in)) {
                new MenuConsola(sistema, entrada).iniciar();
            }
            return;
        }
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal.aplicarAspecto();
            new VentanaLogin(sistema).setVisible(true);
        });
    }
}
