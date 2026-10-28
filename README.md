# Monedero Virtual con Sistema de Puntos

Proyecto Final de Estructuras de Datos. Autor: Brahian Stiven Toro C.
Java 21, IntelliJ IDEA, sin dependencias externas.

## Como correr

En IntelliJ: abrir la carpeta del proyecto, marcar `src` como Sources y `test`
como Test Sources, y ejecutar `monedero.Main` (menu) o `monedero.PruebasFase1` /
`monedero.PruebasFase2` / `monedero.PruebasFase3` (pruebas).

Por consola (PowerShell):

```
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src, test | ForEach-Object { $_.FullName })
java -cp out monedero.Main
java -cp out monedero.PruebasFase1
java -cp out monedero.PruebasFase2
java -cp out monedero.PruebasFase3
```

## Arquitectura

La interfaz solo habla con `SistemaMonedero`, y los servicios solo conocen las
interfaces de `estructuras`. Todas las estructuras son propias (no se usan las
colecciones de `java.util`).

```
src/monedero/
  Main.java
  estructuras/   Coleccion, ListaEnlazada, Pila, PilaEnlazada, ListaCircular,
                 Mapa, TablaHash, ArbolBST, ArbolAVL
  modelo/        Cliente, Monedero, Transaccion, TipoTransaccion, TipoMonedero,
                 Categoria, Rango, Notificacion, TipoNotificacion,
                 Beneficio, BeneficioActivo, MovimientoPuntos
  servicio/      SistemaMonedero, CalculadoraPuntos, VerificadorIntegridad,
                 ResultadoIntegridad, Notificador, RegistroPuntos (AVL),
                 ClavePuntos, PoliticaComision, ComisionEstandar, Reloj
  excepciones/   SaldoInsuficienteException, NoEncontradoException,
                 OperacionInvalidaException
  datos/         DatosIniciales (datos de ejemplo, no hay persistencia)
  ui/            MenuConsola
test/monedero/
  PruebasFase1.java   pruebas sin JUnit, imprimen OK / FALLO
  PruebasFase2.java
  PruebasFase3.java
  modelo/Corruptor    ayuda de pruebas para simular datos corruptos
```

## Fases

| Fase | Contenido | Estructura | Estado |
|---|---|---|---|
| 1 | Depositos, retiros, transferencias, saldo, historial, puntos, rangos, integridad recursiva | Lista enlazada, recursion (divide y venceras) | Hecha |
| 2 | Revertir transacciones (recalculo de puntos), notificaciones | Pila, lista circular | Hecha |
| 3 | Arbol de puntos, rangos, canje, comisiones, tabla hash propia | BST, AVL, tabla hash | Hecha |
| 4 | Transacciones programadas recurrentes | Monticulo (cola de prioridad) | Pendiente |
| 5 | Patrones de gasto, transferencias entre clientes, multimonedero | Grafo dirigido | Pendiente |
| 6 | Interfaz grafica | Swing / JavaFX | Pendiente |

Las decisiones de diseno y las preguntas abiertas estan en
[docs/decisiones.md](docs/decisiones.md).
