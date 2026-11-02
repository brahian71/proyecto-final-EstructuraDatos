package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.modelo.Transaccion;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

/** Historial de un monedero (lista enlazada en orden cronologico); las revertidas se ven tachadas en gris. */
final class PanelHistorial extends JPanel implements Refrescable {

    private static final int COLUMNA_ESTADO = 9;

    private final Contexto contexto;
    private final JComboBox<String> monedero = new JComboBox<>();
    private final JLabel resumen = new JLabel(" ");
    private final ModeloTabla modelo = new ModeloTabla("Id", "Fecha", "Tipo", "Monto", "Comision", "Desde",
            "Hacia", "Categoria", "Puntos", "Estado", "Efecto en saldo", "Descripcion");
    private boolean llenando;

    PanelHistorial(Contexto contexto) {
        super(new BorderLayout(10, 10));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        arriba.add(new JLabel("Monedero:"));
        monedero.addActionListener(e -> {
            if (!llenando) {
                llenarTabla();
            }
        });
        arriba.add(monedero);
        arriba.add(resumen);
        add(arriba, BorderLayout.NORTH);

        JTable tabla = modelo.crearTabla();
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                           boolean foco, int fila, int columna) {
                Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
                boolean revertida = "REVERTIDA".equals(modelo.valor(fila, COLUMNA_ESTADO));
                if (!seleccionada) {
                    c.setForeground(revertida ? Formato.APAGADO : java.awt.Color.BLACK);
                }
                return c;
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    @Override
    public void refrescar() {
        llenando = true;
        contexto.llenarMonederos(monedero, false);
        llenando = false;
        llenarTabla();
    }

    private void llenarTabla() {
        Coleccion<Object[]> filas = ModeloTabla.nuevasFilas();
        if (monedero.getSelectedItem() != null) {
            String id = Formato.idDe(monedero.getSelectedItem());
            int revertidas = 0;
            for (Transaccion t : contexto.sistema().historial(id)) {
                long efecto = t.efectoVigenteSobre(id);
                revertidas += t.isRevertida() ? 1 : 0;
                filas.agregar(new Object[]{t.getId(), Formato.fechaHora(t.getFecha()), t.getTipo(),
                        Formato.pesos(t.getMonto()), Formato.pesos(t.getComision()),
                        t.getOrigenId() == null ? "-" : t.getOrigenId(), t.getDestinoId() == null ? "-" : t.getDestinoId(),
                        t.getCategoria(), t.getPuntos(), t.isRevertida() ? "REVERTIDA" : "Vigente",
                        (efecto > 0 ? "+" : "") + Formato.pesos(efecto), t.getDescripcion()});
            }
            resumen.setText("   " + filas.tamanio() + " transacciones, " + revertidas + " revertidas. Saldo: "
                    + Formato.pesos(contexto.sistema().consultarSaldo(id)));
        }
        modelo.setFilas(filas);
    }
}
