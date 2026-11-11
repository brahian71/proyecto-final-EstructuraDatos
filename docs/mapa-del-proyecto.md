# Mapa del proyecto: donde esta cada cosa

<!-- Generado con: python docs/generar-mapa.py (no editar a mano; editar mapa-plantilla.md) -->

Para la sustentacion: que se muestra en la interfaz y en que parte del codigo
esta, por si el profesor pregunta "muestrame donde haces X". Los enlaces abren
el archivo en la linea del metodo (en IntelliJ y en GitHub).

**Atajos de IntelliJ para moverse rapido**

| Atajo | Para que |
|---|---|
| `Ctrl + N` | Abrir una clase por nombre (escribe `SistemaMonedero`, `ArbolAVL`...) |
| `Ctrl + F12` | Ver la lista de metodos de la clase abierta y saltar a uno |
| `Ctrl + clic` sobre un metodo | Ir a donde esta escrito |
| `Alt + F7` sobre un metodo | Ver desde donde se llama |
| `Ctrl + Alt + Izquierda` | Volver a donde estabas |

---

## 1. Como esta organizado (capas)

```
Interfaz (ui/gui)          VentanaLogin -> VentanaPrincipal -> 7 pestanias (Panel...)
      |                    solo lee datos y llama a la fachada; no tiene reglas
      v
Fachada (servicio)         SistemaMonedero: depositar, retirar, transferir, revertir, canjear...
      |                    + RegistroPuntos, ProgramadorTransacciones, AnalisisGastos,
      |                      VerificadorIntegridad, ServicioUsuarios, Notificador
      v
Modelo (modelo)            Cliente, Monedero, Transaccion, OperacionProgramada, Usuario...
      |
      v
Estructuras (estructuras)  ListaEnlazada, PilaEnlazada, ColaEnlazada, ListaCircular,
                           TablaHash, ArbolBST, ArbolAVL, MonticuloBinario, GrafoDirigido
```

