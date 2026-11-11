package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Frecuencia;
import monedero.modelo.OperacionProgramada;
import monedero.modelo.TipoTransaccion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Transacciones programadas: la tabla muestra la cola de prioridad en el
 * orden en que se van a ejecutar (de la mas proxima a la mas lejana).
 */
final class PanelProgramadas extends JPanel implements Refrescable {

    private final Contexto contexto;
    private final ModeloTabla modelo = new ModeloTabla("Id", "Proxima", "Tipo", "Monto", "Desde", "Hacia",
            "Frecuencia", "Restantes", "Fallos", "Descripcion");
    private final JTable tabla = modelo.crearTabla();

    private final JComboBox<TipoTransaccion> tipo = new JComboBox<>(new TipoTransaccion[]{
            TipoTransaccion.DEPOSITO, TipoTransaccion.RETIRO, TipoTransaccion.TRANSFERENCIA});
    private final JComboBox<String> origen = new JComboBox<>();
    private final JComboBox<String> destino = new JComboBox<>();
    private final JTextField monto = new JTextField(10);
    private final JComboBox<Categoria> categoria = new JComboBox<>(Categoria.values());
    private final JComboBox<Frecuencia> frecuencia = new JComboBox<>(Frecuencia.values());
    private final JSpinner enDias = new JSpinner(new SpinnerNumberModel(1, 0, 365, 1));
    private final JSpinner veces = new JSpinner(new SpinnerNumberModel(6, 0, 999, 1));
    private final JTextField descripcion = new JTextField(14);

    PanelProgramadas(Contexto contexto) {
        super(new BorderLayout(10, 10));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel lista = new JPanel(new BorderLayout(0, 6));
        lista.setBorder(Contexto.tarjeta((contexto.esAdmin() ? "Pendientes de todos los clientes" : "Mis programadas")
                + " (cola de prioridad: monticulo por fecha)"));
        lista.add(new JScrollPane(tabla), BorderLayout.CENTER);
        JButton cancelar = new JButton("Cancelar la seleccionada");
        cancelar.addActionListener(e -> cancelar());
        lista.add(cancelar, BorderLayout.SOUTH);
        add(lista, BorderLayout.CENTER);
        add(formulario(), BorderLayout.WEST);

        tipo.addActionListener(e -> habilitarCampos());
        origen.addActionListener(e -> {
            if (tipo.getSelectedItem() == TipoTransaccion.TRANSFERENCIA) {
                llenarDestino();
            }
        });
        frecuencia.addActionListener(e -> habilitarCampos());
        habilitarCampos();
    }

