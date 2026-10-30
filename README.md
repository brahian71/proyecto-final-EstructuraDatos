# Monedero Virtual con Sistema de Puntos

Proyecto Final de Estructuras de Datos. Autor: Brahian Stiven Toro C.
Java 21, IntelliJ IDEA, sin dependencias externas.

## Como correr

En IntelliJ: abrir la carpeta del proyecto, marcar `src` como Sources y `test`
como Test Sources, y ejecutar `monedero.Main` (menu) o `monedero.PruebasFase1` /
`monedero.PruebasFase1` ... `monedero.PruebasFase5` (pruebas).

Por consola (PowerShell):

```
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src, test | ForEach-Object { $_.FullName })
java -cp out monedero.Main
java -cp out monedero.PruebasFase1
java -cp out monedero.PruebasFase2
java -cp out monedero.PruebasFase3
java -cp out monedero.PruebasFase4
java -cp out monedero.PruebasFase5
```

## Arquitectura

La interfaz solo habla con `SistemaMonedero`, y los servicios solo conocen las
interfaces de `estructuras`. Todas las estructuras son propias (no se usan las
colecciones de `java.util`).

```
src/monedero/
  Main.java
  estructuras/   Coleccion, ListaEnlazada, Pila, PilaEnlazada, ListaCircular,
                 Cola, ColaEnlazada, Mapa, TablaHash, ArbolBST, ArbolAVL,
                 ColaPrioridad, MonticuloBinario, GrafoDirigido
  modelo/        Cliente, Monedero, Transaccion, TipoTransaccion, TipoMonedero,
                 Categoria, Rango, Notificacion, TipoNotificacion,
                 Beneficio, BeneficioActivo, MovimientoPuntos,
                 OperacionProgramada, Frecuencia
  servicio/      SistemaMonedero, CalculadoraPuntos, VerificadorIntegridad,
                 ResultadoIntegridad, Notificador, RegistroPuntos (AVL),
                 ClavePuntos, PoliticaComision, ComisionEstandar,
                 ProgramadorTransacciones (monticulo), AnalisisGastos
                 (grafos), FilaReporte, Reloj
  excepciones/   SaldoInsuficienteException, NoEncontradoException,
                 OperacionInvalidaException
  datos/         DatosIniciales (datos de ejemplo, no hay persistencia)
  ui/            MenuConsola
test/monedero/
  PruebasFase1.java   pruebas sin JUnit, imprimen OK / FALLO
  PruebasFase2.java
  PruebasFase3.java
  PruebasFase4.java
  PruebasFase5.java
  modelo/Corruptor    ayuda de pruebas para simular datos corruptos
```

## Fases

Entrega estimada: mediados de noviembre de 2026.

| Fase | Contenido | Estructura | Estado / fecha objetivo |
|---|---|---|---|
| 1 | Depositos, retiros, transferencias, saldo, historial, puntos, rangos, integridad recursiva | Lista enlazada, recursion (divide y venceras) | Hecha (27 oct) |
| 2 | Revertir transacciones (recalculo de puntos), notificaciones | Pila, lista circular | Hecha (27 oct) |
| 3 | Arbol de puntos, rangos, canje, comisiones, tabla hash propia | BST, AVL, tabla hash | Hecha (28 oct) |
| 4 | Transacciones programadas recurrentes, recordatorios | Monticulo (cola de prioridad) | Hecha (30 oct) |
| 5 | Patrones de gasto, transferencias entre clientes, multimonedero | Grafo dirigido (BFS, DFS), cola | Hecha (30 oct) |
| 6 | Interfaz grafica | Swing | 3 - 9 nov |
| 7 | Ajustes a pesos colombianos, umbrales, datos de demostracion | - | 10 nov |
| 8 | Informe tecnico, tabla de requisitos, ensayo de sustentacion | - | 11 - 14 nov |

Las decisiones de diseno y las preguntas abiertas estan en
[docs/decisiones.md](docs/decisiones.md).
