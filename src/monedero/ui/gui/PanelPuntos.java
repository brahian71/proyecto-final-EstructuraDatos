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

final class PanelPuntos extends JPanel implements Refrescable {

    private final Contexto contexto;
    private final JLabel disponibles = Estilo.titulo("");
    private final JButton[] botonesCanje = new JButton[Beneficio.values().length];
    private final ModeloTabla historial = new ModeloTabla("Fecha", "Movimiento", "Puntos", "Detalle", "Disponibles");
    private final ModeloTabla ranking = new ModeloTabla("#", "Cliente", "Puntos", "Rango");
    private final ModeloTabla porRango = new ModeloTabla("Rango", "Puntos", "Desc. transf.", "Bono puntos", "Clientes");
    private final JLabel datosArbol = new JLabel(" ");

    PanelPuntos(Contexto contexto) {
        super(new GridLayout(1, 2, 10, 0));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel izquierda = new JPanel(new BorderLayout(0, 10));
        JPanel canje = new JPanel(new BorderLayout(0, 8));
        Estilo.comoTarjeta(canje, "Canjear puntos disponibles");
        canje.add(disponibles, BorderLayout.NORTH);
        JPanel botones = new JPanel(new GridLayout(Beneficio.values().length, 1, 0, 6));
        for (Beneficio b : Beneficio.values()) {
            String detalle = b == Beneficio.BONO_SALDO
                    ? " (" + Formato.pesos(contexto.sistema().montoBono()) + ")" : "";
            JButton boton = Estilo.botonSecundario("<html><b>" + b.getCostoPuntos() + " pts</b> &nbsp; "
                    + b.getDescripcion() + detalle + "</html>");
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
        izquierda.add(Estilo.comoTarjeta(new JScrollPane(historial.crearTabla()),
                "Historial de puntos (lista enlazada)"), BorderLayout.CENTER);
        add(izquierda);

        JPanel derecha = new JPanel(new BorderLayout(0, 10));
        derecha.add(Estilo.comoTarjeta(new JScrollPane(ranking.crearTabla()),
                "Ranking: top 10 (recorrido inorden invertido del AVL)"), BorderLayout.CENTER);
        JPanel abajo = new JPanel(new BorderLayout(0, 4));
        JScrollPane tablaRangos = new JScrollPane(porRango.crearTabla());
        tablaRangos.setPreferredSize(new java.awt.Dimension(100, 172));
        abajo.add(Estilo.comoTarjeta(tablaRangos, "Clientes por rango (consulta de rango en el AVL)"), BorderLayout.CENTER);
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
            disponibles.setText(c.getPuntosDisponibles() + " puntos disponibles  ·  " + c.getPuntos()
                    + " acumulados  ·  " + c.getRango());
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
            filasRango.agregar(new Object[]{r, limites, r.getDescuentoTransferencia() + " %",
                    "+" + r.getBonoPuntos() + " %", nombres.toString()});
        }
        porRango.setFilas(filasRango);

        RegistroPuntos registro = contexto.sistema().getRegistroPuntos();
        datosArbol.setText("Arbol AVL de clientes: " + registro.tamanio() + " nodos, altura " + registro.altura()
                + ", " + registro.rotaciones() + " rotaciones hechas, " + (registro.arbolValido() ? "balanceado" : "INVALIDO"));
    }
}
