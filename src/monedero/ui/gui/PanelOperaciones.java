package monedero.ui.gui;

import monedero.modelo.Categoria;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.TipoTransaccion;
import monedero.modelo.Transaccion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

/**
 * Deposito, retiro y transferencia. Antes de confirmar se muestra la
 * comision y los puntos que se ganarian (cotizacion con el rango y los
 * beneficios de hoy). Abajo, revertir la ultima transaccion (pila).
 */
final class PanelOperaciones extends JPanel implements Refrescable {

    private final Contexto contexto;

    private final JComboBox<String> depositoDestino = new JComboBox<>();
    private final JTextField depositoMonto = new JTextField(10);
    private final JTextField depositoDescripcion = new JTextField(14);
    private final JLabel depositoCotizacion = new JLabel(" ");

    private final JComboBox<String> retiroOrigen = new JComboBox<>();
    private final JTextField retiroMonto = new JTextField(10);
    private final JComboBox<Categoria> retiroCategoria = new JComboBox<>(Categoria.values());
    private final JTextField retiroDescripcion = new JTextField(14);
    private final JLabel retiroCotizacion = new JLabel(" ");

    private final JComboBox<String> transferenciaOrigen = new JComboBox<>();
    private final JComboBox<String> transferenciaDestino = new JComboBox<>();
    private final JTextField transferenciaMonto = new JTextField(10);
    private final JComboBox<Categoria> transferenciaCategoria = new JComboBox<>(Categoria.values());
    private final JTextField transferenciaDescripcion = new JTextField(14);
    private final JLabel transferenciaCotizacion = new JLabel(" ");

    private final JLabel proximaReversible = new JLabel(" ");
    private final JButton botonRevertir = new JButton("Revertir ultima transaccion");

    PanelOperaciones(Contexto contexto) {
        super(new BorderLayout(10, 10));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formularios = new JPanel(new GridLayout(1, 3, 10, 0));
        formularios.add(formulario("Deposito", new String[]{"Monedero", "Monto", "Descripcion"},
                new java.awt.Component[]{depositoDestino, depositoMonto, depositoDescripcion},
                depositoCotizacion, "Depositar", this::depositar));
        formularios.add(formulario("Retiro", new String[]{"Monedero", "Monto", "Categoria", "Descripcion"},
                new java.awt.Component[]{retiroOrigen, retiroMonto, retiroCategoria, retiroDescripcion},
                retiroCotizacion, "Retirar", this::retirar));
        formularios.add(formulario("Transferencia", new String[]{"Desde", "Hacia", "Monto", "Categoria", "Descripcion"},
                new java.awt.Component[]{transferenciaOrigen, transferenciaDestino, transferenciaMonto,
                        transferenciaCategoria, transferenciaDescripcion},
                transferenciaCotizacion, "Transferir", this::transferir));
        add(formularios, BorderLayout.CENTER);

        JPanel reversion = new JPanel(new BorderLayout(10, 0));
        reversion.setBorder(Contexto.tarjeta("Revertir (pila de transacciones del cliente: se deshace la mas reciente)"));
        reversion.add(proximaReversible, BorderLayout.CENTER);
        botonRevertir.addActionListener(e -> revertir());
        reversion.add(botonRevertir, BorderLayout.EAST);
        add(reversion, BorderLayout.SOUTH);

        alCambiar(depositoMonto, this::cotizar);
        alCambiar(retiroMonto, this::cotizar);
        alCambiar(transferenciaMonto, this::cotizar);
        retiroOrigen.addActionListener(e -> cotizar());
        transferenciaOrigen.addActionListener(e -> cotizar());
        transferenciaDestino.addActionListener(e -> cotizar());
    }

