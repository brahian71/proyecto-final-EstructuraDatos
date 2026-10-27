package monedero.modelo;

/**
 * Categoria de gasto. Se asigna a retiros y transferencias; luego sera un
 * vertice del grafo de patrones de gasto.
 */
public enum Categoria {
    ALIMENTACION,
    TRANSPORTE,
    SERVICIOS,
    SALUD,
    EDUCACION,
    ENTRETENIMIENTO,
    AHORRO,
    OTROS
}
