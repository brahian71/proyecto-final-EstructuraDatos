# Mapa del proyecto: donde esta cada cosa

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
{{SistemaMonedero}}. La interfaz nunca cambia datos por su cuenta.

---

## 2. Requisito del enunciado -> donde se ve -> donde esta el codigo

| Requisito | Donde se muestra | Codigo |
|---|---|---|
| Deposito | Operaciones -> Deposito | {{SistemaMonedero.depositar}} |
| Retiro (con saldo suficiente) | Operaciones -> Retiro (probar un monto mayor al saldo) | {{SistemaMonedero.retirar}}, {{SistemaMonedero.validarSaldo}} |
| Transferencia | Operaciones -> Transferencia | {{SistemaMonedero.transferir}} |
| Consulta de saldo | Resumen (saldo total y por monedero) | {{SistemaMonedero.consultarSaldo}}, {{Cliente.saldoTotal}} |
| Historial de transacciones | Historial | {{Monedero.getHistorial}} (lista enlazada), {{Monedero.aplicar}} |
| Puntos 1/2/3 por cada 100 unidades | La cotizacion antes de confirmar en Operaciones | {{CalculadoraPuntos.calcular}} |
| Puntos con arbol binario de busqueda | Puntos y canje -> ranking | {{RegistroPuntos.ganar}} (saca y reinserta en el arbol), {{ArbolBST.insertar}} |
| Canje 100 / 500 / 1000 | Puntos y canje -> botones de canje | {{SistemaMonedero.canjear}}, {{Cliente.canjear}} |
| Rangos con arbol balanceado | Puntos y canje -> ranking y clientes por rango | {{Rango.desdePuntos}}, {{RegistroPuntos.clientesDeRango}}, {{ArbolAVL.ajustar}} |
| Beneficios del rango (tarifa reducida y bono de puntos) | Cotizacion de comision y puntos en Operaciones | {{ComisionEstandar.calcular}}, {{SistemaMonedero.bonoPorRango}}, {{Rango.bonoSobre}} |
| Transacciones programadas con cola de prioridad | Programadas + "Avanzar dias" | {{ProgramadorTransacciones.programar}}, {{ProgramadorTransacciones.avanzarHasta}}, {{MonticuloBinario.insertar}} |
| Patrones de gasto con grafos (categorias y transferencias) | Reportes y grafos | {{AnalisisGastos.grafoCategorias}}, {{AnalisisGastos.grafoTransferencias}} |
| Multimonedero y grafo de monederos | Mis monederos / Clientes, reporte 6 | {{SistemaMonedero.crearMonedero}}, {{AnalisisGastos.grafoMonederos}}, {{AnalisisGastos.flujoMonederos}} |
| Notificaciones con lista circular | Resumen -> Notificaciones | {{Notificador.notificar}}, {{ListaCircular.agregar}} |
| Integridad con algoritmos recursivos | Boton "Verificar integridad" (administrador) | {{VerificadorIntegridad.verificar}}, {{VerificadorIntegridad.sumarEfectos}} |
| Revertir con recalculo de puntos | Operaciones -> "Revertir ultima transaccion" | {{SistemaMonedero.revertirUltima}}, {{Monedero.saldoMinimoDesde}}, {{RegistroPuntos.perder}} |
| Tablas hash para acceder a los puntos por cliente | (interno) buscar cliente por id | {{SistemaMonedero.buscarCliente}}, {{TablaHash.obtener}} |
| Interfaz avanzada | Toda la ventana | {{VentanaPrincipal}}, {{Contexto.ejecutar}} |
| Inicio de sesion (agregado) | Pantalla inicial | {{ServicioUsuarios.iniciarSesion}}, {{VentanaLogin.ingresar}} |

---

## 3. Cada estructura de datos: donde esta y donde se usa

