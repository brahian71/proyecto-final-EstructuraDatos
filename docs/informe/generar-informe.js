// Genera el informe tecnico del Proyecto Final en Word.
// Uso: node informe.js <carpeta del proyecto> <archivo de salida .docx>
const fs = require('fs');
const path = require('path');
const {
  Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, Table, TableRow, TableCell,
  WidthType, ShadingType, BorderStyle, ImageRun, PageBreak, Footer, PageNumber, TableOfContents,
  LevelFormat, ExternalHyperlink,
} = require('docx');

const proyecto = process.argv[2];
const salida = process.argv[3];
const capturas = path.join(proyecto, 'docs', 'capturas');

const AZUL = '1F4E79';
const GRIS = 'F2F5F9';
const ANCHO = 9360; // carta con margenes de 1": 12240 - 2 * 1440

// ---------------------------------------------------------------- ayudas de texto

/** Texto con `codigo` y **negrita** en linea. */
function runs(texto, base = {}) {
  const partes = [];
  const re = /(`[^`]+`|\*\*[^*]+\*\*)/g;
  let ultimo = 0;
  let m;
  while ((m = re.exec(texto)) !== null) {
    if (m.index > ultimo) partes.push(new TextRun({ text: texto.slice(ultimo, m.index), ...base }));
    const t = m[0];
    if (t.startsWith('`')) partes.push(new TextRun({ text: t.slice(1, -1), font: 'Consolas', size: 20, ...base }));
    else partes.push(new TextRun({ text: t.slice(2, -2), bold: true, ...base }));
    ultimo = m.index + t.length;
  }
  if (ultimo < texto.length) partes.push(new TextRun({ text: texto.slice(ultimo), ...base }));
  return partes;
}

const p = (texto, opciones = {}) => new Paragraph({ children: runs(texto), spacing: { after: 120 }, ...opciones });
const h1 = (texto) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(texto)] });
const h2 = (texto) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(texto)] });
const h3 = (texto) => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(texto)] });
const vineta = (texto, nivel = 0) => new Paragraph({ children: runs(texto), numbering: { reference: 'vinetas', level: nivel }, spacing: { after: 60 } });
// cada lista numerada usa su propia instancia para que empiece en 1
let listaActual = 0;
const nuevaLista = () => { listaActual++; };
const numero = (texto) => new Paragraph({ children: runs(texto), numbering: { reference: 'numeros', level: 0, instance: listaActual }, spacing: { after: 60 } });
const salto = () => new Paragraph({ children: [new PageBreak()] });

function nota(texto) {
  return new Paragraph({
    children: runs(texto, { italics: true, color: '555555', size: 20 }),
    spacing: { before: 60, after: 160 },
  });
}

function codigo(lineas) {
  return lineas.map((l, i) => new Paragraph({
    children: [new TextRun({ text: l === '' ? ' ' : l, font: 'Consolas', size: 18 })],
    shading: { type: ShadingType.CLEAR, fill: GRIS, color: 'auto' },
    spacing: { before: i === 0 ? 80 : 0, after: i === lineas.length - 1 ? 160 : 0 },
    indent: { left: 200 },
  }));
}

// ---------------------------------------------------------------- tablas

const borde = { style: BorderStyle.SINGLE, size: 4, color: 'B8C4D2' };
const bordes = { top: borde, bottom: borde, left: borde, right: borde };

function tabla(columnas, anchos, filas) {
  const total = anchos.reduce((a, b) => a + b, 0);
  const celda = (texto, ancho, encabezado) => new TableCell({
    width: { size: ancho, type: WidthType.DXA },
    borders: bordes,
    margins: { top: 60, bottom: 60, left: 100, right: 100 },
    shading: encabezado ? { type: ShadingType.CLEAR, fill: AZUL, color: 'auto' } : undefined,
    children: [new Paragraph({ children: runs(String(texto), encabezado ? { bold: true, color: 'FFFFFF', size: 20 } : { size: 20 }) })],
  });
  return new Table({
    width: { size: total, type: WidthType.DXA },
    columnWidths: anchos,
    rows: [
      new TableRow({ tableHeader: true, children: columnas.map((c, i) => celda(c, anchos[i], true)) }),
      ...filas.map((f) => new TableRow({ children: f.map((c, i) => celda(c, anchos[i], false)) })),
    ],
  });
}

const espacio = () => new Paragraph({ children: [], spacing: { after: 120 } });

// ---------------------------------------------------------------- imagenes

function captura(archivo, titulo, w = 580) {
  const datos = fs.readFileSync(path.join(capturas, archivo));
  const ancho = datos.readUInt32BE(16);
  const alto = datos.readUInt32BE(20);
  return [
    new Paragraph({
      alignment: AlignmentType.CENTER,
      children: [new ImageRun({ type: 'png', data: datos, transformation: { width: w, height: Math.round(w * alto / ancho) },
        altText: { title: titulo, description: titulo, name: archivo } })],
      spacing: { before: 120, after: 60 },
    }),
    new Paragraph({ alignment: AlignmentType.CENTER, children: runs(titulo, { italics: true, size: 20, color: '444444' }), spacing: { after: 200 } }),
  ];
}

// ---------------------------------------------------------------- contenido

