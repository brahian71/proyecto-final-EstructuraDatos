package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.modelo.Cliente;
import monedero.servicio.AnalisisGastos;
import monedero.servicio.FilaReporte;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

final class PanelReportes extends JPanel implements Refrescable {

    private static final String[] REPORTES = {
            "1. Gasto por categoria del cliente actual",
            "2. Secuencias de gasto mas frecuentes (todos)",
            "3. Clientes que mas reciben por transferencias",
            "4. Ciclos de transferencias (DFS)",
            "5. Conexion del cliente actual con otro (BFS)",
            "6. Flujo entre monederos del cliente actual"};

    private final Contexto contexto;
    private final int[] disponibles;
    private final JComboBox<String> reporte = new JComboBox<>();
    private final JComboBox<String> otroCliente = new JComboBox<>();
    private final ModeloTabla modelo = new ModeloTabla("Concepto", "Dinero", "Veces");
    private final JTextArea explicacion = new JTextArea(3, 40);
    private final PanelGrafo dibujo = new PanelGrafo();

    PanelReportes(Contexto contexto) {
        super(new GridLayout(1, contexto.esAdmin() ? 2 : 1, 10, 0));
        this.contexto = contexto;
        this.disponibles = contexto.esAdmin() ? new int[]{0, 1, 2, 3, 4, 5} : new int[]{0, 1, 5};
        for (int i : disponibles) {
            reporte.addItem(i == 1 && !contexto.esAdmin() ? "2. Mis secuencias de gasto mas frecuentes" : REPORTES[i]);
        }
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel izquierda = new JPanel(new BorderLayout(0, 8));
        JPanel opciones = new JPanel(new BorderLayout(10, 8));
        Estilo.comoTarjeta(opciones, "Reporte");
        opciones.add(reporte, BorderLayout.NORTH);
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fila.add(new JLabel("Otro cliente:"));
        fila.add(otroCliente);
        fila.setVisible(contexto.esAdmin());
        opciones.add(fila, BorderLayout.CENTER);
        JButton generar = Estilo.botonPrimario("Generar");
        generar.addActionListener(e -> generar());
        opciones.add(generar, BorderLayout.EAST);
        izquierda.add(opciones, BorderLayout.NORTH);
        izquierda.add(Estilo.comoTarjeta(new JScrollPane(modelo.crearTabla()), "Resultado"), BorderLayout.CENTER);
        explicacion.setEditable(false);
        explicacion.setLineWrap(true);
        explicacion.setWrapStyleWord(true);
        explicacion.setFont(explicacion.getFont().deriveFont(Font.PLAIN, 13f));
        izquierda.add(Estilo.comoTarjeta(new JScrollPane(explicacion), "Detalle"), BorderLayout.SOUTH);
        add(izquierda);

        if (contexto.esAdmin()) {
            add(Estilo.comoTarjeta(dibujo, "Grafo de transferencias entre clientes (grosor = dinero)"));
        }
        reporte.addActionListener(e -> otroCliente.setEnabled(seleccionado() == 4));
        otroCliente.setEnabled(false);
        otroCliente.setVisible(contexto.esAdmin());
    }

    private void generar() {
        Cliente actual = contexto.cliente();
        if (actual == null) {
            return;
        }
        int elegido = seleccionado();
        if (elegido == 4 && otroCliente.getItemCount() == 0) {
            contexto.informar("Reportes", "Registre otro cliente para ver como esta conectado con " + actual.getNombre() + ".");
            return;
        }
        AnalisisGastos analisis = new AnalisisGastos(contexto.sistema());
        contexto.conManejo(() -> {
            dibujo.mostrar(analisis.grafoTransferencias(), id -> contexto.sistema().buscarCliente(id).getNombre());
            switch (elegido) {
                case 0 -> tabla(analisis.gastoPorCategoria(actual.getId()),
                        "Gasto de " + actual.getNombre() + " por categoria (retiros y transferencias a otros clientes).");
                case 1 -> tabla(analisis.secuenciasFrecuentes(contexto.esAdmin() ? null : actual.getId(), 10),
                        "Aristas mas repetidas del grafo de categorias: X -> Y = despues de gastar en X, el mismo cliente gasto en Y.");
                case 2 -> tabla(analisis.mayoresReceptores(10),
                        "Peso entrante de cada cliente en el grafo de transferencias; 'veces' = de cuantos clientes distintos recibe.");
                case 3 -> ciclos(analisis.ciclosTransferencias());
                case 4 -> conexion(analisis.conexion(actual.getId(), Formato.idDe(otroCliente.getSelectedItem())));
                case 5 -> tabla(analisis.flujoMonederos(actual.getId()),
                        "Aristas del grafo de monederos que salen o llegan a los monederos de " + actual.getNombre() + ".");
                default -> {
                }
            }
        });
    }

    private int seleccionado() {
        int i = reporte.getSelectedIndex();
        return i < 0 ? -1 : disponibles[i];
    }

    private void tabla(Coleccion<FilaReporte> filas, String detalle) {
        Coleccion<Object[]> datos = ModeloTabla.nuevasFilas();
        for (FilaReporte f : filas) {
            datos.agregar(new Object[]{f.etiqueta(), Formato.pesos(f.valor()), f.veces()});
        }
        modelo.setFilas(datos);
        explicacion.setText(filas.estaVacia() ? "Sin datos para este reporte." : detalle);
    }

    private void ciclos(Coleccion<Coleccion<String>> ciclos) {
        Coleccion<Object[]> datos = ModeloTabla.nuevasFilas();
        for (Coleccion<String> ciclo : ciclos) {
            datos.agregar(new Object[]{nombres(ciclo), "-", ciclo.tamanio() - 1});
        }
        modelo.setFilas(datos);
        explicacion.setText(ciclos.estaVacia() ? "No hay ciclos: el dinero no vuelve a quien lo envio."
                : "El dinero dio la vuelta y volvio al cliente inicial ('veces' = cantidad de transferencias del ciclo). "
                + "Se resalta en rojo el primero.");
        if (!ciclos.estaVacia()) {
            dibujo.resaltar(ciclos.obtener(0));
        }
    }

    private void conexion(Coleccion<String> camino) {
        Coleccion<Object[]> datos = ModeloTabla.nuevasFilas();
        if (!camino.estaVacia()) {
            datos.agregar(new Object[]{nombres(camino), "-", camino.tamanio() - 1});
        }
        modelo.setFilas(datos);
        explicacion.setText(camino.estaVacia() ? "No hay una cadena de transferencias entre esos clientes."
                : "Camino con menos intermediarios (BFS), resaltado en rojo; 'veces' = cantidad de pasos.");
        dibujo.resaltar(camino);
    }

    private String nombres(Iterable<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            sb.append(sb.length() == 0 ? "" : " -> ").append(contexto.sistema().buscarCliente(id).getNombre());
        }
        return sb.toString();
    }

    @Override
    public void refrescar() {
        Object antes = otroCliente.getSelectedItem();
        otroCliente.removeAllItems();
        Cliente actual = contexto.cliente();
        for (Cliente c : contexto.clientesOrdenados()) {
            if (actual == null || !c.getId().equals(actual.getId())) {
                otroCliente.addItem(Contexto.etiqueta(c));
            }
        }
        if (antes != null) {
            otroCliente.setSelectedItem(antes);
        }
        modelo.setFilas(ModeloTabla.nuevasFilas());
        explicacion.setText("Elija un reporte y pulse Generar.");
        dibujo.mostrar(new AnalisisGastos(contexto.sistema()).grafoTransferencias(),
                id -> contexto.sistema().buscarCliente(id).getNombre());
    }
}
