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
    crecer). Se avisa: saldo bajo (< 500 unidades = $50.000 despues de un retiro o envio),
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

24. **Cola de prioridad = monticulo binario de minimos** en arreglo, ordenado
    por fecha de proxima ejecucion; en empate sale la programada antes
    (secuencia). insertar/extraer O(log n). El proyecto de referencia usaba
    una lista ordenada (insertar O(n)).
25. **Simulacion por eventos**: al avanzar el reloj se saca la programada mas
    proxima, el reloj salta a su fecha exacta, se ejecuta y, si es
    recurrente, vuelve a la cola con su siguiente fecha. Cada transaccion queda
    con la fecha en que debia ocurrir y el historial sigue en orden.
26. **Fechas recurrentes desde la fecha inicial** (ejecucion n = inicio + n
    periodos), no sumando a la anterior: una mensual del 31 no se corre al 28
    para siempre.
27. **Si una programada falla** (saldo insuficiente) se avisa al cliente, se
    cuenta el fallo y se sigue con la siguiente fecha; no se reintenta.
28. **Recordatorio 24 horas antes** de cada ejecucion (una vez por fecha),
    tambien dentro de un salto largo del reloj.
29. **Lo ejecutado por una programada es una transaccion normal**: da puntos,
    paga comision y se puede revertir.

30. **Grafo con lista de adyacencia** (cada vertice guarda una tabla hash
    destino -> arista): sumar peso a una arista existente es O(1) y la memoria
    es O(V + E). Cada arista acumula dinero (peso) y cantidad de transacciones
    (veces).
31. **Los grafos se arman al pedir el reporte** desde el registro global de
    transacciones (O(T)), sin guardarlos aparte: asi nunca quedan
    desactualizados y las revertidas se excluyen solas.
32. **Grafo de categorias = secuencias de gasto**: X -> Y si despues de gastar
    en X el mismo cliente gasto en Y. Nunca une gastos de clientes distintos.
    Gasto = retiro o transferencia a otro cliente (los movimientos entre
    monederos propios no son gasto).
33. **Ciclos con DFS de tres estados** (sin visitar, en pila, terminado): cada
    arista hacia un vertice "en pila" cierra un ciclo. Encuentra al menos uno si
    existe; no enumera todos (pueden ser exponenciales).
34. **Conexion entre clientes con BFS** (cola FIFO propia): camino con menos
    intermediarios.
35. **Rankings de los reportes con el monticulo propio** (k log k).

36. **Interfaz en Swing con una pestania por funcionalidad** (resumen,
    operaciones, historial, puntos y canje, programadas, reportes y grafos,
    clientes). La interfaz no tiene logica: solo lee y llama a
    `SistemaMonedero`. Despues de cada operacion se refrescan todas las vistas
    (`Contexto.refrescarTodo`).
37. **Antes de confirmar se muestra la comision y los puntos** que daria la
    operacion (`SistemaMonedero.cotizarComision`).
38. **Las tablas usan un modelo propio** (`ModeloTabla`) que se llena desde
    nuestras estructuras; los clientes y monederos se muestran ordenados por id
    con un `ArbolAVL` (la tabla hash no guarda orden). No con un `ArbolBST`
    simple: la tabla entrega los ids casi en orden y el BST se degeneraba
    (altura 461 con 2000 clientes, contra 12 del AVL).
39. **El grafo de transferencias se dibuja** (vertices en circulo, grosor segun
    el dinero) y los reportes de ciclos (DFS) y conexion (BFS) resaltan en
    rojo lo que encontraron.
40. **El menu de consola se conserva** (`Main --consola`) para pruebas rapidas.

41. **Escala de puntos en pesos colombianos (opcion C)**: el enunciado da los
    puntos "por cada 100 unidades de moneda" sin decir cuanto vale una unidad.
    Se fija unidad = $100, asi los puntos se ganan por cada $10.000 completos
    (un salario de $2.000.000 da 200 puntos y se llega a Platino con meses de
    uso, no con el primer deposito). La regla 1/2/3 no cambia.