const portada = [
  new Paragraph({ children: [], spacing: { before: 2400 } }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Proyecto Final de Estructuras de Datos', size: 28, color: '555555' })], spacing: { after: 240 } }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Monedero Virtual con Sistema de Puntos', bold: true, size: 48, color: AZUL })], spacing: { after: 240 } }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Informe técnico', size: 32 })], spacing: { after: 1600 } }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Brahian Stiven Toro C.', bold: true, size: 26 })], spacing: { after: 120 } }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Docente: ________________________', size: 24 })], spacing: { after: 120 } }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: 'Noviembre de 2026', size: 24 })], spacing: { after: 600 } }),
  new Paragraph({ alignment: AlignmentType.CENTER, children: [
    new TextRun({ text: 'Código fuente: ', size: 20 }),
    new ExternalHyperlink({ link: 'https://github.com/brahian71/proyecto-final-EstructuraDatos',
      children: [new TextRun({ text: 'github.com/brahian71/proyecto-final-EstructuraDatos', style: 'Hyperlink', size: 20 })] }),
  ] }),
  salto(),
];

const indice = [
  new Paragraph({ children: [new TextRun({ text: 'Contenido', bold: true, size: 32, color: AZUL })], spacing: { after: 120 } }),
  new TableOfContents('Contenido', { hyperlink: true, headingStyleRange: '1-2' }),
];

const introduccion = [
  salto(),
  h1('1. Introducción'),
  p('Este informe describe el diseño y la implementación de un simulador de **monedero virtual con sistema de puntos**. El sistema gestiona clientes con varios monederos, permite depósitos, retiros y transferencias, consulta de saldo e historial, y agrega las funcionalidades avanzadas del enunciado: puntos por transacción, rangos, canje de beneficios, transacciones programadas recurrentes, análisis de patrones de gasto con grafos, notificaciones, reversión de transacciones y verificación recursiva de la integridad.'),
  p('El objetivo central del proyecto es la elección y el análisis de las estructuras de datos. Por eso **todas las estructuras son implementaciones propias**: no se usa ninguna colección de `java.util` (ni `ArrayList`, ni `HashMap`, ni `PriorityQueue`). Cada una se eligió según las operaciones que el monedero necesita hacer rápido, y su costo se documenta en el código y en este informe.'),
  h2('1.1 Alcance'),
  vineta('Lenguaje: Java 21, sin dependencias externas. Interfaz gráfica con Swing (incluida en el JDK).'),
  vineta('Moneda: pesos colombianos. Datos de ejemplo precargados (no hay persistencia en archivos ni base de datos).'),
  vineta('Reloj simulado: el tiempo avanza desde la interfaz, lo que permite demostrar las transacciones programadas sin esperar.'),
  vineta('Trabajo individual. Código en GitHub (enlace en la portada).'),
  h2('1.2 Organización del informe'),
  p('La sección 2 presenta la arquitectura. La 3 explica cada estructura de datos y por qué se eligió. La 4 describe los algoritmos principales y la 5 el análisis de optimización con mediciones. La 6 detalla el sistema de puntos con un ejemplo práctico. La 7 muestra la interfaz, la 8 las pruebas, la 9 la correspondencia con el enunciado y la 10 las conclusiones.'),
];

const arquitectura = [
  h1('2. Arquitectura'),
  p('El sistema está organizado en capas. La interfaz solo conversa con la fachada `SistemaMonedero`; los servicios usan las estructuras a través de interfaces propias (`Coleccion`, `Pila`, `Cola`, `Mapa`, `ColaPrioridad`). Así la lógica no depende de cómo está implementada cada estructura y la interfaz no contiene reglas de negocio.'),
  tabla(['Capa / paquete', 'Responsabilidad', 'Clases principales'], [2000, 3700, 3660], [
    ['`ui.gui`', 'Inicio de sesión e interfaz gráfica: 7 pestañas, solo lee y llama a la fachada', '`VentanaLogin`, `VentanaPrincipal`, `PanelOperaciones`, `PanelReportes`, `PanelGrafo`…'],
    ['`servicio`', 'Reglas de negocio: operaciones, puntos, comisiones, programadas, reportes, integridad, usuarios', '`SistemaMonedero`, `RegistroPuntos`, `ProgramadorTransacciones`, `AnalisisGastos`, `VerificadorIntegridad`, `ServicioUsuarios`'],
    ['`modelo`', 'Entidades del dominio', '`Cliente`, `Monedero`, `Transaccion`, `OperacionProgramada`, `Rango`, `Beneficio`'],
    ['`estructuras`', 'Estructuras de datos propias, genéricas', '`ListaEnlazada`, `PilaEnlazada`, `ColaEnlazada`, `ListaCircular`, `TablaHash`, `ArbolBST`, `ArbolAVL`, `MonticuloBinario`, `GrafoDirigido`'],
    ['`datos`, `excepciones`', 'Datos de ejemplo y errores de negocio', '`DatosIniciales`, `SaldoInsuficienteException`…'],
  ]),
  espacio(),
  h2('2.1 Flujo de una operación'),
  p('Ejemplo: el usuario hace una transferencia desde la pestaña Operaciones.'),
  (nuevaLista(), numero('`PanelOperaciones` lee los campos y llama a `SistemaMonedero.transferir(origen, destino, monto, categoría)`.')),
  numero('La fachada busca los monederos en la **tabla hash** (O(1)), calcula la comisión según el rango y los beneficios vigentes del cliente, y valida el saldo.'),
  numero('Se crea la `Transaccion`, se aplica a los dos monederos (se agrega a sus **listas enlazadas** de historial) y se apila en la **pila** de reversibles del cliente.'),
  numero('`RegistroPuntos` suma los puntos y reubica al cliente en el **árbol AVL** (O(log n)). Si cambió de rango se envía una notificación a su **lista circular**.'),
  numero('La interfaz refresca todas las vistas con los datos nuevos.'),
];

