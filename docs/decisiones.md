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
12. **Una pila de reversibles por cliente** (no una global). Guarda las
    transacciones que inicio el cliente; solo se revierte la cima (LIFO), asi
    nunca se deshace algo de lo que dependen operaciones suyas posteriores. Con
    una pila global, un cliente no podria revertir porque otro opero despues.
13. **Revertir no borra**: la transaccion queda en el historial marcada como
    REVERTIDA y cuenta 0 en saldo y puntos. Se conserva la evidencia y la
    integridad sigue cuadrando.
14. **No se revierte plata ya gastada**: si el monedero que recibio el dinero
    lo uso despues (el saldo minimo desde esa transaccion es menor que el
    monto), la reversion se rechaza. Si no, el historial quedaria con un saldo
    negativo en algun momento.
15. **Al revertir se descuentan los mismos puntos que se dieron** (guardados en
    la transaccion) y se avisa si el cliente cambia de rango.
16. **Notificaciones en lista circular de capacidad 10 por cliente**. Cuando
    se llena, la nueva sobrescribe la mas vieja reutilizando el nodo (O(1), sin
    crecer). Se avisa: saldo bajo (< 1.000 despues de un retiro o envio),
    transferencia recibida, cambio de rango y reversion. No se avisa cada
    transaccion para no llenar el buzon de ruido.

17. **Dos cuentas de puntos**: acumulados (todo lo ganado menos lo revertido,
    define el rango) y disponibles (acumulados menos lo canjeado, es lo que se
    gasta). Canjear no baja de rango; revertir si, porque esos puntos nunca
    debieron existir. No se puede revertir si los puntos ya se canjearon.
18. **Historial de puntos por cliente** en lista enlazada (ganados, revertidos,
    canjeados). Su suma debe dar los disponibles; lo revisa la integridad.
19. **AVL de clientes con clave (puntos, id)**: el id desempata y hace la clave
    unica. Cuando cambian los puntos se saca y se reinserta (O(log n)). Las
    consultas de rango usan claves centinela (id "" y id maximo).
20. **La comision se calcula con el rango y los beneficios del momento**, antes
    de sumar los puntos de esa misma transaccion. La paga quien envia o retira;
    el destino recibe el monto completo. Al revertir se devuelve tambien.
21. **Politica de comision inyectable** (`PoliticaComision`): el sistema usa
    `ComisionEstandar`; las pruebas de las fases 1 y 2 usan `SIN_COMISION` para
    comprobar saldos exactos sin depender de las tarifas.
22. **Beneficios del canje**: el descuento de transferencias y los retiros sin
    cargo duran 30 dias del reloj simulado; el bono se deposita en el monedero
    principal como transaccion `BONO` (no da puntos y no se puede revertir).
23. **Integridad ampliada**: puntos disponibles == suma del historial de puntos,
    cada cliente esta en el AVL con su clave actual, y el AVL esta ordenado,
    con alturas correctas y balanceado (verificacion recursiva).

## Respondidas (2026-10-28)

- **Todas las estructuras deben ser propias.** Hay que reemplazar `MapaJava`
  (temporal sobre `HashMap`) por una `TablaHash` propia.
- **Interfaz grafica** (no consola). La consola queda para desarrollo.
- **Trabajo individual.** Entrega estimada: mediados de noviembre de 2026
  (sin fecha oficial).
- **Pesos colombianos.** Ajustar montos de ejemplo, umbral de saldo bajo y
  escala de puntos cuando sea pertinente.

- **Comisiones (opcion C)**: transferencia 1 % con minimo $500; retiro
  tarifa fija $2.000; depositos y movimientos internos gratis. Descuento por
  rango en transferencias: Bronce 0 %, Plata 10 %, Oro 20 %, Platino 30 %; el
  canje de 100 puntos suma 10 % mas; el de 500 deja los retiros en $0 por 30
  dias (reloj simulado).
- **Grafos**: una clase `GrafoDirigido<V>` para tres grafos (transferencias
  entre clientes, flujo entre monederos, secuencias de categorias). Reportes:
  top categorias por cliente, secuencias de gasto frecuentes, mayores
  receptores, ciclos de transferencias (DFS), conexion entre clientes (BFS),
  flujo entre monederos de un cliente.
- **Arboles de puntos (opcion A)**: `ArbolAVL` hereda de `ArbolBST`. El sistema
  usa un AVL de clientes ordenados por puntos (ranking, clientes por rango,
  quien puede canjear). Tabla hash id -> cliente. Historial de puntos de cada
  cliente en lista enlazada. El BST simple se usa para mostrar, con pruebas de
  altura, por que hace falta balancear.

## Pendientes

- Que mas debe verificar la integridad (pregunta enviada al profesor).
- Umbral de saldo bajo (hoy 1.000) y cuantas notificaciones guardar (hoy 10).
- Monto del bono de 1000 puntos: el enunciado dice 50 unidades; en pesos
  colombianos habria que reescalarlo (junto con la escala de puntos).
