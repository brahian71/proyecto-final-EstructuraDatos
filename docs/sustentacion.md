# Guia de sustentacion

## Antes de empezar

- Tener abierto IntelliJ con el proyecto y `monedero.Main` listo para ejecutar.
- Tener a mano `docs/decisiones.md` y el informe.
- Correr una vez las pruebas (`PruebasFase1` a `PruebasFase5`) para mostrar que pasan.

## Guion de demostracion (5 minutos)

| Min | Pestania | Que hacer | Que decir |
|---|---|---|---|
| 0:00 | Inicio de sesion | Entrar como `admin` / `admin123` (antes, mostrar que una contrasena mala da error) | "Hay dos roles: administrador y cliente. Las contrasenas se guardan con hash SHA-256 y sal." |
| 0:20 | Resumen | Mostrar a Ana (Plata, 559 pts) y cambiar a Diego (Oro) | "Cada cliente tiene varios monederos (multimonedero). El rango sale de los puntos acumulados." |
| 0:40 | Operaciones | Transferir $120.000 de Ana a Bruno; senalar la cotizacion antes de confirmar | "Antes de confirmar se ve la comision (1 %, minimo $500, menos 10 % por Plata) y los puntos: 3 por cada $10.000 mas 10 % de bono por rango." |
| 1:30 | Operaciones | Revertir la ultima transaccion | "Cada cliente tiene una pila: se deshace la mas reciente. Se devuelven monto y comision y se descuentan los mismos puntos." |
| 2:00 | Historial | Mostrar la transaccion en gris | "No se borra: queda marcada REVERTIDA y deja de contar. La historia se conserva." |
| 2:20 | Puntos y canje | Canjear 100 puntos; mostrar ranking y clientes por rango | "Canjear gasta disponibles, no baja el rango. El ranking y los rangos salen de un arbol AVL." |
| 3:00 | Programadas | Mostrar la cola; avanzar 31 dias desde la barra | "Es un monticulo por fecha. Al avanzar el reloj se ejecutan en orden, cada una en su fecha." |
| 3:40 | Resumen | Mostrar recordatorios y notificaciones | "Las notificaciones viven en una lista circular de 10: la nueva reemplaza a la mas vieja." |
| 4:00 | Reportes y grafos | Reporte 4 (ciclos) y reporte 5 (conexion) | "Grafo dirigido: DFS encuentra el ciclo Ana -> Carla -> Bruno -> Ana; BFS el camino con menos intermediarios." |
| 4:40 | Barra superior | Verificar integridad | "Verificacion recursiva por divide y venceras: saldos, puntos, AVL y monticulo." |
| 4:50 | Barra superior | Cerrar sesion y entrar como `ana` / `ana123` | "Un cliente solo ve lo suyo: no hay selector de clientes, ni reloj, ni grafo de todos." |

## Preguntas probables

**Por que todas las estructuras son propias y no de `java.util`?**
Lo pidio el profesor. Ademas permite explicar y medir cada una. Los servicios usan interfaces propias (`Coleccion`, `Pila`, `Cola`, `Mapa`, `ColaPrioridad`).

**Por que un AVL y no solo un BST?**
Con claves que llegan casi en orden (ids consecutivos, clientes que empiezan en 0 puntos) el BST se vuelve una lista: con 1.000 claves ordenadas, altura 1.000 contra 10 del AVL. El AVL rota para mantener O(log n). Paso en el propio proyecto: la interfaz usaba un BST y con 2.000 clientes llego a altura 461.

**Como sabe el AVL cuando rotar?**
Al volver de insertar o eliminar, recalcula la altura de cada nodo del camino y su factor de balance (altura izquierda - derecha). Si es 2 o -2 aplica LL, LR, RR o RL segun hacia donde este cargado el hijo.

**Que guarda la clave del AVL de puntos?**
(puntos, id). El id desempata, asi dos clientes con los mismos puntos no chocan. Si cambian los puntos, se saca y se reinserta: O(log n).

