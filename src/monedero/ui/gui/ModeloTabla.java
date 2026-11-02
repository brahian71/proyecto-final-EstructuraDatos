package monedero.ui.gui;

import monedero.estructuras.Coleccion;
import monedero.estructuras.ListaEnlazada;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

/**
 * Modelo de JTable de solo lectura. Las filas se arman con nuestras propias
 * estructuras (Coleccion) y se copian a un arreglo para que JTable pueda
 * pedir cualquier celda en O(1).
 */
final class ModeloTabla extends AbstractTableModel {

    private final String[] columnas;
    private Object[][] filas = new Object[0][];

    ModeloTabla(String... columnas) {
        this.columnas = columnas;
    }

    /** Para armar las filas antes de llamar a setFilas. */
    static Coleccion<Object[]> nuevasFilas() {
        return new ListaEnlazada<>();
    }

    void setFilas(Coleccion<Object[]> datos) {
        filas = new Object[datos.tamanio()][];
        int i = 0;
        for (Object[] fila : datos) {
            filas[i++] = fila;
        }
        fireTableDataChanged();
    }

    Object valor(int fila, int columna) {
        return filas[fila][columna];
    }

    @Override
    public int getRowCount() {
        return filas.length;
    }

    @Override
    public int getColumnCount() {
        return columnas.length;
    }

    @Override
    public String getColumnName(int columna) {
        return columnas[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        return filas[fila][columna];
    }

    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    JTable crearTabla() {
        JTable tabla = new JTable(this);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(24);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        return tabla;
    }
}
