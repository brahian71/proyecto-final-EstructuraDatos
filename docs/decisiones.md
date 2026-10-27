# Decisiones de diseno

Sirven de base para el informe tecnico.

## Tomadas

1. **Dinero en `long`** (unidades enteras). Con `double` los redondeos harian
   fallar la verificacion de integridad.
2. **Puntos por centenas completas** (division entera): depositar 250 da 2
   puntos. Los puntos se calculan al crear la transaccion y se guardan en ella,
   para que al revertir se descuenten exactamente los mismos.
3. **Transferir entre monederos propios = `MOVIMIENTO_INTERNO`, sin puntos.**
   Si diera puntos, mover la misma plata de un lado a otro sumaria puntos sin
   limite.
4. **En una transferencia, los puntos los gana quien envia.**
5. **Una transferencia es un solo objeto** que queda en el historial de los dos
   monederos; `efectoSobre(monedero)` dice si resta o suma.
6. **El saldo solo cambia con `Monedero.aplicar`**, que tambien guarda la
   transaccion en el historial. Asi el saldo siempre es la suma del historial,
   que es lo que comprueba la integridad.
7. **Integridad por divide y venceras** sobre el historial pasado a arreglo:
   O(n) y profundidad de recursion O(log n). Comprueba transacciones validas,
   orden cronologico, saldo == suma del historial, saldo nunca negativo
   (minimo de sumas parciales) y puntos del cliente == suma de sus
   transacciones. Hay una prueba con 50.000 transacciones que no desborda la
   pila.
8. **Reloj simulado**: el sistema pide la hora al `Reloj`, no a
   `LocalDateTime.now()`. Las programadas se procesaran con "avanzar dias".
9. **Rangos**: `Rango.desdePuntos` calcula el rango de un cliente en O(1). El
   AVL sirve para mantener a todos los clientes ordenados y responder consultas
   por rango.
10. **Estructuras detras de interfaces** (`Coleccion`, `Mapa`, ...). Las que
    todavia no se han visto en clase se usan con un adaptador temporal.
11. **Sin persistencia**: los datos de ejemplo se cargan al iniciar.

## Pendientes (preguntas al profesor)

- Estructuras propias o de `java.util`. La arquitectura soporta ambas.
- Interfaz de consola o grafica. Por ahora es de consola.
- Trabajo individual o en grupo, y fecha de entrega.
- Valor de las comisiones. Hoy son 0 en `SistemaMonedero.calcularComision`;
  el modelo ya las soporta.
- Que reportes se esperan de los grafos.
- Que debe verificar la integridad, ademas de lo listado arriba.
- Que guarda el BST de puntos: clientes por puntos, o historial de puntos.
- Unidad de moneda: con pesos reales, 1 punto por cada 100 deja a todos en
  Platino muy rapido.
