package monedero.servicio;

record ClavePuntos(int puntos, String clienteId) implements Comparable<ClavePuntos> {

    private static final String ID_MAXIMO = "\uffff";

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