    private JPanel formulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(Contexto.tarjeta("Programar para el cliente actual"));
        String[] etiquetas = {"Tipo", "Desde", "Hacia", "Monto", "Categoria", "Frecuencia",
                "Primera en (dias)", "Veces (0 = sin limite)", "Descripcion"};
        java.awt.Component[] campos = {tipo, origen, destino, monto, categoria, frecuencia, enDias, veces, descripcion};
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < etiquetas.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            panel.add(new JLabel(etiquetas[i] + ":"), g);
            g.gridx = 1;
            panel.add(campos[i], g);
        }
        JButton programar = new JButton("Programar");
        programar.addActionListener(e -> programar());
        g.gridx = 0;
        g.gridy = etiquetas.length;
        g.gridwidth = 2;
        panel.add(programar, g);
        g.gridy++;
        g.weighty = 1;
        panel.add(new JLabel(), g);
        return panel;
    }

    private void habilitarCampos() {
        TipoTransaccion t = (TipoTransaccion) tipo.getSelectedItem();
        origen.setEnabled(t != TipoTransaccion.DEPOSITO);
        destino.setEnabled(t != TipoTransaccion.RETIRO);
        categoria.setEnabled(t != TipoTransaccion.DEPOSITO);
        veces.setEnabled(frecuencia.getSelectedItem() != Frecuencia.UNA_VEZ);
        llenarDestino();
    }

    /**
     * Un deposito programado entra a un monedero del cliente actual; una
     * transferencia puede ir a cualquier monedero. Si se ofrecieran todos en
     * el deposito, quedaria programado a nombre de otro cliente.
     */
    private void llenarDestino() {
        contexto.llenarMonederos(destino, tipo.getSelectedItem() != TipoTransaccion.DEPOSITO);
        // en una transferencia, que no quede por defecto el mismo monedero de origen
        if (tipo.getSelectedItem() == TipoTransaccion.TRANSFERENCIA && origen.getSelectedItem() != null
                && origen.getSelectedItem().equals(destino.getSelectedItem())) {
            for (int i = 0; i < destino.getItemCount(); i++) {
                if (!destino.getItemAt(i).equals(origen.getSelectedItem())) {
                    destino.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void programar() {
        OperacionProgramada[] hecha = new OperacionProgramada[1];
        boolean bien = contexto.ejecutar(null, () -> {
            TipoTransaccion t = (TipoTransaccion) tipo.getSelectedItem();
            int cuantas = (Integer) veces.getValue();
            hecha[0] = contexto.sistema().getProgramador().programar(t,
                    t == TipoTransaccion.DEPOSITO ? null : Formato.idDe(origen.getSelectedItem()),
                    t == TipoTransaccion.RETIRO ? null : Formato.idDe(destino.getSelectedItem()),
                    Formato.leerMonto(monto.getText()),
                    t == TipoTransaccion.DEPOSITO ? null : (Categoria) categoria.getSelectedItem(),
                    descripcion.getText(), (Frecuencia) frecuencia.getSelectedItem(),
                    contexto.sistema().getReloj().ahora().plusDays((Integer) enDias.getValue()),
                    cuantas == 0 ? OperacionProgramada.SIN_LIMITE : cuantas);
        });
        if (bien) {
            monto.setText("");
            descripcion.setText("");
            contexto.informar("Listo", "Programada " + hecha[0].getId() + ". Primera ejecucion: "
                    + Formato.fecha(hecha[0].getProximaEjecucion()));
        }
    }

    private void cancelar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            contexto.informar("Programadas", "Seleccione una transaccion programada de la tabla.");
            return;
        }
        String id = (String) modelo.valor(tabla.convertRowIndexToModel(fila), 0);
        Cliente actual = contexto.cliente();
        if (actual == null || !contexto.sistema().getProgramador().buscar(id).getClienteId().equals(actual.getId())) {
            contexto.informar("Programadas", "Solo puede cancelar las programadas del cliente actual.");
            return;
        }
        if (contexto.confirmar("Cancelar la transaccion programada " + id + "?")) {
            contexto.ejecutar("Programada " + id + " cancelada.", () -> contexto.sistema().getProgramador().cancelar(id));
        }
    }

    @Override
    public void refrescar() {
        // origen: monederos del cliente actual; destino: cualquiera (en un deposito, uno del cliente)
        contexto.llenarMonederos(origen, false);
        llenarDestino();
        Coleccion<Object[]> filas = ModeloTabla.nuevasFilas();
        for (OperacionProgramada op : contexto.sistema().getProgramador().pendientes()) {
            if (!contexto.esAdmin() && !op.getClienteId().equals(contexto.cliente().getId())) {
                continue; // un cliente solo ve las suyas
            }
            String restantes = op.getEjecucionesTotales() == OperacionProgramada.SIN_LIMITE ? "sin limite"
                    : String.valueOf(op.getEjecucionesTotales() - op.getEjecucionesHechas());
            filas.agregar(new Object[]{op.getId(), Formato.fechaHora(op.getProximaEjecucion()), op.getTipo(),
                    Formato.pesos(op.getMonto()), op.getOrigenId() == null ? "-" : op.getOrigenId(),
                    op.getDestinoId() == null ? "-" : op.getDestinoId(), op.getFrecuencia(), restantes,
                    op.getFallos(), op.getDescripcion()});
        }
        modelo.setFilas(filas);
    }
}
