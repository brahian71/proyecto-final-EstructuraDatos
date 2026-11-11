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

final class PanelResumen extends JPanel implements Refrescable {

    private final Contexto contexto;
    private final JLabel nombre = Estilo.titulo("");
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

        JPanel cabecera = new JPanel(new BorderLayout(0, 10));
        cabecera.setOpaque(false);
        cabecera.add(nombre, BorderLayout.NORTH);
        JPanel datos = new JPanel(new GridLayout(1, 4, 12, 0));
        datos.setOpaque(false);
        datos.add(cifra("Saldo total", saldo));
        datos.add(cifra("Puntos acumulados", puntos));
        datos.add(cifra("Disponibles para canje", disponibles));
        datos.add(cifra("Rango", rango));
        cabecera.add(datos, BorderLayout.CENTER);
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBackground(Estilo.TARJETA);
        barra.setBorder(Estilo.tarjetaSinTitulo());
        progreso.setStringPainted(true);
        progreso.setFont(Estilo.fuente(Font.BOLD, 12));
        progreso.setPreferredSize(new Dimension(100, 22));
        barra.add(progreso, BorderLayout.CENTER);
        cabecera.add(barra, BorderLayout.SOUTH);
        add(cabecera, BorderLayout.NORTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 10, 0));
        centro.add(Estilo.comoTarjeta(new JScrollPane(monederos.crearTabla()), "Monederos (multimonedero)"));

        JPanel avisos = new JPanel(new BorderLayout(0, 6));
        Estilo.comoTarjeta(avisos, "Notificaciones (lista circular, ultimas 10)");
        var tablaAvisos = notificaciones.crearTabla();
        tablaAvisos.getColumnModel().getColumn(0).setMaxWidth(34);
        tablaAvisos.getColumnModel().getColumn(0).setMinWidth(34);
        tablaAvisos.getColumnModel().getColumn(1).setPreferredWidth(95);
        tablaAvisos.getColumnModel().getColumn(2).setPreferredWidth(150);
        tablaAvisos.getColumnModel().getColumn(3).setPreferredWidth(420);
        avisos.add(new JScrollPane(tablaAvisos), BorderLayout.CENTER);
        JButton leidas = Estilo.botonSecundario("Marcar como leidas");
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
        add(Estilo.comoTarjeta(panelBeneficios, "Beneficios canjeados"), BorderLayout.SOUTH);
    }

    private static JPanel cifra(String titulo, JLabel valor) {
        JPanel tarjeta = new JPanel(new BorderLayout(0, 4));
        tarjeta.setBackground(Estilo.TARJETA);
        tarjeta.setBorder(Estilo.tarjetaSinTitulo());
        JLabel etiqueta = new JLabel(titulo);
        etiqueta.setFont(Estilo.fuente(Font.PLAIN, 12));
        etiqueta.setForeground(Estilo.TEXTO_SUAVE);
        valor.setFont(Estilo.fuente(Font.BOLD, 22));
        valor.setForeground(Estilo.TEXTO);
        tarjeta.add(etiqueta, BorderLayout.NORTH);
        tarjeta.add(valor, BorderLayout.CENTER);
        return tarjeta;
    }

    @Override
    public void refrescar() {
        Cliente c = contexto.cliente();
        if (c == null) {
            return;
        }
        Rango r = c.getRango();
        nombre.setText(c.getNombre() + "  (" + c.getId() + ")");
        rango.setText(r.toString());
        rango.setForeground(Formato.colorRango(r));
        saldo.setText(Formato.pesos(c.saldoTotal()));
        puntos.setText(String.valueOf(c.getPuntos()));
        disponibles.setText(String.valueOf(c.getPuntosDisponibles()));
        actualizarProgreso(c, r);

        Coleccion<Object[]> filas = ModeloTabla.nuevasFilas();
        for (Monedero m : c.getMonederos()) {
            filas.agregar(new Object[]{m.getId(), m.getNombre(), m.getTipo(), Formato.pesos(m.getSaldo()),
                    m.cantidadTransacciones()});
        }
        monederos.setFilas(filas);

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
