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
[`SistemaMonedero`](../src/monedero/servicio/SistemaMonedero.java#L38). La interfaz nunca cambia datos por su cuenta.

---

## 2. Requisito del enunciado -> donde se ve -> donde esta el codigo

| Requisito | Donde se muestra | Codigo |
|---|---|---|
| Deposito | Operaciones -> Deposito | [`SistemaMonedero.depositar`](../src/monedero/servicio/SistemaMonedero.java#L149) |
| Retiro (con saldo suficiente) | Operaciones -> Retiro (probar un monto mayor al saldo) | [`SistemaMonedero.retirar`](../src/monedero/servicio/SistemaMonedero.java#L157), [`SistemaMonedero.validarSaldo`](../src/monedero/servicio/SistemaMonedero.java#L454) |
| Transferencia | Operaciones -> Transferencia | [`SistemaMonedero.transferir`](../src/monedero/servicio/SistemaMonedero.java#L173) |
| Consulta de saldo | Resumen (saldo total y por monedero) | [`SistemaMonedero.consultarSaldo`](../src/monedero/servicio/SistemaMonedero.java#L332), [`Cliente.saldoTotal`](../src/monedero/modelo/Cliente.java#L135) |
| Historial de transacciones | Historial | [`Monedero.getHistorial`](../src/monedero/modelo/Monedero.java#L105) (lista enlazada), [`Monedero.aplicar`](../src/monedero/modelo/Monedero.java#L47) |
| Puntos 1/2/3 por cada 100 unidades | La cotizacion antes de confirmar en Operaciones | [`CalculadoraPuntos.calcular`](../src/monedero/servicio/CalculadoraPuntos.java#L38) |
| Puntos con arbol binario de busqueda | Puntos y canje -> ranking | [`RegistroPuntos.ganar`](../src/monedero/servicio/RegistroPuntos.java#L33) (saca y reinserta en el arbol), [`ArbolBST.insertar`](../src/monedero/estructuras/ArbolBST.java#L44) |
| Canje 100 / 500 / 1000 | Puntos y canje -> botones de canje | [`SistemaMonedero.canjear`](../src/monedero/servicio/SistemaMonedero.java#L267), [`Cliente.canjear`](../src/monedero/modelo/Cliente.java#L83) |
| Rangos con arbol balanceado | Puntos y canje -> ranking y clientes por rango | [`Rango.desdePuntos`](../src/monedero/modelo/Rango.java#L35), [`RegistroPuntos.clientesDeRango`](../src/monedero/servicio/RegistroPuntos.java#L64), [`ArbolAVL.ajustar`](../src/monedero/estructuras/ArbolAVL.java#L23) |
| Beneficios del rango (tarifa reducida y bono de puntos) | Cotizacion de comision y puntos en Operaciones | [`ComisionEstandar.calcular`](../src/monedero/servicio/ComisionEstandar.java#L27), [`SistemaMonedero.bonoPorRango`](../src/monedero/servicio/SistemaMonedero.java#L410), [`Rango.bonoSobre`](../src/monedero/modelo/Rango.java#L64) |
| Transacciones programadas con cola de prioridad | Programadas + "Avanzar dias" | [`ProgramadorTransacciones.programar`](../src/monedero/servicio/ProgramadorTransacciones.java#L56), [`ProgramadorTransacciones.avanzarHasta`](../src/monedero/servicio/ProgramadorTransacciones.java#L95), [`MonticuloBinario.insertar`](../src/monedero/estructuras/MonticuloBinario.java#L41) |
| Patrones de gasto con grafos (categorias y transferencias) | Reportes y grafos | [`AnalisisGastos.grafoCategorias`](../src/monedero/servicio/AnalisisGastos.java#L71), [`AnalisisGastos.grafoTransferencias`](../src/monedero/servicio/AnalisisGastos.java#L41) |
| Multimonedero y grafo de monederos | Mis monederos / Clientes, reporte 6 | [`SistemaMonedero.crearMonedero`](../src/monedero/servicio/SistemaMonedero.java#L118), [`AnalisisGastos.grafoMonederos`](../src/monedero/servicio/AnalisisGastos.java#L54), [`AnalisisGastos.flujoMonederos`](../src/monedero/servicio/AnalisisGastos.java#L154) |
| Notificaciones con lista circular | Resumen -> Notificaciones | [`Notificador.notificar`](../src/monedero/servicio/Notificador.java#L25), [`ListaCircular.agregar`](../src/monedero/estructuras/ListaCircular.java#L48) |
| Integridad con algoritmos recursivos | Boton "Verificar integridad" (administrador) | [`VerificadorIntegridad.verificar`](../src/monedero/servicio/VerificadorIntegridad.java#L48), [`VerificadorIntegridad.sumarEfectos`](../src/monedero/servicio/VerificadorIntegridad.java#L171) |
| Revertir con recalculo de puntos | Operaciones -> "Revertir ultima transaccion" | [`SistemaMonedero.revertirUltima`](../src/monedero/servicio/SistemaMonedero.java#L214), [`Monedero.saldoMinimoDesde`](../src/monedero/modelo/Monedero.java#L87), [`RegistroPuntos.perder`](../src/monedero/servicio/RegistroPuntos.java#L46) |
| Tablas hash para acceder a los puntos por cliente | (interno) buscar cliente por id | [`SistemaMonedero.buscarCliente`](../src/monedero/servicio/SistemaMonedero.java#L127), [`TablaHash.obtener`](../src/monedero/estructuras/TablaHash.java#L74) |
| Interfaz avanzada | Toda la ventana | [`VentanaPrincipal`](../src/monedero/ui/gui/VentanaPrincipal.java#L33), [`Contexto.ejecutar`](../src/monedero/ui/gui/Contexto.java#L147) |
| Inicio de sesion (agregado) | Pantalla inicial | [`ServicioUsuarios.iniciarSesion`](../src/monedero/servicio/ServicioUsuarios.java#L61), [`VentanaLogin.ingresar`](../src/monedero/ui/gui/VentanaLogin.java#L91) |

---

## 3. Cada estructura de datos: donde esta y donde se usa

| Estructura | Clase | Para que se usa en el monedero | Metodos para mostrar |
|---|---|---|---|
| Lista enlazada | [`ListaEnlazada`](../src/monedero/estructuras/ListaEnlazada.java#L15) | Historial de cada monedero, historial de puntos, monederos de un cliente | [`ListaEnlazada.agregar`](../src/monedero/estructuras/ListaEnlazada.java#L31) (O(1) con cola) |
| Pila | [`PilaEnlazada`](../src/monedero/estructuras/PilaEnlazada.java#L15) | Transacciones reversibles de cada cliente | [`PilaEnlazada.apilar`](../src/monedero/estructuras/PilaEnlazada.java#L31), [`PilaEnlazada.desapilar`](../src/monedero/estructuras/PilaEnlazada.java#L37); uso: [`Cliente.registrarReversible`](../src/monedero/modelo/Cliente.java#L113) |
| Cola FIFO | [`ColaEnlazada`](../src/monedero/estructuras/ColaEnlazada.java#L15) | Recorrido en anchura (BFS) del grafo | [`ColaEnlazada.encolar`](../src/monedero/estructuras/ColaEnlazada.java#L31); uso: [`GrafoDirigido.caminoMasCorto`](../src/monedero/estructuras/GrafoDirigido.java#L164) |
| Lista circular | [`ListaCircular`](../src/monedero/estructuras/ListaCircular.java#L21) | Ultimas 10 notificaciones de cada cliente | [`ListaCircular.agregar`](../src/monedero/estructuras/ListaCircular.java#L48) (sobrescribe la mas vieja) |
| Tabla hash | [`TablaHash`](../src/monedero/estructuras/TablaHash.java#L24) | Clientes, monederos y usuarios por id; buzones; programadas | [`TablaHash.poner`](../src/monedero/estructuras/TablaHash.java#L56), [`TablaHash.redimensionar`](../src/monedero/estructuras/TablaHash.java#L189) |
| Arbol binario de busqueda | [`ArbolBST`](../src/monedero/estructuras/ArbolBST.java#L24) | Base del AVL; ordenar; consultas de rango | [`ArbolBST.insertar`](../src/monedero/estructuras/ArbolBST.java#L44), [`ArbolBST.eliminar`](../src/monedero/estructuras/ArbolBST.java#L96), [`ArbolBST.rango`](../src/monedero/estructuras/ArbolBST.java#L189) |
| Arbol AVL | [`ArbolAVL`](../src/monedero/estructuras/ArbolAVL.java#L18) | Clientes ordenados por puntos (ranking y rangos) | [`ArbolAVL.ajustar`](../src/monedero/estructuras/ArbolAVL.java#L23), [`ArbolAVL.rotarDerecha`](../src/monedero/estructuras/ArbolAVL.java#L62), [`ArbolAVL.rotarIzquierda`](../src/monedero/estructuras/ArbolAVL.java#L73) |
| Monticulo (cola de prioridad) | [`MonticuloBinario`](../src/monedero/estructuras/MonticuloBinario.java#L27) | Programadas por fecha; ordenar reportes | [`MonticuloBinario.subir`](../src/monedero/estructuras/MonticuloBinario.java#L141), [`MonticuloBinario.hundir`](../src/monedero/estructuras/MonticuloBinario.java#L152) |
| Grafo dirigido | [`GrafoDirigido`](../src/monedero/estructuras/GrafoDirigido.java#L21) | Transferencias, monederos, categorias | [`GrafoDirigido.agregarArista`](../src/monedero/estructuras/GrafoDirigido.java#L83), [`GrafoDirigido.ciclos`](../src/monedero/estructuras/GrafoDirigido.java#L233), [`GrafoDirigido.caminoMasCorto`](../src/monedero/estructuras/GrafoDirigido.java#L164) |

---

## 4. Preguntas tipo "muestrame donde..."

**...calculas los puntos de una transaccion?**
[`SistemaMonedero.crearTransaccion`](../src/monedero/servicio/SistemaMonedero.java#L396): puntos base con [`CalculadoraPuntos.calcular`](../src/monedero/servicio/CalculadoraPuntos.java#L38)
y bono del rango con [`SistemaMonedero.bonoPorRango`](../src/monedero/servicio/SistemaMonedero.java#L410). Se guardan en la transaccion.

**...sumas los puntos al cliente y lo reubicas en el arbol?**
[`SistemaMonedero.registrar`](../src/monedero/servicio/SistemaMonedero.java#L417) -> [`RegistroPuntos.ganar`](../src/monedero/servicio/RegistroPuntos.java#L33): saca al cliente del AVL con
su clave vieja, suma los puntos y lo vuelve a insertar.

**...calculas el rango?**
[`Rango.desdePuntos`](../src/monedero/modelo/Rango.java#L35) para un cliente. Para "todos los Oro": [`RegistroPuntos.clientesDeRango`](../src/monedero/servicio/RegistroPuntos.java#L64)
-> [`ArbolBST.rango`](../src/monedero/estructuras/ArbolBST.java#L189) sobre el AVL.

**...balanceas el arbol (rotaciones)?**
[`ArbolAVL.ajustar`](../src/monedero/estructuras/ArbolAVL.java#L23) decide el caso (LL, LR, RR, RL); [`ArbolAVL.rotarDerecha`](../src/monedero/estructuras/ArbolAVL.java#L62) y
[`ArbolAVL.rotarIzquierda`](../src/monedero/estructuras/ArbolAVL.java#L73) hacen el cambio. Se llama al volver de [`ArbolBST.insertar`](../src/monedero/estructuras/ArbolBST.java#L44)
y [`ArbolBST.eliminar`](../src/monedero/estructuras/ArbolBST.java#L96).

**...cobras la comision?**
[`ComisionEstandar.calcular`](../src/monedero/servicio/ComisionEstandar.java#L27) y [`ComisionEstandar.comisionTransferencia`](../src/monedero/servicio/ComisionEstandar.java#L35): 1 % minimo
$500 menos el descuento del rango y del canje; retiro $2.000.

**...reviertes una transaccion?**
[`SistemaMonedero.revertirUltima`](../src/monedero/servicio/SistemaMonedero.java#L214): toma la cima de la pila, valida con
[`Monedero.saldoMinimoDesde`](../src/monedero/modelo/Monedero.java#L87) que la plata no se haya gastado, devuelve saldos con
[`Monedero.revertir`](../src/monedero/modelo/Monedero.java#L67) y descuenta puntos con [`RegistroPuntos.perder`](../src/monedero/servicio/RegistroPuntos.java#L46).

**...canjeas puntos?**
[`SistemaMonedero.canjear`](../src/monedero/servicio/SistemaMonedero.java#L267) -> [`Cliente.canjear`](../src/monedero/modelo/Cliente.java#L83) (gasta disponibles, no baja el rango).

**...ejecutas las programadas?**
[`SistemaMonedero.avanzarDias`](../src/monedero/servicio/SistemaMonedero.java#L365) -> [`ProgramadorTransacciones.avanzarHasta`](../src/monedero/servicio/ProgramadorTransacciones.java#L95): saca del
monticulo la mas proxima, mueve el reloj a su fecha, la ejecuta con
[`ProgramadorTransacciones.ejecutar`](../src/monedero/servicio/ProgramadorTransacciones.java#L153) y, si es recurrente, la vuelve a meter.
La fecha siguiente: [`Frecuencia.fechaNumero`](../src/monedero/modelo/Frecuencia.java#L20).

**...mandas las notificaciones?**
[`Notificador.notificar`](../src/monedero/servicio/Notificador.java#L25). Se llaman desde [`SistemaMonedero.avisarSiSaldoBajo`](../src/monedero/servicio/SistemaMonedero.java#L427),
[`SistemaMonedero.avisarSiCambioRango`](../src/monedero/servicio/SistemaMonedero.java#L435), la reversion, el canje y las programadas.

**...detectas ciclos de transferencias?**
[`GrafoDirigido.ciclos`](../src/monedero/estructuras/GrafoDirigido.java#L233) y [`GrafoDirigido.dfsCiclos`](../src/monedero/estructuras/GrafoDirigido.java#L248) (DFS con tres estados). El
reporte esta en [`AnalisisGastos.ciclosTransferencias`](../src/monedero/servicio/AnalisisGastos.java#L139).

**...buscas la conexion entre dos clientes?**
[`GrafoDirigido.caminoMasCorto`](../src/monedero/estructuras/GrafoDirigido.java#L164) (BFS con [`ColaEnlazada`](../src/monedero/estructuras/ColaEnlazada.java#L15)).

**...verificas la integridad (recursion)?**
[`VerificadorIntegridad.verificar`](../src/monedero/servicio/VerificadorIntegridad.java#L48) -> [`VerificadorIntegridad.verificarMonedero`](../src/monedero/servicio/VerificadorIntegridad.java#L80).
Recursivos por divide y venceras: [`VerificadorIntegridad.sumarEfectos`](../src/monedero/servicio/VerificadorIntegridad.java#L171),
[`VerificadorIntegridad.validarTransacciones`](../src/monedero/servicio/VerificadorIntegridad.java#L104), [`VerificadorIntegridad.primeraFueraDeOrden`](../src/monedero/servicio/VerificadorIntegridad.java#L145),
[`VerificadorIntegridad.sumarPuntos`](../src/monedero/servicio/VerificadorIntegridad.java#L236), [`VerificadorIntegridad.sumarMovimientos`](../src/monedero/servicio/VerificadorIntegridad.java#L218);
el AVL con [`ArbolBST.esValido`](../src/monedero/estructuras/ArbolBST.java#L233) y el monticulo con [`MonticuloBinario.esValido`](../src/monedero/estructuras/MonticuloBinario.java#L103).

**...manejas las colisiones de la tabla hash?**
[`TablaHash.poner`](../src/monedero/estructuras/TablaHash.java#L56) (encadenamiento: cada cubeta es una lista) y
[`TablaHash.redimensionar`](../src/monedero/estructuras/TablaHash.java#L189) (duplica la capacidad al pasar el 75 %).

**...guardas las contrasenas?**
[`ServicioUsuarios.registrar`](../src/monedero/servicio/ServicioUsuarios.java#L50) y [`ServicioUsuarios.hash`](../src/monedero/servicio/ServicioUsuarios.java#L97): SHA-256 de sal + contrasena.
El login compara en [`ServicioUsuarios.iniciarSesion`](../src/monedero/servicio/ServicioUsuarios.java#L61).

**...decides que ve un cliente y que ve el administrador?**
[`Contexto.esAdmin`](../src/monedero/ui/gui/Contexto.java#L52) y su uso en [`VentanaPrincipal`](../src/monedero/ui/gui/VentanaPrincipal.java#L33), [`PanelReportes`](../src/monedero/ui/gui/PanelReportes.java#L25),
[`PanelProgramadas`](../src/monedero/ui/gui/PanelProgramadas.java#L29) y [`PanelClientes`](../src/monedero/ui/gui/PanelClientes.java#L28).

---

## 5. Recorrido completo de una transferencia

1. El usuario pulsa "Transferir": [`PanelOperaciones.transferir`](../src/monedero/ui/gui/PanelOperaciones.java#L140).
2. [`Contexto.ejecutar`](../src/monedero/ui/gui/Contexto.java#L147) llama a [`SistemaMonedero.transferir`](../src/monedero/servicio/SistemaMonedero.java#L173).
3. Busca los monederos en la tabla hash: [`SistemaMonedero.buscarMonedero`](../src/monedero/servicio/SistemaMonedero.java#L135).
4. Calcula la comision: [`SistemaMonedero.calcularComision`](../src/monedero/servicio/SistemaMonedero.java#L392) -> [`ComisionEstandar.calcular`](../src/monedero/servicio/ComisionEstandar.java#L27).
5. Valida el saldo: [`SistemaMonedero.validarSaldo`](../src/monedero/servicio/SistemaMonedero.java#L454).
6. Crea la transaccion con sus puntos: [`SistemaMonedero.crearTransaccion`](../src/monedero/servicio/SistemaMonedero.java#L396).
7. La aplica a los dos monederos (lista enlazada del historial): [`Monedero.aplicar`](../src/monedero/modelo/Monedero.java#L47).
8. La apila para poder revertirla y suma puntos en el AVL: [`SistemaMonedero.registrar`](../src/monedero/servicio/SistemaMonedero.java#L417).
9. Avisa si quedo con saldo bajo o subio de rango: [`SistemaMonedero.avisarSiSaldoBajo`](../src/monedero/servicio/SistemaMonedero.java#L427), [`SistemaMonedero.avisarSiCambioRango`](../src/monedero/servicio/SistemaMonedero.java#L435).
10. Se refrescan todas las pestanias: [`Contexto.refrescarTodo`](../src/monedero/ui/gui/Contexto.java#L68).

---

## 6. Pruebas

| Clase | Que prueba |
|---|---|
| [`PruebasFase1`](../test/monedero/PruebasFase1.java#L28) | Operaciones basicas, puntos, rangos, monto maximo, integridad |
| [`PruebasFase2`](../test/monedero/PruebasFase2.java#L30) | Pila, lista circular, reversion, notificaciones |
| [`PruebasFase3`](../test/monedero/PruebasFase3.java#L28) | Tabla hash, BST, AVL, comisiones, canje, bono por rango, ranking |
| [`PruebasFase4`](../test/monedero/PruebasFase4.java#L29) | Monticulo y programadas |
| [`PruebasFase5`](../test/monedero/PruebasFase5.java#L25) | Cola, grafo (BFS, DFS, ciclos) y reportes |
| [`PruebasFase6`](../test/monedero/PruebasFase6.java#L16) | Inicio de sesion y usuarios |

En IntelliJ: clic derecho sobre la clase -> Run. Cada una imprime OK / FALLO y el total.
