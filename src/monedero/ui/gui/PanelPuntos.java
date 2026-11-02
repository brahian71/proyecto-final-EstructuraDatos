package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.modelo.Beneficio;
import monedero.modelo.Cliente;
import monedero.modelo.MovimientoPuntos;
import monedero.modelo.Rango;
import monedero.servicio.RegistroPuntos;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Canje de puntos, historial de puntos del cliente y, a la derecha, lo que
 * sale del arbol AVL de clientes: ranking y clientes por rango.
 */
final class PanelPuntos extends JPanel implements Refrescable {

    private final Contexto contexto;
    private final JLabel disponibles = Contexto.titulo("");
    private final JButton[] botonesCanje = new JButton[Beneficio.values().length];
    private final ModeloTabla historial = new ModeloTabla("Fecha", "Movimiento", "Puntos", "Detalle", "Disponibles");
    private final ModeloTabla ranking = new ModeloTabla("#", "Cliente", "Puntos", "Rango");
    private final ModeloTabla porRango = new ModeloTabla("Rango", "Puntos", "Descuento transf.", "Clientes");
    private final JLabel datosArbol = new JLabel(" ");

    PanelPuntos(Contexto contexto) {
        super(new GridLayout(1, 2, 10, 0));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- izquierda: canje e historial
        JPanel izquierda = new JPanel(new BorderLayout(0, 10));
        JPanel canje = new JPanel(new BorderLayout(0, 8));
        canje.setBorder(Contexto.tarjeta("Canjear puntos disponibles"));
        canje.add(disponibles, BorderLayout.NORTH);
        JPanel botones = new JPanel(new GridLayout(Beneficio.values().length, 1, 0, 6));
        for (Beneficio b : Beneficio.values()) {
            JButton boton = new JButton("<html><b>" + b.getCostoPuntos() + " pts</b> &nbsp; "
                    + b.getDescripcion() + "</html>");
            boton.setHorizontalAlignment(SwingConstants.LEFT);
            boton.addActionListener(e -> canjear(b));
            botonesCanje[b.ordinal()] = boton;
            botones.add(boton);
        }
        canje.add(botones, BorderLayout.CENTER);
        JLabel nota = new JLabel("Canjear gasta puntos disponibles pero no baja el rango (se calcula con los acumulados).");
        nota.setFont(nota.getFont().deriveFont(Font.ITALIC, 11f));
        canje.add(nota, BorderLayout.SOUTH);
        izquierda.add(canje, BorderLayout.NORTH);
        izquierda.add(Contexto.conBorde(new JScrollPane(historial.crearTabla()),
                "Historial de puntos (lista enlazada)"), BorderLayout.CENTER);
        add(izquierda);

        // ---- derecha: arbol AVL
        JPanel derecha = new JPanel(new BorderLayout(0, 10));
        derecha.add(Contexto.conBorde(new JScrollPane(ranking.crearTabla()),
                "Ranking: top 10 (recorrido inorden invertido del AVL)"), BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        JScrollPane tablaRangos = new JScrollPane(porRango.crearTabla());
        tablaRangos.setPreferredSize(new java.awt.Dimension(100, 150));
        abajo.add(Contexto.conBorde(tablaRangos, "Clientes por rango (consulta de rango en el AVL)"), BorderLayout.CENTER);
        abajo.add(datosArbol, BorderLayout.SOUTH);
        derecha.add(abajo, BorderLayout.SOUTH);
        add(derecha);
    }

    private void canjear(Beneficio b) {
        Cliente c = contexto.cliente();
        if (c != null && contexto.confirmar("Canjear " + b.getCostoPuntos() + " puntos por: " + b.getDescripcion() + "?")) {
            contexto.ejecutar("Canje realizado. Te quedan " + (c.getPuntosDisponibles() - b.getCostoPuntos())
                    + " puntos disponibles.", () -> contexto.sistema().canjear(c.getId(), b));
        }
    }

    @Override
    public void refrescar() {
        Cliente c = contexto.cliente();
        if (c != null) {
            disponibles.setText(c.getNombre() + ": " + c.getPuntosDisponibles() + " puntos disponibles  ("
                    + c.getPuntos() + " acumulados, rango " + c.getRango() + ")");
            for (Beneficio b : Beneficio.values()) {
                botonesCanje[b.ordinal()].setEnabled(c.getPuntosDisponibles() >= b.getCostoPuntos());
            }
            Coleccion<Object[]> filas = ModeloTabla.nuevasFilas();
            for (MovimientoPuntos m : c.getHistorialPuntos()) {
                filas.agregar(new Object[]{Formato.fecha(m.getFecha()), m.getTipo(),
                        (m.getCantidad() > 0 ? "+" : "") + m.getCantidad(), m.getDetalle(), m.getDisponiblesDespues()});
            }
            historial.setFilas(filas);
        }

        Coleccion<Object[]> filasRanking = ModeloTabla.nuevasFilas();
        int puesto = 1;
        for (Cliente cliente : contexto.sistema().ranking(10)) {
            filasRanking.agregar(new Object[]{puesto++, Contexto.etiqueta(cliente), cliente.getPuntos(), cliente.getRango()});
        }
        ranking.setFilas(filasRanking);

        Coleccion<Object[]> filasRango = ModeloTabla.nuevasFilas();
        for (Rango r : Rango.values()) {
            StringBuilder nombres = new StringBuilder();
            for (Cliente cliente : contexto.sistema().clientesDeRango(r)) {
                nombres.append(nombres.length() == 0 ? "" : ", ").append(cliente.getNombre());
            }
            String limites = r == Rango.PLATINO ? "mas de 5000" : r.getPuntosMinimos() + " - " + r.getPuntosMaximos();
            filasRango.agregar(new Object[]{r, limites, r.getDescuentoTransferencia() + " %", nombres.toString()});
        }
        porRango.setFilas(filasRango);

        RegistroPuntos registro = contexto.sistema().getRegistroPuntos();
        datosArbol.setText("Arbol AVL de clientes: " + registro.tamanio() + " nodos, altura " + registro.altura()
                + ", " + registro.rotaciones() + " rotaciones hechas, " + (registro.arbolValido() ? "balanceado" : "INVALIDO"));
    }
}
