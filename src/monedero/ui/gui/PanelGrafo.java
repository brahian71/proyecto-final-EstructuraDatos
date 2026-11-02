package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.estructuras.GrafoDirigido;
import monedero.estructuras.ListaEnlazada;
import monedero.estructuras.Mapa;
import monedero.estructuras.TablaHash;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.function.Function;

/**
 * Dibuja un grafo dirigido con los vertices en circulo y las aristas como
 * flechas; el grosor depende del peso. Se pueden resaltar aristas (por
 * ejemplo, el camino del BFS o un ciclo del DFS).
 */
final class PanelGrafo extends JPanel {

    private static final int RADIO_NODO = 28;

    private GrafoDirigido<String> grafo = new GrafoDirigido<>();
    private Function<String, String> nombre = v -> v;
    private Coleccion<String> resaltado = new ListaEnlazada<>();

    PanelGrafo() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(480, 380));
    }

    void mostrar(GrafoDirigido<String> grafo, Function<String, String> nombre) {
        this.grafo = grafo;
        this.nombre = nombre;
        this.resaltado = new ListaEnlazada<>();
        repaint();
    }

    /** Resalta las aristas consecutivas del camino v0 -> v1 -> ... */
    void resaltar(Coleccion<String> camino) {
        this.resaltado = camino;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int n = grafo.cantidadVertices();
        if (n == 0) {
            g.drawString("Sin datos", 20, 20);
            return;
        }
        Mapa<String, Point2D.Double> posicion = new TablaHash<>();
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;
        double r = Math.min(getWidth(), getHeight()) / 2.0 - RADIO_NODO - 30;
        int i = 0;
        for (String v : grafo.getVertices()) {
            double angulo = -Math.PI / 2 + 2 * Math.PI * i++ / n;
            posicion.poner(v, new Point2D.Double(cx + r * Math.cos(angulo), cy + r * Math.sin(angulo)));
        }

        long pesoMaximo = 1;
        for (GrafoDirigido.Arista<String> a : grafo.aristas()) {
            pesoMaximo = Math.max(pesoMaximo, a.getPeso());
        }
        for (GrafoDirigido.Arista<String> a : grafo.aristas()) {
            boolean destacada = estaResaltada(a.getOrigen(), a.getDestino());
            float grosor = 1.5f + 5f * a.getPeso() / pesoMaximo;
            g.setColor(destacada ? Formato.ERROR : new Color(0x5B7DB1));
            g.setStroke(new BasicStroke(destacada ? grosor + 1.5f : grosor));
            flecha(g, posicion.obtener(a.getOrigen()), posicion.obtener(a.getDestino()),
                    grafo.arista(a.getDestino(), a.getOrigen()) != null, Formato.pesos(a.getPeso()));
        }

        g.setStroke(new BasicStroke(2f));
        FontMetrics fm = g.getFontMetrics();
        for (String v : grafo.getVertices()) {
            Point2D.Double p = posicion.obtener(v);
            g.setColor(Formato.FONDO_TARJETA);
            g.fillOval((int) p.x - RADIO_NODO, (int) p.y - RADIO_NODO, 2 * RADIO_NODO, 2 * RADIO_NODO);
            g.setColor(Formato.PRIMARIO);
            g.drawOval((int) p.x - RADIO_NODO, (int) p.y - RADIO_NODO, 2 * RADIO_NODO, 2 * RADIO_NODO);
            g.drawString(v, (int) p.x - fm.stringWidth(v) / 2, (int) p.y + 4);
            String etiqueta = nombre.apply(v);
            int ancho = fm.stringWidth(etiqueta);
            int ex = (int) p.x - ancho / 2;
            int ey = (int) p.y + RADIO_NODO + 14;
            g.setColor(Color.WHITE); // fondo para que las flechas no tapen el nombre
            g.fillRect(ex - 3, ey - fm.getAscent(), ancho + 6, fm.getHeight());
            g.setColor(Color.DARK_GRAY);
            g.drawString(etiqueta, ex, ey);
        }
    }

    private boolean estaResaltada(String origen, String destino) {
        String anterior = null;
        for (String v : resaltado) {
            if (origen.equals(anterior) && destino.equals(v)) {
                return true;
            }
            anterior = v;
        }
        return false;
    }

    /**
     * Flecha de borde a borde de los circulos. Si tambien existe la arista
     * contraria, se desplaza a un lado para que las dos no se encimen.
     */
    private void flecha(Graphics2D g, Point2D.Double a, Point2D.Double b, boolean hayContraria, String texto) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double largo = Math.hypot(dx, dy);
        if (largo == 0) {
            return;
        }
        double ux = dx / largo;
        double uy = dy / largo;
        double desplazamiento = hayContraria ? 8 : 0;
        double ox = -uy * desplazamiento;
        double oy = ux * desplazamiento;
        double x1 = a.x + ux * RADIO_NODO + ox;
        double y1 = a.y + uy * RADIO_NODO + oy;
        double x2 = b.x - ux * (RADIO_NODO + 2) + ox;
        double y2 = b.y - uy * (RADIO_NODO + 2) + oy;
        g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);

        double punta = 12;
        int[] xs = {(int) x2, (int) (x2 - ux * punta - uy * punta / 2), (int) (x2 - ux * punta + uy * punta / 2)};
        int[] ys = {(int) y2, (int) (y2 - uy * punta + ux * punta / 2), (int) (y2 - uy * punta - ux * punta / 2)};
        g.fillPolygon(xs, ys, 3);

        Color antes = g.getColor();
        g.setColor(Color.BLACK);
        g.drawString(texto, (int) ((x1 + x2) / 2 + ox), (int) ((y1 + y2) / 2 + oy) - 4);
        g.setColor(antes);
    }
}
