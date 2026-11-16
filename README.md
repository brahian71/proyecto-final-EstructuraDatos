# Monedero Virtual con Sistema de Puntos

Monedero virtual con depositos, retiros, transferencias, puntos, rangos y canje de
beneficios, construido con estructuras de datos propias e interfaz grafica en Swing.

Proyecto Final de Estructuras de Datos. Autor: Brahian Stiven Toro C.
Java 21, IntelliJ IDEA, sin dependencias externas.

## Funcionalidades por fase

| Fase | Funcionalidades | Estructuras |
|---|---|---|
| 1 | Depositos, retiros, transferencias, saldo, historial, puntos, rangos e integridad recursiva | Lista enlazada, recursion (divide y venceras) |
| 2 | Reversion de transacciones con recalculo de puntos y notificaciones | Pila, lista circular |
| 3 | Arbol de puntos, rangos, canje de beneficios y comisiones | BST, AVL, tabla hash |
| 4 | Transacciones programadas recurrentes y recordatorios | Monticulo (cola de prioridad) |
| 5 | Patrones de gasto, transferencias entre clientes y multimonedero | Grafo dirigido (BFS, DFS), cola |
| 6 | Interfaz grafica con 7 pestanias y grafo dibujado | Swing |
| 7 | Valores en pesos colombianos y datos de demostracion | - |
| 7b | Inicio de sesion con roles de administrador y cliente | Tabla hash |

## Arquitectura

La interfaz solo usa `SistemaMonedero`, y los servicios trabajan con las
interfaces de `estructuras`. Todas las estructuras son propias; no se usan las
colecciones de `java.util`.

```
src/monedero/
  Main.java
  estructuras/   Coleccion, ListaEnlazada, Pila, PilaEnlazada, ListaCircular,
                 Cola, ColaEnlazada, Mapa, TablaHash, ArbolBST, ArbolAVL,
                 ColaPrioridad, MonticuloBinario, GrafoDirigido
  modelo/        Cliente, Monedero, Transaccion, TipoTransaccion, TipoMonedero,
                 Categoria, Rango, Notificacion, TipoNotificacion,
                 Beneficio, BeneficioActivo, MovimientoPuntos,
                 OperacionProgramada, Frecuencia, Usuario, Rol
  servicio/      SistemaMonedero, CalculadoraPuntos, VerificadorIntegridad,
                 ResultadoIntegridad, Notificador, RegistroPuntos (AVL),
                 ClavePuntos, PoliticaComision, ComisionEstandar,
                 ProgramadorTransacciones (monticulo), AnalisisGastos
                 (grafos), FilaReporte, ServicioUsuarios (login), Reloj
  excepciones/   SaldoInsuficienteException, NoEncontradoException,
                 OperacionInvalidaException
  datos/         DatosIniciales (datos de ejemplo, sin persistencia)
  ui/            MenuConsola
  ui/gui/        VentanaLogin, VentanaPrincipal, PanelResumen, PanelOperaciones,
                 PanelHistorial, PanelPuntos, PanelProgramadas,
                 PanelReportes, PanelGrafo, PanelClientes,
                 Contexto, ModeloTabla, Formato
test/monedero/
  PruebasFase1 ... PruebasFase6, PruebaEstres, PruebaInterfaz,
  CapturasInterfaz, modelo/Corruptor
```

## Ejecucion

El proyecto se abre en IntelliJ con `src` como Sources y `test` como Test Sources.

| Clase | Descripcion |
|---|---|
| `monedero.Main` | Inicio de sesion e interfaz grafica; con `--consola`, menu de texto |
| `monedero.PruebasFase1` ... `PruebasFase6` | Pruebas de cada fase, sin JUnit (OK / FALLO) |
| `monedero.PruebaEstres` | 60.000 operaciones al azar verificando la integridad |
| `monedero.PruebaInterfaz` | Pruebas de la interfaz grafica |
| `monedero.CapturasInterfaz` | Capturas de la interfaz en `docs/capturas` |

Compilacion y ejecucion por consola (PowerShell):

```
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src, test | ForEach-Object { $_.FullName })
java -cp out monedero.Main
java -cp out monedero.Main --consola
java -cp out monedero.PruebasFase1
java -cp out monedero.PruebaEstres
java -cp out monedero.PruebaInterfaz
```

## Usuarios de demostracion

| Usuario | Contrasena | Rol |
|---|---|---|
| admin | admin123 | Administrador: ve a todos, registra clientes, avanza el reloj, verifica integridad |
| ana | ana123 | Cliente (Plata) |
| bruno | bruno123 | Cliente (Bronce) |
| carla | carla123 | Cliente (Bronce) |
| diego | diego123 | Cliente (Oro) |

Las contrasenas se guardan con hash SHA-256 y sal.

## Documentos

- `docs/capturas/`: capturas de la interfaz.
- `docs/informe/`: script del informe tecnico (`generar-informe.js`).
