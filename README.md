# Monedero Virtual con Sistema de Puntos

Proyecto Final de Estructuras de Datos. Autor: Brahian Stiven Toro C.
Java 21, IntelliJ IDEA, sin dependencias externas.

## Como correr

En IntelliJ: abrir la carpeta del proyecto, marcar `src` como Sources y `test`
como Test Sources, y ejecutar:
- `monedero.Main`: abre el inicio de sesion y la interfaz grafica (con el argumento `--consola`, el menu de texto).
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

## Fases

Entrega estimada: mediados de noviembre de 2026.

| Fase | Contenido | Estructura | Estado / fecha objetivo |
|---|---|---|---|
| 1 | Depositos, retiros, transferencias, saldo, historial, puntos, rangos, integridad recursiva | Lista enlazada, recursion (divide y venceras) | Hecha (27 oct) |
| 2 | Revertir transacciones (recalculo de puntos), notificaciones | Pila, lista circular | Hecha (27 oct) |
| 3 | Arbol de puntos, rangos, canje, comisiones, tabla hash propia | BST, AVL, tabla hash | Hecha (28 oct) |
| 4 | Transacciones programadas recurrentes, recordatorios | Monticulo (cola de prioridad) | Hecha (30 oct) |
| 5 | Patrones de gasto, transferencias entre clientes, multimonedero | Grafo dirigido (BFS, DFS), cola | Hecha (30 oct) |
| 6 | Interfaz grafica (7 pestanias, grafo dibujado) | Swing | Hecha (1 nov) |
| 7 | Ajustes a pesos colombianos (escala de puntos, bono, umbral), datos de demostracion | - | Hecha (3 nov) |
| 7b | Inicio de sesion con roles (administrador y cliente) | Tabla hash | Hecha (9 nov) |
| 8 | Informe tecnico, tabla de requisitos, ensayo de sustentacion | - | 11 - 14 nov |

## Documentos

- [docs/decisiones.md](docs/decisiones.md): decisiones de diseno y preguntas abiertas.
- [docs/sustentacion.md](docs/sustentacion.md): guion de demostracion y preguntas probables.
- [docs/mapa-del-proyecto.md](docs/mapa-del-proyecto.md): donde esta cada cosa en el codigo (se genera
  con `python docs/generar-mapa.py` desde `docs/mapa-plantilla.md`).
- `docs/informe/`: informe tecnico en Word (el `.docx` no se sube al repositorio) y
  `generar-informe.js`, que lo genera: en esa carpeta `npm install` (usa `package.json`) y
  `node generar-informe.js ../.. Informe-Tecnico-Monedero-Virtual.docx`.
- `docs/capturas/`: imagenes de cada pestania de la interfaz.