    private JPanel formulario(String titulo, String[] etiquetas, java.awt.Component[] campos, JLabel cotizacion,
                              String textoBoton, Runnable accion) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(Contexto.tarjeta(titulo));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < etiquetas.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            g.weightx = 0;
            panel.add(new JLabel(etiquetas[i] + ":"), g);
            g.gridx = 1;
            g.weightx = 1;
            panel.add(campos[i], g);
        }
        g.gridx = 0;
        g.gridy = etiquetas.length;
        g.gridwidth = 2;
        cotizacion.setForeground(Formato.PRIMARIO);
        panel.add(cotizacion, g);
        g.gridy++;
        JButton boton = new JButton(textoBoton);
        boton.addActionListener(e -> accion.run());
        panel.add(boton, g);
        g.gridy++;
        g.weighty = 1;
        panel.add(new JLabel(), g); // empuja el contenido hacia arriba
        return panel;
    }

    // ------------------------------------------------------------------ acciones

    private void depositar() {
        contexto.ejecutar(null, () -> {
            Transaccion t = contexto.sistema().depositar(Formato.idDe(depositoDestino.getSelectedItem()),
                    Formato.leerMonto(depositoMonto.getText()), depositoDescripcion.getText());
            limpiar(depositoMonto, depositoDescripcion);
            confirmarHecho(t);
        });
    }

    private void retirar() {
        contexto.ejecutar(null, () -> {
            Transaccion t = contexto.sistema().retirar(Formato.idDe(retiroOrigen.getSelectedItem()),
                    Formato.leerMonto(retiroMonto.getText()), (Categoria) retiroCategoria.getSelectedItem(),
                    retiroDescripcion.getText());
            limpiar(retiroMonto, retiroDescripcion);
            confirmarHecho(t);
        });
    }

    private void transferir() {
        contexto.ejecutar(null, () -> {
            Transaccion t = contexto.sistema().transferir(Formato.idDe(transferenciaOrigen.getSelectedItem()),
                    Formato.idDe(transferenciaDestino.getSelectedItem()), Formato.leerMonto(transferenciaMonto.getText()),
                    (Categoria) transferenciaCategoria.getSelectedItem(), transferenciaDescripcion.getText());
            limpiar(transferenciaMonto, transferenciaDescripcion);
            confirmarHecho(t);
        });
    }

    private void revertir() {
        Cliente c = contexto.cliente();
        Transaccion t = c == null ? null : contexto.sistema().proximaReversible(c.getId());
        if (t == null) {
            return;
        }
        if (contexto.confirmar("Se revertira " + t.getId() + " (" + t.getTipo() + " " + Formato.pesos(t.getMonto())
                + ") y se descontaran " + t.getPuntos() + " puntos. Continuar?")) {
            contexto.ejecutar("Transaccion " + t.getId() + " revertida.", () -> contexto.sistema().revertirUltima(c.getId()));
        }
    }

    private void confirmarHecho(Transaccion t) {
        contexto.informar("Listo", t.getTipo() + " " + t.getId() + " por " + Formato.pesos(t.getMonto())
                + (t.getComision() > 0 ? "\nComision: " + Formato.pesos(t.getComision()) : "")
                + "\nPuntos ganados: " + t.getPuntos());
    }

    // ------------------------------------------------------------------ cotizacion

    private void cotizar() {
        depositoCotizacion.setText(textoCotizacion(TipoTransaccion.DEPOSITO, depositoMonto, null));
        retiroCotizacion.setText(textoCotizacion(TipoTransaccion.RETIRO, retiroMonto, retiroOrigen));
        TipoTransaccion tipo = esMovimientoInterno() ? TipoTransaccion.MOVIMIENTO_INTERNO : TipoTransaccion.TRANSFERENCIA;
        transferenciaCotizacion.setText(textoCotizacion(tipo, transferenciaMonto, transferenciaOrigen)
                + (tipo == TipoTransaccion.MOVIMIENTO_INTERNO ? " (entre tus monederos)" : ""));
    }

    private String textoCotizacion(TipoTransaccion tipo, JTextField campoMonto, JComboBox<String> origen) {
        try {
            long monto = Formato.leerMonto(campoMonto.getText());
            long comision = origen == null || origen.getSelectedItem() == null ? 0
                    : contexto.sistema().cotizarComision(tipo, monto, Formato.idDe(origen.getSelectedItem()));
            return "Comision: " + Formato.pesos(comision) + "  |  Puntos: " + contexto.sistema().getCalculadoraPuntos().calcular(tipo, monto);
        } catch (RuntimeException e) {
            return " ";
        }
    }

    private boolean esMovimientoInterno() {
        if (transferenciaOrigen.getSelectedItem() == null || transferenciaDestino.getSelectedItem() == null) {
            return false;
        }
        Monedero o = contexto.sistema().buscarMonedero(Formato.idDe(transferenciaOrigen.getSelectedItem()));
        Monedero d = contexto.sistema().buscarMonedero(Formato.idDe(transferenciaDestino.getSelectedItem()));
        return o.getClienteId().equals(d.getClienteId());
    }

    @Override
    public void refrescar() {
        Cliente c = contexto.cliente();
        contexto.llenarMonederos(depositoDestino, false);
        contexto.llenarMonederos(retiroOrigen, false);
        contexto.llenarMonederos(transferenciaOrigen, false);
        contexto.llenarMonederos(transferenciaDestino, true);
        if (esMovimientoInterno() && transferenciaOrigen.getSelectedItem().equals(transferenciaDestino.getSelectedItem())) {
            // por defecto, el primer monedero de otro cliente
            for (int i = 0; i < transferenciaDestino.getItemCount(); i++) {
                Monedero m = contexto.sistema().buscarMonedero(Formato.idDe(transferenciaDestino.getItemAt(i)));
                if (c != null && !m.getClienteId().equals(c.getId())) {
                    transferenciaDestino.setSelectedIndex(i);
                    break;
                }
            }
        }
        Transaccion t = c == null ? null : contexto.sistema().proximaReversible(c.getId());
        proximaReversible.setText(t == null ? "No hay transacciones para revertir"
                : "Proxima a revertir: " + t.getId() + "  " + t.getTipo() + "  " + Formato.pesos(t.getMonto())
                + "  (" + Formato.fecha(t.getFecha()) + ", " + t.getPuntos() + " pts)");
        botonRevertir.setEnabled(t != null);
        cotizar();
    }

    private static void limpiar(JTextField... campos) {
        for (JTextField campo : campos) {
            campo.setText("");
        }
    }

    private static void alCambiar(JTextField campo, Runnable accion) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                accion.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                accion.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                accion.run();
            }
        });
    }
}
