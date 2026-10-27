# Monedero Virtual con Sistema de Puntos

Proyecto Final de Estructuras de Datos. Autor: Brahian Stiven Toro C.
Java 21, IntelliJ IDEA, sin dependencias externas.

## Como correr

En IntelliJ: abrir la carpeta del proyecto, marcar `src` como Sources y `test`
como Test Sources, y ejecutar `monedero.Main` (menu) o `monedero.PruebasFase1` /
`monedero.PruebasFase2` (pruebas).

Por consola (PowerShell):

```
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src, test | ForEach-Object { $_.FullName })
java -cp out monedero.Main
java -cp out monedero.PruebasFase1
java -cp out monedero.PruebasFase2
```

## Arquitectura

La interfaz solo habla con `SistemaMonedero`, y los servicios solo conocen las
interfaces de `estructuras`. Asi una estructura que todavia no se ha visto en
clase se usa primero con una implementacion temporal y despues se cambia por la
propia sin tocar el resto (por ejemplo, `MapaJava` -> `TablaHash`).

```
src/monedero/
  Main.java
  estructuras/   Coleccion, ListaEnlazada, Pila, PilaEnlazada, ListaCircular,
                 Mapa, MapaJava (temporal)
  modelo/        Cliente, Monedero, Transaccion, TipoTransaccion, TipoMonedero,
                 Categoria, Rango, Notificacion, TipoNotificacion
  servicio/      SistemaMonedero, CalculadoraPuntos, VerificadorIntegridad,
                 ResultadoIntegridad, Notificador, Reloj (simulado)
  excepciones/   SaldoInsuficienteException, NoEncontradoException,
                 OperacionInvalidaException
  datos/         DatosIniciales (datos de ejemplo, no hay persistencia)
  ui/            MenuConsola
test/monedero/
  PruebasFase1.java   pruebas sin JUnit, imprimen OK / FALLO
  PruebasFase2.java
  modelo/Corruptor    ayuda de pruebas para simular datos corruptos
```

## Fases

| Fase | Contenido | Estructura | Estado |
|---|---|---|---|
| 1 | Depositos, retiros, transferencias, saldo, historial, puntos, rangos, integridad recursiva | Lista enlazada, recursion (divide y venceras), hash temporal | Hecha |
| 2 | Revertir transacciones (recalculo de puntos), notificaciones | Pila, lista circular | Hecha |
| 3 | Arbol de puntos, rangos balanceados, canje de beneficios | BST, AVL | Pendiente |
| 4 | Transacciones programadas recurrentes, tabla hash propia | Monticulo (cola de prioridad), tabla hash | Pendiente |
| 5 | Patrones de gasto, transferencias entre clientes, multimonedero | Grafo dirigido | Pendiente |
| 6 | Interfaz grafica | Swing / JavaFX | Pendiente |

Las decisiones de diseno y las preguntas abiertas estan en
[docs/decisiones.md](docs/decisiones.md).
