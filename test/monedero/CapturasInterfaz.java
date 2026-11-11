package monedero;

import monedero.datos.DatosIniciales;
import monedero.servicio.Reloj;
import monedero.servicio.SistemaMonedero;
import monedero.modelo.Usuario;
import monedero.ui.gui.VentanaLogin;
import monedero.ui.gui.VentanaPrincipal;

import javax.imageio.ImageIO;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;

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
        sistema.avanzarDias(10);

        VentanaPrincipal.aplicarAspecto();
        guardarLogin(sistema, carpeta);
        Usuario admin = sistema.getUsuarios().iniciarSesion(DatosIniciales.USUARIO_ADMIN, DatosIniciales.CONTRASENA_ADMIN);
        VentanaPrincipal ventana = new VentanaPrincipal(sistema, admin);
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

        Usuario ana = sistema.getUsuarios().iniciarSesion("ana", "ana123");
        VentanaPrincipal vistaCliente = new VentanaPrincipal(sistema, ana);
        vistaCliente.setSize(1280, 760);
        vistaCliente.validate();
        guardar(vistaCliente.getContentPane(), new File(carpeta, "8-vista-cliente.png"));
        vistaCliente.dispose();
    }

    private static void guardarLogin(SistemaMonedero sistema, File carpeta) throws Exception {
        VentanaLogin login = new VentanaLogin(sistema);
        login.validate();
        guardar(login.getContentPane(), new File(carpeta, "0-inicio-de-sesion.png"));
        login.dispose();
    }

    private static void guardar(java.awt.Container panel, File archivo) throws Exception {
        BufferedImage imagen = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        panel.printAll(g);
        g.dispose();
        ImageIO.write(imagen, "png", archivo);
        System.out.println("Captura: " + archivo.getPath());
    }
}