function seccionEstructura(titulo, uso, como, costos, porque) {
  return [
    h2(titulo),
    p('**Uso en el monedero.** ' + uso),
    p('**Cómo funciona.** ' + como),
    tabla(['Operación', 'Costo', 'Comentario'], [3200, 1600, 4560], costos),
    espacio(),
    p('**Por qué esta estructura.** ' + porque),
  ];
}

const estructuras = [
  h1('3. Estructuras de datos'),
  p('La tabla resume qué estructura resuelve cada requisito del enunciado. Luego se explica cada una.'),
  tabla(['Requisito', 'Estructura propia', 'Clase'], [3800, 2800, 2760], [
    ['Transacciones por cliente (historial)', 'Lista enlazada', '`ListaEnlazada`'],
    ['Transacciones reversibles', 'Pila', '`PilaEnlazada`'],
    ['Notificaciones', 'Lista circular de capacidad fija', '`ListaCircular`'],
    ['Transacciones programadas', 'Cola de prioridad (montículo binario)', '`MonticuloBinario`'],
    ['Búsquedas en anchura del grafo', 'Cola FIFO', '`ColaEnlazada`'],
    ['Acceso rápido a clientes y sus puntos', 'Tabla hash con encadenamiento', '`TablaHash`'],
    ['Puntos acumulados y rangos', 'Árbol binario de búsqueda y árbol AVL', '`ArbolBST`, `ArbolAVL`'],
    ['Transferencias, monederos y gastos', 'Grafo dirigido ponderado', '`GrafoDirigido`'],
  ]),
  espacio(),
  ...seccionEstructura('3.1 Lista enlazada',
    'Historial de cada monedero, historial de puntos de cada cliente, lista de monederos de un cliente, registro global de transacciones y resultados de los reportes.',
    'Nodos con un valor y una referencia al siguiente; se guardan la cabeza y la cola, así que agregar al final no exige recorrer. Implementa `Iterable` para recorrerse con for-each.',
    [['agregar al final', 'O(1)', 'se usa la referencia a la cola'], ['recorrer en orden', 'O(n)', 'con el iterador'],
      ['obtener(i)', 'O(n)', 'hay que caminar i nodos'], ['eliminar(x)', 'O(n)', 'hay que buscarlo']],
    'El historial crece siempre por el final y se consulta en orden cronológico completo. La lista enlazada hace ambas cosas de forma óptima sin reservar memoria de antemano.'),
  ...seccionEstructura('3.2 Pila',
    'Cada cliente tiene una pila con las transacciones que inició. Revertir deshace la cima, es decir, la más reciente (como Ctrl+Z).',
    'Lista simplemente enlazada donde la cima es el primer nodo: apilar y desapilar solo tocan la cabeza.',
    [['apilar / desapilar / cima', 'O(1)', 'solo se toca el primer nodo'], ['recorrer', 'O(n)', 'de la cima al fondo']],
    'El orden LIFO garantiza que nunca se revierte una transacción de la que dependen operaciones posteriores del mismo cliente. Se eligió una pila por cliente (y no una global) para que un cliente pueda deshacer lo suyo aunque otros hayan operado después.'),
  ...seccionEstructura('3.3 Lista circular',
    'Buzón de notificaciones de cada cliente: guarda las 10 más recientes (saldo bajo, transferencia recibida, cambio de rango, reversión, canje, recordatorios).',
    'Lista simplemente enlazada en la que el último nodo apunta al primero. Solo se guarda la referencia al último (el primero es `ultimo.siguiente`). Cuando está llena, la notificación nueva sobrescribe la más vieja reutilizando su nodo: basta con avanzar el puntero.',
    [['agregar (con o sin sobrescritura)', 'O(1)', 'no crea basura al estar llena'], ['primero / último', 'O(1)', ''], ['recorrer', 'O(n)', 'se para contando, no buscando null']],
    'Las notificaciones viejas pierden valor; la lista circular mantiene la memoria acotada sin desplazar elementos, que es lo que haría un arreglo.'),
  ...seccionEstructura('3.4 Cola FIFO',
    'Recorrido en anchura (BFS) del grafo de transferencias para encontrar cómo están conectados dos clientes.',
    'Lista enlazada con referencias al frente y al final: se encola por el final y se desencola por el frente.',
    [['encolar / desencolar / frente', 'O(1)', '']],
    'El BFS necesita procesar los vértices en el orden en que se descubren; una cola FIFO con ambos extremos a mano lo hace en tiempo constante.'),
  ...seccionEstructura('3.5 Tabla hash',
    'Clientes y monederos por id, buzones de notificaciones por cliente, operaciones programadas por id y adyacencia de cada vértice del grafo.',
    'Arreglo de cubetas; cada cubeta es una lista enlazada de las entradas cuya clave cayó en esa posición (`(hashCode & 0x7fffffff) % capacidad`). Cuando el factor de carga supera 0,75 se duplica la capacidad y se reubican las entradas (rehash).',
    [['poner / obtener / eliminar', 'O(1) promedio', 'O(n) en el peor caso'], ['rehash', 'O(n)', 'O(1) amortizado: ocurre al duplicar'],
      ['recorrer valores', 'O(n + capacidad)', 'sin orden']],
    'Casi todas las operaciones del monedero empiezan buscando un cliente o un monedero por id; hacerlo en tiempo constante evita recorrer listas en cada transacción. Con 1000 claves la cubeta más larga en las pruebas tuvo como máximo 8 entradas.'),
  ...seccionEstructura('3.6 Árbol binario de búsqueda (BST)',
    'Base del AVL (que hereda de él) y comparación en el análisis de optimización.',
    'Cada nodo tiene claves menores a la izquierda y mayores a la derecha. Insertar y eliminar son recursivos; eliminar contempla los tres casos (hoja, un hijo, dos hijos con sucesor). Ofrece recorrido inorden, consulta por rango con poda y los n mayores.',
    [['insertar / buscar / eliminar', 'O(h)', 'h = altura'], ['rango(desde, hasta)', 'O(h + k)', 'k = resultados'], ['inorden', 'O(n)', '']],
    'Mantiene los datos ordenados y responde consultas de rango sin recorrer todo. Su debilidad es que h puede llegar a n si las claves llegan en orden, lo que motiva el AVL (ver sección 5).'),
  ...seccionEstructura('3.7 Árbol AVL',
    '`RegistroPuntos` mantiene a todos los clientes en un AVL ordenado por (puntos, id). Con él se obtiene el ranking (inorden invertido), los clientes de un rango (consulta de rango: Oro = 1001 a 5000 puntos) y se reubica a un cliente cuando cambian sus puntos. La interfaz también lo usa para listar clientes y monederos ordenados por id.',
    'Es un BST que, al volver de cada inserción o eliminación, revisa el factor de balance (altura izquierda − altura derecha) de cada nodo del camino. Si llega a 2 o −2 aplica una de las cuatro rotaciones (LL, LR, RR, RL). Cada rotación cambia tres referencias, O(1).',
    [['insertar / buscar / eliminar', 'O(log n)', 'altura ≤ 1,44·log₂ n'], ['cambio de puntos de un cliente', 'O(log n)', 'eliminar + insertar'],
      ['clientes de un rango', 'O(log n + k)', ''], ['top N del ranking', 'O(log n + N)', '']],
    'Los puntos de los clientes cambian todo el tiempo y los clientes nuevos entran con 0 puntos: con un BST simple el árbol se desbalancea. El AVL garantiza O(log n) en el peor caso. El id se agrega a la clave para que dos clientes con los mismos puntos no choquen.'),
  ...seccionEstructura('3.8 Montículo binario (cola de prioridad)',
    'Transacciones programadas, ordenadas por la fecha de su próxima ejecución (en empate, la que se programó antes). También ordena los rankings de los reportes.',
    'Árbol binario completo guardado en un arreglo (padre(i) = (i−1)/2, hijos 2i+1 y 2i+2) donde cada padre es menor o igual que sus hijos. Insertar pone el elemento al final y lo sube; extraer saca la raíz, pone el último arriba y lo hunde.',
    [['insertar / extraer', 'O(log n)', ''], ['ver el próximo', 'O(1)', 'siempre en la posición 0'], ['cancelar una programada', 'O(n)', 'hay que encontrarla en el arreglo']],
    'Una lista ordenada da el próximo en O(1) pero insertar cuesta O(n); una lista sin orden inserta en O(1) pero extraer cuesta O(n). El montículo es O(log n) en ambas, que es lo que necesita un programador que inserta y extrae continuamente.'),
  ...seccionEstructura('3.9 Grafo dirigido ponderado',
    'Tres grafos que se arman desde las transacciones: transferencias entre clientes, flujo de dinero entre monederos y secuencias de categorías de gasto (X → Y si después de gastar en X el mismo cliente gastó en Y).',
    'Lista de adyacencia: cada vértice guarda una tabla hash destino → arista. Cada arista acumula el dinero (peso) y la cantidad de transacciones (veces). Incluye BFS (camino más corto), DFS (alcanzables) y detección de ciclos con DFS de tres estados.',
    [['agregar arista o sumar peso', 'O(1)', 'gracias a la tabla hash'], ['BFS / DFS / ciclos', 'O(V + E)', ''], ['memoria', 'O(V + E)', 'una matriz sería O(V²)']],
    'Las relaciones entre clientes, monederos y categorías son naturalmente dirigidas (quién envía a quién, qué gasto sigue a cuál) y dispersas, por eso la lista de adyacencia es la representación adecuada.'),
];