Si preguntan "donde esta la logica": casi todo empieza en
[`SistemaMonedero`](../src/monedero/servicio/SistemaMonedero.java#L24). La interfaz nunca cambia datos por su cuenta.

---

## 2. Requisito del enunciado -> donde se ve -> donde esta el codigo

| Requisito | Donde se muestra | Codigo |
|---|---|---|
| Deposito | Operaciones -> Deposito | [`SistemaMonedero.depositar`](../src/monedero/servicio/SistemaMonedero.java#L115) |
| Retiro (con saldo suficiente) | Operaciones -> Retiro (probar un monto mayor al saldo) | [`SistemaMonedero.retirar`](../src/monedero/servicio/SistemaMonedero.java#L123), [`SistemaMonedero.validarSaldo`](../src/monedero/servicio/SistemaMonedero.java#L373) |
| Transferencia | Operaciones -> Transferencia | [`SistemaMonedero.transferir`](../src/monedero/servicio/SistemaMonedero.java#L135) |
| Consulta de saldo | Resumen (saldo total y por monedero) | [`SistemaMonedero.consultarSaldo`](../src/monedero/servicio/SistemaMonedero.java#L269), [`Cliente.saldoTotal`](../src/monedero/modelo/Cliente.java#L110) |
| Historial de transacciones | Historial | [`Monedero.getHistorial`](../src/monedero/modelo/Monedero.java#L79) (lista enlazada), [`Monedero.aplicar`](../src/monedero/modelo/Monedero.java#L33) |
| Puntos 1/2/3 por cada 100 unidades | La cotizacion antes de confirmar en Operaciones | [`CalculadoraPuntos.calcular`](../src/monedero/servicio/CalculadoraPuntos.java#L21) |
| Puntos con arbol binario de busqueda | Puntos y canje -> ranking | [`RegistroPuntos.ganar`](../src/monedero/servicio/RegistroPuntos.java#L19) (saca y reinserta en el arbol), [`ArbolBST.insertar`](../src/monedero/estructuras/ArbolBST.java#L24) |
| Canje 100 / 500 / 1000 | Puntos y canje -> botones de canje | [`SistemaMonedero.canjear`](../src/monedero/servicio/SistemaMonedero.java#L209), [`Cliente.canjear`](../src/monedero/modelo/Cliente.java#L61) |
| Rangos con arbol balanceado | Puntos y canje -> ranking y clientes por rango | [`Rango.desdePuntos`](../src/monedero/modelo/Rango.java#L22), [`RegistroPuntos.clientesDeRango`](../src/monedero/servicio/RegistroPuntos.java#L47), [`ArbolAVL.ajustar`](../src/monedero/estructuras/ArbolAVL.java#L10) |
| Beneficios del rango (tarifa reducida y bono de puntos) | Cotizacion de comision y puntos en Operaciones | [`ComisionEstandar.calcular`](../src/monedero/servicio/ComisionEstandar.java#L17), [`SistemaMonedero.bonoPorRango`](../src/monedero/servicio/SistemaMonedero.java#L330), [`Rango.bonoSobre`](../src/monedero/modelo/Rango.java#L50) |
| Transacciones programadas con cola de prioridad | Programadas + "Avanzar dias" | [`ProgramadorTransacciones.programar`](../src/monedero/servicio/ProgramadorTransacciones.java#L33), [`ProgramadorTransacciones.avanzarHasta`](../src/monedero/servicio/ProgramadorTransacciones.java#L69), [`MonticuloBinario.insertar`](../src/monedero/estructuras/MonticuloBinario.java#L23) |
| Patrones de gasto con grafos (categorias y transferencias) | Reportes y grafos | [`AnalisisGastos.grafoCategorias`](../src/monedero/servicio/AnalisisGastos.java#L51), [`AnalisisGastos.grafoTransferencias`](../src/monedero/servicio/AnalisisGastos.java#L25) |
| Multimonedero y grafo de monederos | Mis monederos / Clientes, reporte 6 | [`SistemaMonedero.crearMonedero`](../src/monedero/servicio/SistemaMonedero.java#L86), [`AnalisisGastos.grafoMonederos`](../src/monedero/servicio/AnalisisGastos.java#L38), [`AnalisisGastos.flujoMonederos`](../src/monedero/servicio/AnalisisGastos.java#L123) |
| Notificaciones con lista circular | Resumen -> Notificaciones | [`Notificador.notificar`](../src/monedero/servicio/Notificador.java#L18), [`ListaCircular.agregar`](../src/monedero/estructuras/ListaCircular.java#L29) |
| Integridad con algoritmos recursivos | Boton "Verificar integridad" (administrador) | [`VerificadorIntegridad.verificar`](../src/monedero/servicio/VerificadorIntegridad.java#L18), [`VerificadorIntegridad.sumarEfectos`](../src/monedero/servicio/VerificadorIntegridad.java#L125) |
| Revertir con recalculo de puntos | Operaciones -> "Revertir ultima transaccion" | [`SistemaMonedero.revertirUltima`](../src/monedero/servicio/SistemaMonedero.java#L163), [`Monedero.saldoMinimoDesde`](../src/monedero/modelo/Monedero.java#L62), [`RegistroPuntos.perder`](../src/monedero/servicio/RegistroPuntos.java#L31) |
| Tablas hash para acceder a los puntos por cliente | (interno) buscar cliente por id | [`SistemaMonedero.buscarCliente`](../src/monedero/servicio/SistemaMonedero.java#L95), [`TablaHash.obtener`](../src/monedero/estructuras/TablaHash.java#L56) |
| Interfaz avanzada | Toda la ventana | [`VentanaPrincipal`](../src/monedero/ui/gui/VentanaPrincipal.java#L27), [`Contexto.ejecutar`](../src/monedero/ui/gui/Contexto.java#L113) |
| Inicio de sesion (agregado) | Pantalla inicial | [`ServicioUsuarios.iniciarSesion`](../src/monedero/servicio/ServicioUsuarios.java#L49), [`VentanaLogin.ingresar`](../src/monedero/ui/gui/VentanaLogin.java#L99) |

---

## 3. Cada estructura de datos: donde esta y donde se usa

| Estructura | Clase | Para que se usa en el monedero | Metodos para mostrar |
|---|---|---|---|
| Lista enlazada | [`ListaEnlazada`](../src/monedero/estructuras/ListaEnlazada.java#L7) | Historial de cada monedero, historial de puntos, monederos de un cliente | [`ListaEnlazada.agregar`](../src/monedero/estructuras/ListaEnlazada.java#L23) (O(1) con cola) |
| Pila | [`PilaEnlazada`](../src/monedero/estructuras/PilaEnlazada.java#L6) | Transacciones reversibles de cada cliente | [`PilaEnlazada.apilar`](../src/monedero/estructuras/PilaEnlazada.java#L22), [`PilaEnlazada.desapilar`](../src/monedero/estructuras/PilaEnlazada.java#L28); uso: [`Cliente.registrarReversible`](../src/monedero/modelo/Cliente.java#L90) |
| Cola FIFO | [`ColaEnlazada`](../src/monedero/estructuras/ColaEnlazada.java#L6) | Recorrido en anchura (BFS) del grafo | [`ColaEnlazada.encolar`](../src/monedero/estructuras/ColaEnlazada.java#L22); uso: [`GrafoDirigido.caminoMasCorto`](../src/monedero/estructuras/GrafoDirigido.java#L136) |
| Lista circular | [`ListaCircular`](../src/monedero/estructuras/ListaCircular.java#L6) | Ultimas 10 notificaciones de cada cliente | [`ListaCircular.agregar`](../src/monedero/estructuras/ListaCircular.java#L29) (sobrescribe la mas vieja) |
| Tabla hash | [`TablaHash`](../src/monedero/estructuras/TablaHash.java#L7) | Clientes, monederos y usuarios por id; buzones; programadas | [`TablaHash.poner`](../src/monedero/estructuras/TablaHash.java#L39), [`TablaHash.redimensionar`](../src/monedero/estructuras/TablaHash.java#L166) |
| Arbol binario de busqueda | [`ArbolBST`](../src/monedero/estructuras/ArbolBST.java#L6) | Base del AVL; ordenar; consultas de rango | [`ArbolBST.insertar`](../src/monedero/estructuras/ArbolBST.java#L24), [`ArbolBST.eliminar`](../src/monedero/estructuras/ArbolBST.java#L70), [`ArbolBST.rango`](../src/monedero/estructuras/ArbolBST.java#L149) |
| Arbol AVL | [`ArbolAVL`](../src/monedero/estructuras/ArbolAVL.java#L3) | Clientes ordenados por puntos (ranking y rangos) | [`ArbolAVL.ajustar`](../src/monedero/estructuras/ArbolAVL.java#L10), [`ArbolAVL.rotarDerecha`](../src/monedero/estructuras/ArbolAVL.java#L41), [`ArbolAVL.rotarIzquierda`](../src/monedero/estructuras/ArbolAVL.java#L51) |
| Monticulo (cola de prioridad) | [`MonticuloBinario`](../src/monedero/estructuras/MonticuloBinario.java#L9) | Programadas por fecha; ordenar reportes | [`MonticuloBinario.subir`](../src/monedero/estructuras/MonticuloBinario.java#L114), [`MonticuloBinario.hundir`](../src/monedero/estructuras/MonticuloBinario.java#L125) |
| Grafo dirigido | [`GrafoDirigido`](../src/monedero/estructuras/GrafoDirigido.java#L5) | Transferencias, monederos, categorias | [`GrafoDirigido.agregarArista`](../src/monedero/estructuras/GrafoDirigido.java#L64), [`GrafoDirigido.ciclos`](../src/monedero/estructuras/GrafoDirigido.java#L193), [`GrafoDirigido.caminoMasCorto`](../src/monedero/estructuras/GrafoDirigido.java#L136) |

---

## 4. Preguntas tipo "muestrame donde..."

**...calculas los puntos de una transaccion?**
[`SistemaMonedero.crearTransaccion`](../src/monedero/servicio/SistemaMonedero.java#L321): puntos base con [`CalculadoraPuntos.calcular`](../src/monedero/servicio/CalculadoraPuntos.java#L21)
y bono del rango con [`SistemaMonedero.bonoPorRango`](../src/monedero/servicio/SistemaMonedero.java#L330). Se guardan en la transaccion.

**...sumas los puntos al cliente y lo reubicas en el arbol?**
[`SistemaMonedero.registrar`](../src/monedero/servicio/SistemaMonedero.java#L337) -> [`RegistroPuntos.ganar`](../src/monedero/servicio/RegistroPuntos.java#L19): saca al cliente del AVL con
su clave vieja, suma los puntos y lo vuelve a insertar.

**...calculas el rango?**
[`Rango.desdePuntos`](../src/monedero/modelo/Rango.java#L22) para un cliente. Para "todos los Oro": [`RegistroPuntos.clientesDeRango`](../src/monedero/servicio/RegistroPuntos.java#L47)
-> [`ArbolBST.rango`](../src/monedero/estructuras/ArbolBST.java#L149) sobre el AVL.

**...balanceas el arbol (rotaciones)?**
[`ArbolAVL.ajustar`](../src/monedero/estructuras/ArbolAVL.java#L10) decide el caso (LL, LR, RR, RL); [`ArbolAVL.rotarDerecha`](../src/monedero/estructuras/ArbolAVL.java#L41) y
[`ArbolAVL.rotarIzquierda`](../src/monedero/estructuras/ArbolAVL.java#L51) hacen el cambio. Se llama al volver de [`ArbolBST.insertar`](../src/monedero/estructuras/ArbolBST.java#L24)
y [`ArbolBST.eliminar`](../src/monedero/estructuras/ArbolBST.java#L70).

**...cobras la comision?**
[`ComisionEstandar.calcular`](../src/monedero/servicio/ComisionEstandar.java#L17) y [`ComisionEstandar.comisionTransferencia`](../src/monedero/servicio/ComisionEstandar.java#L25): 1 % minimo
$500 menos el descuento del rango y del canje; retiro $2.000.

**...reviertes una transaccion?**
[`SistemaMonedero.revertirUltima`](../src/monedero/servicio/SistemaMonedero.java#L163): toma la cima de la pila, valida con
[`Monedero.saldoMinimoDesde`](../src/monedero/modelo/Monedero.java#L62) que la plata no se haya gastado, devuelve saldos con
[`Monedero.revertir`](../src/monedero/modelo/Monedero.java#L48) y descuenta puntos con [`RegistroPuntos.perder`](../src/monedero/servicio/RegistroPuntos.java#L31).

**...canjeas puntos?**
[`SistemaMonedero.canjear`](../src/monedero/servicio/SistemaMonedero.java#L209) -> [`Cliente.canjear`](../src/monedero/modelo/Cliente.java#L61) (gasta disponibles, no baja el rango).

**...ejecutas las programadas?**
[`SistemaMonedero.avanzarDias`](../src/monedero/servicio/SistemaMonedero.java#L297) -> [`ProgramadorTransacciones.avanzarHasta`](../src/monedero/servicio/ProgramadorTransacciones.java#L69): saca del
monticulo la mas proxima, mueve el reloj a su fecha, la ejecuta con
[`ProgramadorTransacciones.ejecutar`](../src/monedero/servicio/ProgramadorTransacciones.java#L118) y, si es recurrente, la vuelve a meter.
La fecha siguiente: [`Frecuencia.fechaNumero`](../src/monedero/modelo/Frecuencia.java#L13).

**...mandas las notificaciones?**
[`Notificador.notificar`](../src/monedero/servicio/Notificador.java#L18). Se llaman desde [`SistemaMonedero.avisarSiSaldoBajo`](../src/monedero/servicio/SistemaMonedero.java#L347),
[`SistemaMonedero.avisarSiCambioRango`](../src/monedero/servicio/SistemaMonedero.java#L355), la reversion, el canje y las programadas.

**...detectas ciclos de transferencias?**
[`GrafoDirigido.ciclos`](../src/monedero/estructuras/GrafoDirigido.java#L193) y [`GrafoDirigido.dfsCiclos`](../src/monedero/estructuras/GrafoDirigido.java#L208) (DFS con tres estados). El
reporte esta en [`AnalisisGastos.ciclosTransferencias`](../src/monedero/servicio/AnalisisGastos.java#L113).

**...buscas la conexion entre dos clientes?**
[`GrafoDirigido.caminoMasCorto`](../src/monedero/estructuras/GrafoDirigido.java#L136) (BFS con [`ColaEnlazada`](../src/monedero/estructuras/ColaEnlazada.java#L6)).

**...verificas la integridad (recursion)?**
[`VerificadorIntegridad.verificar`](../src/monedero/servicio/VerificadorIntegridad.java#L18) -> [`VerificadorIntegridad.verificarMonedero`](../src/monedero/servicio/VerificadorIntegridad.java#L48).
Recursivos por divide y venceras: [`VerificadorIntegridad.sumarEfectos`](../src/monedero/servicio/VerificadorIntegridad.java#L125),
[`VerificadorIntegridad.validarTransacciones`](../src/monedero/servicio/VerificadorIntegridad.java#L71), [`VerificadorIntegridad.primeraFueraDeOrden`](../src/monedero/servicio/VerificadorIntegridad.java#L106),
[`VerificadorIntegridad.sumarPuntos`](../src/monedero/servicio/VerificadorIntegridad.java#L180), [`VerificadorIntegridad.sumarMovimientos`](../src/monedero/servicio/VerificadorIntegridad.java#L169);
el AVL con [`ArbolBST.esValido`](../src/monedero/estructuras/ArbolBST.java#L184) y el monticulo con [`MonticuloBinario.esValido`](../src/monedero/estructuras/MonticuloBinario.java#L80).

**...manejas las colisiones de la tabla hash?**
[`TablaHash.poner`](../src/monedero/estructuras/TablaHash.java#L39) (encadenamiento: cada cubeta es una lista) y
[`TablaHash.redimensionar`](../src/monedero/estructuras/TablaHash.java#L166) (duplica la capacidad al pasar el 75 %).

**...guardas las contrasenas?**
[`ServicioUsuarios.registrar`](../src/monedero/servicio/ServicioUsuarios.java#L39) y [`ServicioUsuarios.hash`](../src/monedero/servicio/ServicioUsuarios.java#L84): SHA-256 de sal + contrasena.
El login compara en [`ServicioUsuarios.iniciarSesion`](../src/monedero/servicio/ServicioUsuarios.java#L49).

**...decides que ve un cliente y que ve el administrador?**
[`Contexto.esAdmin`](../src/monedero/ui/gui/Contexto.java#L38) y su uso en [`VentanaPrincipal`](../src/monedero/ui/gui/VentanaPrincipal.java#L27), [`PanelReportes`](../src/monedero/ui/gui/PanelReportes.java#L20),
[`PanelProgramadas`](../src/monedero/ui/gui/PanelProgramadas.java#L25) y [`PanelClientes`](../src/monedero/ui/gui/PanelClientes.java#L23).

---

## 5. Recorrido completo de una transferencia

1. El usuario pulsa "Transferir": [`PanelOperaciones.transferir`](../src/monedero/ui/gui/PanelOperaciones.java#L131).
2. [`Contexto.ejecutar`](../src/monedero/ui/gui/Contexto.java#L113) llama a [`SistemaMonedero.transferir`](../src/monedero/servicio/SistemaMonedero.java#L135).
3. Busca los monederos en la tabla hash: [`SistemaMonedero.buscarMonedero`](../src/monedero/servicio/SistemaMonedero.java#L103).
4. Calcula la comision: [`SistemaMonedero.calcularComision`](../src/monedero/servicio/SistemaMonedero.java#L317) -> [`ComisionEstandar.calcular`](../src/monedero/servicio/ComisionEstandar.java#L17).
5. Valida el saldo: [`SistemaMonedero.validarSaldo`](../src/monedero/servicio/SistemaMonedero.java#L373).
6. Crea la transaccion con sus puntos: [`SistemaMonedero.crearTransaccion`](../src/monedero/servicio/SistemaMonedero.java#L321).
7. La aplica a los dos monederos (lista enlazada del historial): [`Monedero.aplicar`](../src/monedero/modelo/Monedero.java#L33).
8. La apila para poder revertirla y suma puntos en el AVL: [`SistemaMonedero.registrar`](../src/monedero/servicio/SistemaMonedero.java#L337).
9. Avisa si quedo con saldo bajo o subio de rango: [`SistemaMonedero.avisarSiSaldoBajo`](../src/monedero/servicio/SistemaMonedero.java#L347), [`SistemaMonedero.avisarSiCambioRango`](../src/monedero/servicio/SistemaMonedero.java#L355).
10. Se refrescan todas las pestanias: [`Contexto.refrescarTodo`](../src/monedero/ui/gui/Contexto.java#L54).

---

## 6. Pruebas

| Clase | Que prueba |
|---|---|
| [`PruebasFase1`](../test/monedero/PruebasFase1.java#L24) | Operaciones basicas, puntos, rangos, monto maximo, integridad |
| [`PruebasFase2`](../test/monedero/PruebasFase2.java#L26) | Pila, lista circular, reversion, notificaciones |
| [`PruebasFase3`](../test/monedero/PruebasFase3.java#L24) | Tabla hash, BST, AVL, comisiones, canje, bono por rango, ranking |
| [`PruebasFase4`](../test/monedero/PruebasFase4.java#L25) | Monticulo y programadas |
| [`PruebasFase5`](../test/monedero/PruebasFase5.java#L21) | Cola, grafo (BFS, DFS, ciclos) y reportes |
| [`PruebasFase6`](../test/monedero/PruebasFase6.java#L12) | Inicio de sesion y usuarios |

En IntelliJ: clic derecho sobre la clase -> Run. Cada una imprime OK / FALLO y el total.