**Por que un monticulo para las programadas?**
Hay que insertar y sacar la mas proxima todo el tiempo. Lista ordenada: insertar O(n). Lista sin orden: extraer O(n). Monticulo: ambas O(log n). Medido: 20.000 programadas en 13 ms contra 233 ms de una lista ordenada.

**Por que una pila por cliente y no una global?**
Con una global, Ana no podria revertir si Bruno opero despues. Por cliente, cada uno deshace lo suyo en orden LIFO.

**Que pasa si revierto una transferencia y el que la recibio ya gasto la plata?**
Se rechaza. Se calcula el saldo minimo que tuvo ese monedero desde la transaccion; si es menor que el monto, quitarla dejaria la historia en negativo.

**Que pasa si los puntos de esa transaccion ya se canjearon?**
Tambien se rechaza: no se pueden quitar puntos que ya no estan disponibles.

**Por que dos cuentas de puntos?**
Acumulados define el rango; disponibles es lo que se canjea. Asi canjear no castiga bajando de rango, pero revertir si, porque esos puntos nunca debieron existir.

**Por que la unidad de moneda es $100?**
El enunciado no dice cuanto vale una unidad. Con $1, un salario de $2.000.000 daria 20.000 puntos y todos serian Platino desde el primer deposito. Con $100 se gana por cada $10.000 y los rangos tienen sentido.

**Por que transferir entre monederos propios no da puntos?**
Se podria mover la misma plata de ida y vuelta solo para sumar puntos.

**Como funciona la verificacion de integridad y por que es recursiva asi?**
Divide y venceras sobre el historial pasado a arreglo: O(n) y profundidad O(log n). Recorrer la lista nodo a nodo recursivamente tendria profundidad O(n) y con 50.000 transacciones desbordaria la pila. Revisa saldos, saldo nunca negativo, orden, puntos, AVL y monticulo.

**Como detecta los ciclos de transferencias?**
DFS con tres estados: sin visitar, en pila, terminado. Si desde un vertice se llega a otro que sigue en la pila de la recursion, esa arista cierra un ciclo.

**Por que lista de adyacencia y no matriz?**
Memoria O(V + E) y no O(V^2); los clientes se transfieren con pocos otros. Cada vertice guarda una tabla hash destino -> arista, asi sumar a una arista existente es O(1).

**Como resuelve colisiones la tabla hash?**
Encadenamiento: cada cubeta es una lista. Cuando tamanio / capacidad pasa de 0,75 se duplica la capacidad y se reubica todo (rehash), O(1) amortizado.

**Por que la lista circular para notificaciones?**
Se guardan las 10 mas recientes con memoria fija. Al llenarse, la nueva reutiliza el nodo de la mas vieja avanzando un puntero: O(1), sin desplazar elementos.

**Por que el dinero en `long` y no `double`?**
`double` tiene errores de redondeo y la verificacion de integridad (saldo == suma del historial) fallaria.

**Como funciona el inicio de sesion? Se guardan las contrasenas?**
No se guardan. Se guarda SHA-256(sal + contrasena), con una sal aleatoria distinta por usuario: dos usuarios con la misma contrasena tienen hashes distintos. Al entrar se recalcula y se compara. Si falla, el mensaje es el mismo para usuario o contrasena incorrectos, para no revelar que usuarios existen. Los usuarios estan en una tabla hash por nombre: O(1).

**Que puede hacer cada rol?**
El administrador ve a todos, registra clientes con su usuario, avanza el reloj y verifica la integridad. El cliente solo opera sus monederos y ve sus propios reportes; no ve saldos de otros ni el grafo de todos.

**Como se probo que funciona?**
279 pruebas propias por fase, una prueba de estres de 60.000 operaciones al azar verificando integridad cada 100 pasos (0 fallos) y una prueba automatizada de la interfaz que recorre 82 flujos, incluido el inicio de sesion.
