package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.modelo.BeneficioActivo;
import monedero.modelo.Cliente;
import monedero.modelo.Monedero;
import monedero.modelo.Notificacion;
import monedero.modelo.Rango;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/** Datos del cliente actual: puntos, rango, monederos, notificaciones y beneficios activos. */
final class PanelResumen extends JPanel implements Refrescable {

    private final Contexto contexto;
    private final JLabel nombre = Contexto.titulo("");
    private final JLabel rango = new JLabel();
    private final JLabel saldo = new JLabel();
    private final JLabel puntos = new JLabel();
    private final JLabel disponibles = new JLabel();
    private final JProgressBar progreso = new JProgressBar();
    private final ModeloTabla monederos = new ModeloTabla("Id", "Nombre", "Tipo", "Saldo", "Transacciones");
    private final ModeloTabla notificaciones = new ModeloTabla("", "Fecha", "Tipo", "Mensaje");
    private final ModeloTabla beneficios = new ModeloTabla("Beneficio", "Vence", "Estado");

    PanelResumen(Contexto contexto) {
        super(new BorderLayout(10, 10));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel cabecera = new JPanel(new BorderLayout(10, 6));
        cabecera.setBorder(Contexto.tarjeta("Cliente"));
        cabecera.add(nombre, BorderLayout.NORTH);
        JPanel datos = new JPanel(new GridLayout(1, 4, 12, 0));
        for (JLabel l : new JLabel[]{rango, saldo, puntos, disponibles}) {
            l.setFont(l.getFont().deriveFont(Font.BOLD, 14f));
            datos.add(l);
        }
        cabecera.add(datos, BorderLayout.CENTER);
        progreso.setStringPainted(true);
        cabecera.add(progreso, BorderLayout.SOUTH);
        add(cabecera, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 10, 0));
        centro.add(Contexto.conBorde(new JScrollPane(monederos.crearTabla()), "Monederos (multimonedero)"));

        JPanel avisos = new JPanel(new BorderLayout(0, 6));
        avisos.setBorder(Contexto.tarjeta("Notificaciones (lista circular, ultimas 10)"));
        var tablaAvisos = notificaciones.crearTabla();
        tablaAvisos.getColumnModel().getColumn(0).setMaxWidth(24);
        tablaAvisos.getColumnModel().getColumn(1).setPreferredWidth(80);
        tablaAvisos.getColumnModel().getColumn(2).setPreferredWidth(150);
        tablaAvisos.getColumnModel().getColumn(3).setPreferredWidth(420);
        avisos.add(new JScrollPane(tablaAvisos), BorderLayout.CENTER);
        JButton leidas = new JButton("Marcar como leidas");
        leidas.addActionListener(e -> {
            Cliente c = contexto.cliente();
            if (c != null) {
                contexto.ejecutar(null, () -> contexto.sistema().getNotificador().marcarLeidas(c.getId()));
            }
        });
        avisos.add(leidas, BorderLayout.SOUTH);
        centro.add(avisos);
        add(centro, BorderLayout.CENTER);

        JScrollPane panelBeneficios = new JScrollPane(beneficios.crearTabla());
        panelBeneficios.setPreferredSize(new Dimension(100, 120));
        add(Contexto.conBorde(panelBeneficios, "Beneficios canjeados"), BorderLayout.SOUTH);
    }

    @Override
    public void refrescar() {
        Cliente c = contexto.cliente();
        if (c == null) {
            return;
        }
        Rango r = c.getRango();
        nombre.setText(c.getNombre() + "  (" + c.getId() + ")");
        rango.setText("Rango: " + r);
        rango.setForeground(Formato.colorRango(r));
        saldo.setText("Saldo total: " + Formato.pesos(c.saldoTotal()));
        puntos.setText("Puntos acumulados: " + c.getPuntos());
        disponibles.setText("Disponibles para canje: " + c.getPuntosDisponibles());
        actualizarProgreso(c, r);

        Coleccion<Object[]> filas = ModeloTabla.nuevasFilas();
        for (Monedero m : c.getMonederos()) {
            filas.agregar(new Object[]{m.getId(), m.getNombre(), m.getTipo(), Formato.pesos(m.getSaldo()),
                    m.cantidadTransacciones()});
        }
        monederos.setFilas(filas);

        // la lista circular va de la mas vieja a la mas nueva; se muestran al reves
        Coleccion<Object[]> avisos = ModeloTabla.nuevasFilas();
        monedero.estructuras.Pila<Object[]> alReves = new monedero.estructuras.PilaEnlazada<>();
        for (Notificacion n : contexto.sistema().getNotificador().buzon(c.getId())) {
            alReves.apilar(new Object[]{n.isLeida() ? "" : "●", Formato.fecha(n.getFecha()), n.getTipo(), n.getMensaje()});
        }
        for (Object[] fila : alReves) {
            avisos.agregar(fila);
        }
        notificaciones.setFilas(avisos);

        Coleccion<Object[]> filasBeneficios = ModeloTabla.nuevasFilas();
        for (BeneficioActivo b : c.getBeneficios()) {
            boolean vigente = b.vigenteEn(contexto.sistema().getReloj().ahora());
            filasBeneficios.agregar(new Object[]{b.getBeneficio().getDescripcion(), Formato.fecha(b.getVence()),
                    vigente ? "Vigente" : "Vencido"});
        }
        beneficios.setFilas(filasBeneficios);
    }

    private void actualizarProgreso(Cliente c, Rango r) {
        if (r == Rango.PLATINO) {
            progreso.setMinimum(0);
            progreso.setMaximum(1);
            progreso.setValue(1);
            progreso.setString("Rango maximo alcanzado");
            return;
        }
        Rango siguiente = Rango.values()[r.ordinal() + 1];
        progreso.setMinimum(r.getPuntosMinimos());
        progreso.setMaximum(siguiente.getPuntosMinimos());
        progreso.setValue(c.getPuntos());
        progreso.setString(c.getPuntos() + " / " + siguiente.getPuntosMinimos() + " pts para " + siguiente
                + " (faltan " + (siguiente.getPuntosMinimos() - c.getPuntos()) + ")");
    }
}
