"""Genera docs/mapa-del-proyecto.md desde docs/mapa-plantilla.md.

Cada {{Clase}} o {{Clase.metodo}} de la plantilla se reemplaza por un enlace
al archivo y a la linea donde esta declarado, asi los numeros de linea siempre
estan al dia. Uso (desde la carpeta del proyecto):

    python docs/generar-mapa.py
"""
import os
import re
import sys

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DECLARACION = re.compile(
    r'^\s*(?:@\w+\s+)*(?:(?:public|protected|private|static|final|abstract|sealed)\s+)*'
    r'(?:class|interface|enum|record)\s+(\w+)|'
    r'^    (?:(?:public|protected|private|static|final|abstract|synchronized)\s+)*'
    r'(?:<[^>]+>\s+)?[\w.<>\[\], ?]+?\s+(\w+)\s*\([^;]*$')


def indexar():
    """{'Clase': (ruta, linea), 'Clase.metodo': (ruta, linea)} con la primera declaracion."""
    indice = {}
    for carpeta in ('src', 'test'):
        for dirpath, _, archivos in os.walk(os.path.join(RAIZ, carpeta)):
            for nombre in archivos:
                if not nombre.endswith('.java'):
                    continue
                ruta = os.path.join(dirpath, nombre)
                clase = nombre[:-5]
                rel = os.path.relpath(ruta, RAIZ).replace(os.sep, '/')
                with open(ruta, encoding='utf-8') as f:
                    for n, linea in enumerate(f, 1):
                        m = DECLARACION.match(linea)
                        if not m:
                            continue
                        if m.group(1) == clase:
                            indice.setdefault(clase, (rel, n))
                        elif m.group(2) and not linea.strip().startswith(('return', 'new ', 'throw', 'if', 'for', 'while')):
                            indice.setdefault(clase + '.' + m.group(2), (rel, n))
    return indice


def main():
    indice = indexar()
    plantilla = open(os.path.join(RAIZ, 'docs', 'mapa-plantilla.md'), encoding='utf-8').read()
    faltan = []

    def enlace(m):
        clave = m.group(1)
        if clave not in indice:
            faltan.append(clave)
            return '`' + clave + '`'
        ruta, linea = indice[clave]
        return '[`%s`](../%s#L%d)' % (clave, ruta, linea)

    texto = re.sub(r'\{\{([\w.]+)\}\}', enlace, plantilla)
    texto = texto.replace('# Mapa del proyecto: donde esta cada cosa',
                          '# Mapa del proyecto: donde esta cada cosa\n\n'
                          '<!-- Generado con: python docs/generar-mapa.py (no editar a mano; editar mapa-plantilla.md) -->', 1)
    with open(os.path.join(RAIZ, 'docs', 'mapa-del-proyecto.md'), 'w', encoding='utf-8') as f:
        f.write(texto)
    if faltan:
        print('No encontrados:', ', '.join(sorted(set(faltan))))
        sys.exit(1)
    print('Mapa generado: docs/mapa-del-proyecto.md')


if __name__ == '__main__':
    main()