const algoritmos = [
  h1('4. Algoritmos principales'),
  h2('4.1 Verificación recursiva de integridad'),
  p('El enunciado pide garantizar la validez de las transacciones con algoritmos recursivos. `VerificadorIntegridad` pasa el historial de cada monedero a un arreglo y aplica **divide y vencerás**: parte el tramo a la mitad, resuelve cada mitad y combina. Comprueba:'),
  (nuevaLista(), numero('Cada transacción es válida: involucra al monedero, sus monederos existen, sus puntos base coinciden con la regla y el bono de rango no supera el máximo.')),
  numero('El historial está en orden cronológico.'),
  numero('El saldo guardado es igual a la suma recursiva de los efectos del historial.'),
  numero('El saldo nunca fue negativo en ningún momento del historial.'),
  numero('Los puntos acumulados de cada cliente son iguales a la suma de los puntos de sus transacciones vigentes.'),
  numero('Los puntos disponibles son iguales a la suma de su historial de puntos.'),
  numero('Cada cliente está en el AVL con su clave actual, y el AVL está ordenado, con alturas correctas y balanceado.'),
  numero('La cola de programadas cumple la propiedad de montículo.'),
  p('El punto 4 combina dos valores por tramo: la suma y el mínimo de las sumas parciales.'),
  ...codigo([
    'Tramo combinar(izq, der):',
    '    suma   = izq.suma + der.suma',
    '    minimo = min(izq.minimo, izq.suma + der.minimo)',
  ]),
  p('Costo: O(n) por monedero y profundidad de recursión O(log n). Recorrer la lista nodo a nodo recursivamente tendría profundidad O(n); una prueba con 50.000 transacciones en un monedero confirma que la versión por divide y vencerás no desborda la pila.'),
  h2('4.2 Reversión con recálculo de puntos'),
  p('Revertir toma la cima de la pila del cliente, devuelve los saldos (incluida la comisión), descuenta exactamente los puntos que esa transacción dio y reubica al cliente en el AVL. La transacción no se borra: queda en el historial marcada como REVERTIDA y deja de contar en saldos y puntos.'),
  p('Se rechaza si el dinero ya se gastó: si el monedero que lo recibió tuvo después un saldo menor que el monto, quitarlo dejaría el historial en negativo. Esto se calcula con el saldo mínimo desde esa transacción, O(n). También se rechaza si esos puntos ya se canjearon.'),
  h2('4.3 Simulación por eventos de las programadas'),
  p('Al avanzar el reloj se saca del montículo la programada más próxima mientras su fecha no pase el destino. El reloj salta a su fecha exacta, se envía el recordatorio 24 horas antes, se ejecuta y, si es recurrente, vuelve al montículo con su siguiente fecha. Así cada transacción queda con la fecha en que debía ocurrir y el historial sigue en orden. La fecha n se calcula desde la fecha inicial (inicio + n periodos), así una mensual del día 31 no se corre al 28 para siempre. Si falla por saldo insuficiente, se avisa al cliente y se sigue con la siguiente fecha.'),
  h2('4.4 Análisis de patrones de gasto'),
  vineta('**Gasto por categoría** y **mayores receptores**: totales por vértice, ordenados con el montículo.'),
  vineta('**Secuencias frecuentes**: aristas del grafo de categorías con más repeticiones.'),
  vineta('**Ciclos de transferencias** (A → B → C → A): DFS con tres estados (sin visitar, en pila, terminado); una arista hacia un vértice "en pila" cierra un ciclo.'),
  vineta('**Conexión entre clientes**: BFS con la cola propia; da el camino con menos intermediarios.'),
  vineta('**Flujo entre monederos**: aristas del grafo de monederos que salen o llegan a los del cliente.'),
];