42. **Lo que el enunciado expresa en unidades se convierte con la misma
    escala**: el bono de 50 unidades = $5.000 y el umbral de saldo bajo de 500
    unidades = $50.000. Las comisiones ($500 minimo, $2.000 por retiro) ya se
    definieron en pesos y no dependen de la escala.
43. **La escala es configurable** (`CalculadoraPuntos`): el programa usa
    `PESOS_COLOMBIANOS`; las pruebas usan `UNIDAD_UN_PESO` (unidad = $1) para
    trabajar con numeros simples, igual que usan `SIN_COMISION`.
44. **Datos de demostracion en pesos reales** (salarios, arriendo, mercado),
    elegidos para que haya clientes en Bronce, Plata y Oro.

45. **Bono de puntos por rango** ("bonos de puntos adicionales por ciertas
    transacciones" del enunciado): Plata +10 %, Oro +20 %, Platino +30 % sobre
    los puntos base, solo en retiros y transferencias (premia usar el monedero,
    no solo guardar). Se usa el rango que el cliente tenia antes de esa
    transaccion. La transaccion guarda cuanto fue bono; al revertir se quita
    todo. La integridad comprueba que puntos - bono == regla base y que el bono
    no supere el del rango mas alto.

46. **Monto maximo por transaccion: $1.000.000.000.** Es un limite razonable
    y evita que los puntos (int) se desborden. Tambien aplica a las programadas.
47. **Una programada que falla por cualquier motivo cuenta como ejecucion
    fallida** y avisa con una notificacion `FALLO_PROGRAMADA`; nunca se pierde
    de la cola. La integridad comprueba que la cola tenga exactamente las
    programadas activas.
48. **No se puede programar una transferencia al mismo monedero** (todas sus
    ejecuciones fallarian).
49. **Los montos en la interfaz son pesos enteros**: se aceptan "12500" y
    "12.500" (puntos de miles); se rechazan decimales como "12.5" o "12,50"
    para que no se lean como otro numero.

50. **Inicio de sesion con dos roles.** ADMINISTRADOR: ve a todos, registra
    clientes, avanza el reloj y verifica la integridad. CLIENTE: solo ve y
    opera sus monederos; no ve saldos ajenos, el grafo de todos ni las
    programadas de otros. El ranking de puntos si es visible para todos (es
    parte del juego de rangos: cada cliente ve su posicion frente a los demas).
    En el historial de un monedero, la comision y los puntos de una
    transferencia recibida de otro cliente se muestran como "-" (son del que
    la envio).
51. **Contrasenas con hash SHA-256 y sal aleatoria por usuario**; nunca se
    guardan en texto plano. La comparacion es en tiempo constante y el
    mensaje de error es el mismo si falla el usuario o la contrasena (no
    revela que usuarios existen).
52. **Usuarios en una TablaHash por nombre de usuario** (en minusculas):
    iniciar sesion es O(1) promedio.
53. **Al registrar un cliente con usuario, primero se valida el usuario**;
    si es invalido no queda un cliente creado sin usuario.
54. **Los permisos se aplican en la interfaz** (que pestanias, datos y
    botones ve cada rol). Los servicios no los revisan, porque tambien los
    usan las pruebas y el menu de consola de desarrollo.

## Respondidas (2026-10-28)

- **Todas las estructuras deben ser propias.** Hay que reemplazar `MapaJava`
  (temporal sobre `HashMap`) por una `TablaHash` propia.
- **Interfaz grafica** (no consola), hecha con **Swing** (elegido el 2026-10-30:
  viene con el JDK, no requiere configurar librerias). La consola queda para
  desarrollo.
- **Trabajo individual.** Entrega estimada: mediados de noviembre de 2026
  (sin fecha oficial).
- **Pesos colombianos.** Escala de puntos con unidad = $100 (opcion C, elegida
  el 2026-11-03); ver decisiones 41 a 44.

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
- Cuantas notificaciones guardar por cliente (hoy 10).
