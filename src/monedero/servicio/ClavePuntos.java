package monedero.servicio;

/**
 * Clave del arbol de clientes: ordena por puntos y, si empatan, por id.
 * El id hace la clave unica (dos clientes con los mismos puntos no chocan).
 *
 * Para consultar un rango de puntos [a, b] se usan claves centinela:
 * desde(a) tiene id "" (menor que cualquier id) y hasta(b) tiene un id
 * mayor que cualquiera, asi entran todos los clientes con a..b puntos.
 */
record ClavePuntos(int puntos, String clienteId) implements Comparable<ClavePuntos> {

    private static final String ID_MAXIMO = "￿";

    static ClavePuntos desde(int puntos) {
        return new ClavePuntos(puntos, "");
    }

    static ClavePuntos hasta(int puntos) {
        return new ClavePuntos(puntos, ID_MAXIMO);
    }

    @Override
    public int compareTo(ClavePuntos otra) {
        int porPuntos = Integer.compare(puntos, otra.puntos);
        return porPuntos != 0 ? porPuntos : clienteId.compareTo(otra.clienteId);
    }
}