const optimizacion = [
  h1('5. Análisis de optimización'),
  p('Las siguientes mediciones se obtuvieron ejecutando las clases del proyecto. Los tiempos son de este equipo y cambian un poco entre ejecuciones; lo importante es cómo crecen.'),
  h2('5.1 BST simple frente a AVL'),
  p('Insertando claves en orden creciente (como ocurre con ids consecutivos o clientes que entran con 0 puntos), el BST simple se degenera en una lista; el AVL se mantiene cerca de log₂ n:'),
  tabla(['Claves insertadas en orden', 'Altura BST', 'Altura AVL', 'log₂ n'], [3360, 2000, 2000, 2000], [
    ['100', '100', '7', '6,6'], ['1.000', '1.000', '10', '10,0'], ['10.000', '10.000 (lineal)', '14', '13,3'],
  ]),
  espacio(),
  p('Este efecto apareció en el propio proyecto: la interfaz ordenaba los clientes con un BST simple y, como la tabla hash entrega los ids casi en orden, con 2.000 clientes el árbol llegó a altura 461 (contra 12 del AVL). Se corrigió usando el AVL.'),
  h2('5.2 Montículo frente a lista ordenada'),
  p('Insertar y luego extraer todas las programadas:'),
  tabla(['Programadas', 'Montículo O(n log n)', 'Lista ordenada O(n²)'], [3120, 3120, 3120], [
    ['5.000', '11 ms', '16 ms'], ['20.000', '13 ms', '233 ms'],
  ]),
  espacio(),
  p('Al cuadruplicar la cantidad, la lista ordenada tarda unas 15 veces más, mientras el montículo casi no cambia. El proyecto de referencia del semestre anterior usaba una lista ordenada para esta función.'),
  h2('5.3 Otras decisiones de eficiencia'),
  vineta('Tabla hash con rehash al 75 %: búsquedas O(1) promedio y cubetas cortas (máximo 8 con 1.000 claves).'),
  vineta('Lista circular para notificaciones: memoria acotada y O(1) al llenarse, sin desplazar elementos.'),
  vineta('Grafo con lista de adyacencia y tabla hash por vértice: sumar a una arista existente en O(1) y memoria O(V + E).'),
  vineta('Integridad por divide y vencerás: profundidad O(log n) en lugar de O(n).'),
  vineta('Dinero en enteros (`long`): sin errores de redondeo que harían fallar la verificación de integridad.'),
];

