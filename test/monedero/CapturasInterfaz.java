package monedero;

import monedero.datos.DatosIniciales;
import monedero.servicio.Reloj;
import monedero.servicio.SistemaMonedero;
import monedero.ui.gui.VentanaPrincipal;

import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;

/**
 * Genera una imagen PNG de cada pestania de la interfaz (sin mostrar la
 * ventana) en docs/capturas. Sirve para revisar la interfaz y para el informe.
 * Uso: java -cp out monedero.CapturasInterfaz
 */
public class CapturasInterfaz {

    public static void main(String[] args) throws Exception {
        File carpeta = new File(args.length > 0 ? args[0] : "docs/capturas");
        carpeta.mkdirs();
        SwingUtilities.invokeAndWait(() -> {
            try {
                capturar(carpeta);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.exit(0);
    }

    private static void capturar(File carpeta) throws Exception {
        SistemaMonedero sistema = new SistemaMonedero(new Reloj(LocalDateTime.of(2026, 10, 27, 8, 0)));
        DatosIniciales.cargar(sistema);
        sistema.avanzarDias(10); // que se ejecuten algunas programadas

        VentanaPrincipal.aplicarAspecto();
        VentanaPrincipal ventana = new VentanaPrincipal(sistema);
        ventana.setSize(1280, 760);
        ventana.validate();

        JTabbedPane pestanias = ventana.getPestanias();
        for (int i = 0; i < pestanias.getTabCount(); i++) {
            pestanias.setSelectedIndex(i);
            ventana.validate();
            BufferedImage imagen = new BufferedImage(ventana.getContentPane().getWidth(),
                    ventana.getContentPane().getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = imagen.createGraphics();
            ventana.getContentPane().printAll(g);
            g.dispose();
            String nombre = String.format("%d-%s.png", i + 1,
                    pestanias.getTitleAt(i).toLowerCase().replace(' ', '-'));
            ImageIO.write(imagen, "png", new File(carpeta, nombre));
            System.out.println("Captura: " + new File(carpeta, nombre).getPath());
        }
        ventana.dispose();
    }
}
