# Monedero Virtual con Sistema de Puntos

Proyecto Final de Estructuras de Datos. Autor: Brahian Stiven Toro C.
Java 21, IntelliJ IDEA, sin dependencias externas.

## Como correr

En IntelliJ: abrir la carpeta del proyecto, marcar `src` como Sources y `test`
como Test Sources, y ejecutar:
- `monedero.Main`: abre el inicio de sesion y la interfaz grafica. Con el argumento `--consola` abre un menu de texto.
- `monedero.PruebasFase1` ... `monedero.PruebasFase6`: pruebas.
- `monedero.PruebaEstres`: 60.000 operaciones al azar verificando la integridad.
- `monedero.PruebaInterfaz`: maneja la ventana real y comprueba los flujos de la interfaz.
- `monedero.CapturasInterfaz`: guarda una imagen de cada pestania en `docs/capturas`.

Por consola (PowerShell):

```
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src, test | ForEach-Object { $_.FullName })
java -cp out monedero.Main
java -cp out monedero.Main --consola
java -cp out monedero.PruebasFase1
java -cp out monedero.PruebasFase2
java -cp out monedero.PruebasFase3
java -cp out monedero.PruebasFase4
java -cp out monedero.PruebasFase5
java -cp out monedero.PruebasFase6
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

Las contrasenas se guardan con hash SHA-256 y sal, nunca en texto plano.

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
                 OperacionProgramada, Frecuencia, Usuario, Rol
  servicio/      SistemaMonedero, CalculadoraPuntos, VerificadorIntegridad,
                 ResultadoIntegridad, Notificador, RegistroPuntos (AVL),
                 ClavePuntos, PoliticaComision, ComisionEstandar,
                 ProgramadorTransacciones (monticulo), AnalisisGastos
                 (grafos), FilaReporte, ServicioUsuarios (login), Reloj
  excepciones/   SaldoInsuficienteException, NoEncontradoException,
                 OperacionInvalidaException
  datos/         DatosIniciales (datos de ejemplo, no hay persistencia)
  ui/            MenuConsola (texto, para desarrollo)
  ui/gui/        VentanaLogin, VentanaPrincipal y una pestania por funcionalidad: PanelResumen,
                 PanelOperaciones, PanelHistorial, PanelPuntos,
                 PanelProgramadas, PanelReportes (+ PanelGrafo), PanelClientes;
                 Contexto, ModeloTabla, Formato
test/monedero/
  PruebasFase1.java   pruebas sin JUnit, imprimen OK / FALLO
  PruebasFase2.java
  PruebasFase3.java
  PruebasFase4.java
  PruebasFase5.java
  PruebasFase6.java   inicio de sesion y usuarios
  PruebaEstres.java   60.000 operaciones al azar, integridad cada 100 pasos
  PruebaInterfaz.java recorre la interfaz real (clics, campos y dialogos)
  modelo/Corruptor    ayuda de pruebas para simular datos corruptos
  CapturasInterfaz    imagenes de la interfaz para revisar y para el informe
```

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

## Documentos

- `docs/capturas/`: una imagen de cada pestania de la interfaz.
- `docs/informe/`: script que genera el informe tecnico en Word. El archivo `.docx` no se
  incluye en el repositorio. Para generarlo, dentro de esa carpeta:

```
npm install
node generar-informe.js ../.. Informe-Tecnico-Monedero-Virtual.docx
```