const puntos = [
  h1('6. Sistema de puntos'),
  h2('6.1 Reglas'),
  tabla(['Transacción', 'Puntos', 'Con pesos colombianos'], [3120, 3120, 3120], [
    ['Depósito', '1 por cada 100 unidades', '1 por cada $10.000'],
    ['Retiro', '2 por cada 100 unidades', '2 por cada $10.000'],
    ['Transferencia a otro cliente', '3 por cada 100 unidades', '3 por cada $10.000'],
    ['Movimiento entre monederos propios', '0', 'evita sumar puntos moviendo la misma plata'],
  ]),
  espacio(),
  p('El enunciado no dice cuánto vale una "unidad". Con pesos y unidad = $1, un salario de $2.000.000 daría 20.000 puntos y el cliente sería Platino desde el primer depósito. Se definió **unidad = $100** (configurable en `CalculadoraPuntos`); con eso el mismo salario da 200 puntos y llegar a Platino exige meses de uso. El bono de 50 unidades queda en $5.000.'),
  p('Hay dos cuentas de puntos: **acumulados** (lo ganado menos lo revertido; definen el rango) y **disponibles** (acumulados menos lo canjeado). Así canjear no baja de rango, pero revertir sí, porque esos puntos nunca debieron existir.'),
  h2('6.2 Rangos y beneficios'),
  tabla(['Rango', 'Puntos acumulados', 'Descuento en comisión de transferencias', 'Bono de puntos en retiros y transferencias'], [1600, 2200, 2780, 2780], [
    ['Bronce', '0 – 500', '0 %', '0 %'],
    ['Plata', '501 – 1.000', '10 %', '+10 %'],
    ['Oro', '1.001 – 5.000', '20 %', '+20 %'],
    ['Platino', 'más de 5.000', '30 %', '+30 %'],
  ]),
  espacio(),
  tabla(['Canje', 'Costo', 'Efecto'], [3400, 1600, 4360], [
    ['Descuento en transferencias', '100 pts', '10 % adicional de descuento en la comisión por 30 días'],
    ['Retiros sin cargo', '500 pts', 'Retiros con comisión $0 por 30 días'],
    ['Bono de saldo', '1.000 pts', '$5.000 (50 unidades) en el monedero principal'],
  ]),
  espacio(),
  p('Comisiones: transferencia 1 % del monto con mínimo $500; retiro $2.000; depósitos y movimientos internos gratis. La comisión se calcula con el rango y los beneficios del momento.'),
  p('El **bono de puntos** del rango (el enunciado propone "bonos de puntos adicionales por ciertas transacciones") se aplica solo a retiros y transferencias, para premiar el uso del monedero y no solo el ahorro. Se calcula con el rango que el cliente tenía antes de la transacción y queda guardado en ella, así al revertir se quita completo.'),
  h2('6.3 Ejemplo práctico'),
  p('Secuencia real ejecutada en el sistema para una clienta, Ana:'),
  tabla(['Paso', 'Operación', 'Comisión', 'Puntos', 'Acumulados', 'Rango'], [700, 3160, 1300, 1300, 1500, 1400], [
    ['1', 'Deposita su salario de $4.500.000', '$0', '+450', '450', 'Bronce'],
    ['2', 'Retira $350.000 para el mercado', '$2.000', '+70', '520', 'Plata'],
    ['3', 'Transfiere $120.000 a Carla', '$1.080', '+39', '559', 'Plata'],
    ['4', 'Canjea 100 puntos (descuento)', '–', '–', '559 (459 disp.)', 'Plata'],
    ['5', 'Transfiere otros $120.000', '$960', '+39', '598', 'Plata'],
    ['6', 'Revierte la última transferencia', 'devuelta', '−39', '559', 'Plata'],
  ]),
  espacio(),
  vineta('En el paso 2 Ana sube a Plata y recibe una notificación del cambio de rango.'),
  vineta('En el paso 3 la comisión es 1 % de $120.000 = $1.200, menos 10 % por ser Plata: $1.080. Los puntos son 36 (3 por cada $10.000) más 3 de bono por ser Plata (10 % de 36).'),
  vineta('En el paso 4 gasta puntos disponibles, pero su rango no baja porque se calcula con los acumulados.'),
  vineta('En el paso 5 el descuento es 10 % (Plata) + 10 % (canje) = 20 %: paga $960.'),
  vineta('En el paso 6 recupera los $120.000 más los $960 de comisión y se le descuentan exactamente los 39 puntos de esa transferencia, bono incluido.'),
  h2('6.4 Impacto en el uso del monedero'),
  p('El sistema de puntos premia usar el monedero para pagar y transferir (2 y 3 puntos por bloque) más que solo guardar dinero (1 punto). Los rangos convierten esa actividad en ahorro concreto y en más puntos: un cliente Oro paga 20 % menos por transferir (hasta 30 % con un canje de 100 puntos) y gana 20 % más de puntos en cada pago o transferencia, lo que lo acerca a Platino más rápido. Los movimientos entre monederos propios no dan puntos, para que el incentivo sea el uso real y no inflar la cuenta. Las notificaciones de cambio de rango y los recordatorios de las programadas mantienen al cliente al tanto de su progreso.'),
];