| Estructura | Clase | Para que se usa en el monedero | Metodos para mostrar |
|---|---|---|---|
| Lista enlazada | {{ListaEnlazada}} | Historial de cada monedero, historial de puntos, monederos de un cliente | {{ListaEnlazada.agregar}} (O(1) con cola) |
| Pila | {{PilaEnlazada}} | Transacciones reversibles de cada cliente | {{PilaEnlazada.apilar}}, {{PilaEnlazada.desapilar}}; uso: {{Cliente.registrarReversible}} |
| Cola FIFO | {{ColaEnlazada}} | Recorrido en anchura (BFS) del grafo | {{ColaEnlazada.encolar}}; uso: {{GrafoDirigido.caminoMasCorto}} |
| Lista circular | {{ListaCircular}} | Ultimas 10 notificaciones de cada cliente | {{ListaCircular.agregar}} (sobrescribe la mas vieja) |
| Tabla hash | {{TablaHash}} | Clientes, monederos y usuarios por id; buzones; programadas | {{TablaHash.poner}}, {{TablaHash.redimensionar}} |
| Arbol binario de busqueda | {{ArbolBST}} | Base del AVL; ordenar; consultas de rango | {{ArbolBST.insertar}}, {{ArbolBST.eliminar}}, {{ArbolBST.rango}} |
| Arbol AVL | {{ArbolAVL}} | Clientes ordenados por puntos (ranking y rangos) | {{ArbolAVL.ajustar}}, {{ArbolAVL.rotarDerecha}}, {{ArbolAVL.rotarIzquierda}} |
| Monticulo (cola de prioridad) | {{MonticuloBinario}} | Programadas por fecha; ordenar reportes | {{MonticuloBinario.subir}}, {{MonticuloBinario.hundir}} |
| Grafo dirigido | {{GrafoDirigido}} | Transferencias, monederos, categorias | {{GrafoDirigido.agregarArista}}, {{GrafoDirigido.ciclos}}, {{GrafoDirigido.caminoMasCorto}} |

---

## 4. Preguntas tipo "muestrame donde..."

**...calculas los puntos de una transaccion?**
{{SistemaMonedero.crearTransaccion}}: puntos base con {{CalculadoraPuntos.calcular}}
y bono del rango con {{SistemaMonedero.bonoPorRango}}. Se guardan en la transaccion.

**...sumas los puntos al cliente y lo reubicas en el arbol?**
{{SistemaMonedero.registrar}} -> {{RegistroPuntos.ganar}}: saca al cliente del AVL con
su clave vieja, suma los puntos y lo vuelve a insertar.

**...calculas el rango?**
{{Rango.desdePuntos}} para un cliente. Para "todos los Oro": {{RegistroPuntos.clientesDeRango}}
-> {{ArbolBST.rango}} sobre el AVL.

**...balanceas el arbol (rotaciones)?**
{{ArbolAVL.ajustar}} decide el caso (LL, LR, RR, RL); {{ArbolAVL.rotarDerecha}} y
{{ArbolAVL.rotarIzquierda}} hacen el cambio. Se llama al volver de {{ArbolBST.insertar}}
y {{ArbolBST.eliminar}}.

**...cobras la comision?**
{{ComisionEstandar.calcular}} y {{ComisionEstandar.comisionTransferencia}}: 1 % minimo
$500 menos el descuento del rango y del canje; retiro $2.000.

**...reviertes una transaccion?**
{{SistemaMonedero.revertirUltima}}: toma la cima de la pila, valida con
{{Monedero.saldoMinimoDesde}} que la plata no se haya gastado, devuelve saldos con
{{Monedero.revertir}} y descuenta puntos con {{RegistroPuntos.perder}}.

**...canjeas puntos?**
{{SistemaMonedero.canjear}} -> {{Cliente.canjear}} (gasta disponibles, no baja el rango).

**...ejecutas las programadas?**
{{SistemaMonedero.avanzarDias}} -> {{ProgramadorTransacciones.avanzarHasta}}: saca del
monticulo la mas proxima, mueve el reloj a su fecha, la ejecuta con
{{ProgramadorTransacciones.ejecutar}} y, si es recurrente, la vuelve a meter.
La fecha siguiente: {{Frecuencia.fechaNumero}}.

**...mandas las notificaciones?**
{{Notificador.notificar}}. Se llaman desde {{SistemaMonedero.avisarSiSaldoBajo}},
{{SistemaMonedero.avisarSiCambioRango}}, la reversion, el canje y las programadas.

**...detectas ciclos de transferencias?**
{{GrafoDirigido.ciclos}} y {{GrafoDirigido.dfsCiclos}} (DFS con tres estados). El
reporte esta en {{AnalisisGastos.ciclosTransferencias}}.

**...buscas la conexion entre dos clientes?**
{{GrafoDirigido.caminoMasCorto}} (BFS con {{ColaEnlazada}}).

**...verificas la integridad (recursion)?**
{{VerificadorIntegridad.verificar}} -> {{VerificadorIntegridad.verificarMonedero}}.
Recursivos por divide y venceras: {{VerificadorIntegridad.sumarEfectos}},
{{VerificadorIntegridad.validarTransacciones}}, {{VerificadorIntegridad.primeraFueraDeOrden}},
{{VerificadorIntegridad.sumarPuntos}}, {{VerificadorIntegridad.sumarMovimientos}};
el AVL con {{ArbolBST.esValido}} y el monticulo con {{MonticuloBinario.esValido}}.

**...manejas las colisiones de la tabla hash?**
{{TablaHash.poner}} (encadenamiento: cada cubeta es una lista) y
{{TablaHash.redimensionar}} (duplica la capacidad al pasar el 75 %).

**...guardas las contrasenas?**
{{ServicioUsuarios.registrar}} y {{ServicioUsuarios.hash}}: SHA-256 de sal + contrasena.
El login compara en {{ServicioUsuarios.iniciarSesion}}.

**...decides que ve un cliente y que ve el administrador?**
{{Contexto.esAdmin}} y su uso en {{VentanaPrincipal}}, {{PanelReportes}},
{{PanelProgramadas}} y {{PanelClientes}}.

---

## 5. Recorrido completo de una transferencia

1. El usuario pulsa "Transferir": {{PanelOperaciones.transferir}}.
2. {{Contexto.ejecutar}} llama a {{SistemaMonedero.transferir}}.
3. Busca los monederos en la tabla hash: {{SistemaMonedero.buscarMonedero}}.
4. Calcula la comision: {{SistemaMonedero.calcularComision}} -> {{ComisionEstandar.calcular}}.
5. Valida el saldo: {{SistemaMonedero.validarSaldo}}.
6. Crea la transaccion con sus puntos: {{SistemaMonedero.crearTransaccion}}.
7. La aplica a los dos monederos (lista enlazada del historial): {{Monedero.aplicar}}.
8. La apila para poder revertirla y suma puntos en el AVL: {{SistemaMonedero.registrar}}.
9. Avisa si quedo con saldo bajo o subio de rango: {{SistemaMonedero.avisarSiSaldoBajo}}, {{SistemaMonedero.avisarSiCambioRango}}.
10. Se refrescan todas las pestanias: {{Contexto.refrescarTodo}}.

---

## 6. Pruebas

| Clase | Que prueba |
|---|---|
| {{PruebasFase1}} | Operaciones basicas, puntos, rangos, monto maximo, integridad |
| {{PruebasFase2}} | Pila, lista circular, reversion, notificaciones |
| {{PruebasFase3}} | Tabla hash, BST, AVL, comisiones, canje, bono por rango, ranking |
| {{PruebasFase4}} | Monticulo y programadas |
| {{PruebasFase5}} | Cola, grafo (BFS, DFS, ciclos) y reportes |
| {{PruebasFase6}} | Inicio de sesion y usuarios |

En IntelliJ: clic derecho sobre la clase -> Run. Cada una imprime OK / FALLO y el total.