const interfaz = [
  h1('7. Interfaz gráfica'),
  p('Al abrir el programa se pide **usuario y contraseña**. Hay dos roles: el **administrador** ve a todos los clientes, registra clientes con su usuario, avanza el reloj simulado y verifica la integridad; el **cliente** solo ve y opera sus propios monederos (no ve saldos ajenos, el grafo de todos ni las programadas de otros). Las contraseñas no se guardan: se guarda SHA-256(sal + contraseña) con una sal aleatoria por usuario, y los usuarios están en una tabla hash por nombre (O(1)). Si las credenciales fallan, el mensaje no dice si fue el usuario o la contraseña.'),
  ...captura('0-inicio-de-sesion.png', 'Figura 0. Inicio de sesión.', 300),
  p('La ventana principal tiene una barra superior con el cliente activo, la fecha del reloj simulado, el botón para avanzar días (que ejecuta las programadas), la verificación de integridad y el cierre de sesión. Cada funcionalidad tiene su pestaña. Antes de confirmar una operación se muestra la comisión y los puntos que daría. Las figuras 1 a 7 muestran la vista del administrador.'),
  ...captura('1-resumen.png', 'Figura 1. Resumen: rango con progreso al siguiente, monederos y notificaciones (lista circular).'),
  ...captura('2-operaciones.png', 'Figura 2. Operaciones con cotización de comisión y puntos; abajo, la próxima transacción a revertir (pila).'),
  ...captura('3-historial.png', 'Figura 3. Historial de un monedero (lista enlazada); las revertidas se muestran en gris.'),
  ...captura('4-puntos-y-canje.png', 'Figura 4. Canje de puntos, historial de puntos y, a la derecha, el ranking y los clientes por rango obtenidos del AVL.'),
  ...captura('5-programadas.png', 'Figura 5. Transacciones programadas en el orden en que se ejecutarán (montículo).'),
  ...captura('6-reportes-y-grafos.png', 'Figura 6. Reportes de patrones de gasto y grafo de transferencias dibujado; los ciclos (DFS) y caminos (BFS) se resaltan en rojo.'),
  ...captura('7-clientes.png', 'Figura 7. Registro de clientes (con usuario y contraseña) y monederos (multimonedero).'),
  ...captura('8-vista-cliente.png', 'Figura 8. Vista de una clienta (Ana): sin selector de clientes, reloj ni verificación; la última pestaña es "Mis monederos".'),
];

const pruebas = [
  h1('8. Pruebas y verificación'),
  p('Cada fase tiene una clase de pruebas propia que imprime OK o FALLO por cada caso (sin dependencias externas). Se prueban los casos borde de cada estructura (vacía, un elemento, eliminar el primero o el último, capacidad 1, colisiones, rotaciones) y las reglas de negocio.'),
  tabla(['Clase', 'Contenido', 'Casos'], [2400, 5560, 1400], [
    ['`PruebasFase1`', 'Operaciones básicas, puntos (incluida la escala en pesos), rangos, monto máximo e integridad', '56'],
    ['`PruebasFase2`', 'Pila, lista circular, reversión y notificaciones', '48'],
    ['`PruebasFase3`', 'Tabla hash, BST, AVL, comisiones, canje, bono por rango y ranking', '75'],
    ['`PruebasFase4`', 'Montículo y transacciones programadas', '39'],
    ['`PruebasFase5`', 'Cola, grafo (BFS, DFS, ciclos) y reportes', '36'],
    ['`PruebasFase6`', 'Inicio de sesión: hash con sal, credenciales inválidas, roles', '26'],
    ['**Total**', '', '**280**'],
  ]),
  espacio(),
  p('Además se ejecutó una prueba de estrés aleatoria: 20 escenarios de 3.000 operaciones al azar (depósitos, retiros, transferencias, reversiones, canjes, programadas, cancelaciones, saltos de reloj y reportes), es decir **60.000 operaciones**, verificando la integridad cada 100 pasos. Resultado: 0 fallos de integridad y 0 excepciones inesperadas.'),
  p('La interfaz se probó también de forma automatizada: un programa maneja la ventana real (escribe en los campos, pulsa los botones y responde los diálogos) y comprueba 82 flujos, entre ellos inicio de sesión (credenciales malas y correctas), operaciones, errores de monto, reversión, canje, cambio de cliente, programadas, reportes, registro de clientes con usuario y las restricciones de la vista de un cliente.'),
];

const trazabilidad = [
  h1('9. Correspondencia con el enunciado'),
  tabla(['Requisito del enunciado', 'Dónde se cumple'], [4000, 5360], [
    ['Depósitos, retiros (con saldo suficiente), transferencias', '`SistemaMonedero.depositar / retirar / transferir`; pestaña Operaciones'],
    ['Consulta de saldo e historial', 'Pestañas Resumen e Historial'],
    ['Puntos por tipo y monto', '`CalculadoraPuntos`; sección 6'],
    ['Puntos con árbol binario de búsqueda', '`ArbolBST` / `ArbolAVL` en `RegistroPuntos`'],
    ['Canje de puntos por beneficios', '`SistemaMonedero.canjear`; pestaña Puntos y canje'],
    ['Rangos con árbol balanceado', '`ArbolAVL`, `Rango`; ranking y clientes por rango'],
    ['Beneficios por rango: tarifas reducidas y bonos de puntos', '`ComisionEstandar`, `Rango.bonoSobre`; sección 6.2'],
    ['Transacciones programadas con cola de prioridad', '`MonticuloBinario`, `ProgramadorTransacciones`; pestaña Programadas'],
    ['Patrones de gasto con grafos dirigidos', '`GrafoDirigido`, `AnalisisGastos`; pestaña Reportes y grafos'],
    ['Alertas y notificaciones con lista circular', '`ListaCircular`, `Notificador`; pestaña Resumen'],
    ['Multimonedero con grafo de monederos', '`Cliente` con varios `Monedero`; reporte de flujo entre monederos'],
    ['Integridad con algoritmos recursivos', '`VerificadorIntegridad`; botón Verificar integridad'],
    ['Recalcular puntos al revertir', '`SistemaMonedero.revertirUltima`, `PilaEnlazada`'],
    ['Listas, pilas, colas, árboles, grafos, tablas hash', 'Paquete `estructuras` (todas propias)'],
    ['Interfaz avanzada: transacciones, puntos, rangos, canje', '`ui.gui`, 7 pestañas, inicio de sesión con roles'],
    ['Optimización de algoritmos', 'Sección 5'],
  ]),
  espacio(),
  h2('9.1 Decisiones de diseño destacadas'),
  vineta('Transferir entre monederos propios no da puntos (evita inflar puntos).'),
  vineta('Revertir no borra la transacción: queda marcada y deja de contar (se conserva la evidencia).'),
  vineta('No se puede revertir dinero que el destino ya gastó, ni puntos ya canjeados.'),
  vineta('Comisión calculada con el rango y los beneficios del momento; la paga quien envía o retira.'),
  vineta('Reloj simulado para demostrar las programadas sin esperar.'),
  vineta('Monto máximo de $1.000.000.000 por transacción (también evita desbordar los puntos).'),
  vineta('Inicio de sesión con roles; contraseñas con hash SHA-256 y sal, nunca en texto plano.'),
  nota('La lista completa de decisiones (54) está en `docs/decisiones.md` del repositorio.'),
];

const conclusiones = [
  h1('10. Conclusiones'),
  vineta('La elección de la estructura se notó en las mediciones: el AVL mantiene altura 14 con 10.000 claves ordenadas donde un BST simple llega a 10.000, y el montículo procesa 20.000 programadas unas 18 veces más rápido que una lista ordenada.'),
  vineta('Separar la lógica de las estructuras (interfaces propias) y de la interfaz gráfica permitió construir el proyecto por fases y cambiar implementaciones sin tocar el resto, como ocurrió al reemplazar el BST de la interfaz por el AVL.'),
  vineta('La verificación recursiva de integridad, junto con las 280 pruebas, la prueba de estrés y la prueba automatizada de la interfaz, da confianza en que saldos, puntos, árboles y montículo se mantienen coherentes incluso con reversiones y canjes.'),
  vineta('El sistema de puntos, con dos cuentas (acumulados y disponibles) y una escala en pesos, incentiva el uso real del monedero sin que canjear castigue al cliente.'),
  h2('10.1 Trabajo futuro'),
  vineta('Persistencia en archivos para conservar los datos entre ejecuciones.'),
  vineta('Bloqueo temporal de la cuenta tras varios intentos fallidos de inicio de sesión.'),
  h1('Anexo: cómo ejecutar'),
  p('En IntelliJ IDEA: abrir la carpeta del proyecto, marcar `src` como Sources y `test` como Test Sources, y ejecutar `monedero.Main` (interfaz gráfica), `monedero.Main --consola` (menú de texto) o `monedero.PruebasFase1` a `PruebasFase5` (pruebas). Desde PowerShell:'),
  ...codigo([
    'javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java src, test | ForEach-Object { $_.FullName })',
    'java -cp out monedero.Main',
  ]),
];

// ---------------------------------------------------------------- documento

const doc = new Document({
  creator: 'Brahian Stiven Toro C.',
  title: 'Monedero Virtual con Sistema de Puntos - Informe técnico',
  styles: {
    default: { document: { run: { font: 'Arial', size: 22 } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: 'Arial', size: 32, bold: true, color: AZUL },
        paragraph: { spacing: { before: 360, after: 160 }, keepNext: true, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: 'Arial', size: 26, bold: true, color: AZUL },
        paragraph: { spacing: { before: 240, after: 120 }, keepNext: true, outlineLevel: 1 } },
      { id: 'TOC1', name: 'toc 1', basedOn: 'Normal', next: 'Normal',
        run: { font: 'Arial', size: 20, bold: true }, paragraph: { spacing: { before: 80, after: 0 } } },
      { id: 'TOC2', name: 'toc 2', basedOn: 'Normal', next: 'Normal',
        run: { font: 'Arial', size: 20 }, paragraph: { spacing: { before: 0, after: 0 }, indent: { left: 280 } } },
      { id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { font: 'Arial', size: 22, bold: true },
        paragraph: { spacing: { before: 200, after: 80 }, keepNext: true, outlineLevel: 2 } },
    ],
  },
  numbering: {
    config: [
      { reference: 'vinetas', levels: [
        { level: 0, format: LevelFormat.BULLET, text: '•', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
        { level: 1, format: LevelFormat.BULLET, text: '◦', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 1080, hanging: 360 } } } },
      ] },
      { reference: 'numeros', levels: [
        { level: 0, format: LevelFormat.DECIMAL, text: '%1.', alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } },
      ] },
    ],
  },
  sections: [{
    properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1440, bottom: 1440, left: 1440, right: 1440 } } },
    footers: {
      default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
        children: [new TextRun({ children: ['Página ', PageNumber.CURRENT], size: 18, color: '777777' })] })] }),
    },
    children: [
      ...portada, ...indice, ...introduccion, ...arquitectura, ...estructuras, ...algoritmos,
      ...optimizacion, ...puntos, ...interfaz, ...pruebas, ...trazabilidad, ...conclusiones,
    ],
  }],
});

Packer.toBuffer(doc).then((buf) => {
  fs.writeFileSync(salida, buf);
  console.log('Informe generado: ' + salida);
});
