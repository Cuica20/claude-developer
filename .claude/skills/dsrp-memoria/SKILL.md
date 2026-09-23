---
name: dsrp-memoria
description: >-
  Audita la memoria persistente del workspace (memory/*.md y MEMORY.md) contra el estado real del
  repo: contradicciones, archivos citados que ya no existen, contenido que CLAUDE.md ya cubre, y
  entradas que caducaron, y secretos en texto plano. Reporta y propone; el usuario decide qué se borra.
  Triggers: "revisa la memoria", "higiene de memoria", "limpia la memoria", "auditar memoria",
  "MEMORY.md está muy largo", "la memoria dice algo viejo".
---

# DSRP Memoria

## Rol

`MEMORY.md` se carga **en cada sesión**. Cada línea es un costo fijo que se paga siempre, use o no
esa información la sesión. Y una memoria desactualizada no es neutral: es peor que ninguna, porque
se lee con confianza y no se verifica.

Este skill audita. **Nunca borra por su cuenta** — reporta con evidencia y el usuario decide.

Ubicación de la memoria:
```
C:\Users\richa\.claude\projects\C--Users-richa-capacitacion2026-claude-developers\memory\
    MEMORY.md              ← índice, se carga en cada sesión
    <slug>.md              ← una memoria por archivo
```

## Precedente

El 2026-08-03, `reference_skills_ubicacion.md` afirmaba *"`dsrp-feature` es la excepción: vive a
nivel usuario"* y citaba un `.bak` como respaldo vigente. Las dos cosas eran falsas: la skill se
había movido al repo y el `.bak` estaba borrado. La memoria tenía 8 días.

---

## Las seis categorías a buscar

### 0. Secretos en texto plano (primero, siempre)

**Correr esto antes que nada.** La memoria vive fuera del repo, así que ninguna limpieza de
secretos del código la toca — es el único lugar donde nadie mira.

```bash
grep -rniE "password|passwd|token|api[_-]?key|secret|credencial" <memory>/*.md
```

Un secreto en memoria no solo está en disco en claro: **se carga en contexto**. Si aparece:

1. Migrarlo a `.env.local` con un nombre descriptivo.
2. Migrar a la skill correspondiente **el conocimiento que lo rodea** — el motivo, la ruta, el
   síntoma que evita. Eso casi nunca está en el repo y es lo que hace valiosa la memoria.
3. Recién ahí proponer borrar el archivo.

> Precedente (2026-08-03): `playwright_setup.md` tenía dos passwords en claro. Uno ya se había
> migrado desde el código ese mismo día; el otro era de una segunda cuenta que no estaba en ningún
> archivo del repo, y por eso la limpieza de secretos no lo encontró.

### 1. Contradicción con el repo (la más peligrosa)

Toda memoria que afirme algo verificable —una ruta, un nombre de archivo, una convención, un
comando— se contrasta contra el repo. `grep` o `ls`, no criterio.

```bash
# Por cada memoria, extraer las rutas que cita y verificar que existan
grep -oE '`[A-Za-z0-9_./\\-]+\.(md|py|ts|java|js|json|sql)`' <memoria>.md | tr -d '`'
```

Una ruta citada que no existe es contradicción confirmada, no sospecha.

### 2. Duplicado de lo que el repo ya documenta

Si `CLAUDE.md`, `AGENTS.md` o un `SKILL.md` ya lo dicen, la memoria sobra: el repo es la fuente y
además se versiona.

Criterio: si la respuesta a "¿de dónde saldría esto si borro la memoria?" es "de un `grep` al
repo", se borra.

**Nunca declarar un duplicado sin diffear el contenido.** Que dos archivos se llamen igual, o
cubran el mismo tema, no los hace equivalentes: uno puede ser un superconjunto del otro.

```bash
diff <(cat <memoria>.md) <(cat <archivo-del-repo>.md)
```

Leer el diff completo y preguntarse: **¿qué pierdo si borro la memoria?** Si la respuesta no es
"nada", no es un duplicado — es contenido a migrar.

> Precedente (2026-08-03): se marcó `playwright_setup.md` como "duplica la skill". El diff mostró
> que la memoria tenía 7 líneas de más con una cuenta de login adicional y el motivo por el que
> hace falta (`RoleMktGuard` redirige a `/404` y parece un error de ruta). Borrarla habría perdido
> eso. La etiqueta estaba mal aunque la conclusión —el archivo se va— fuera correcta.

### 2b. Ruta que parece muerta pero no lo es

Antes de reportar una ruta citada como inexistente, probar **todas** las bases plausibles: raíz
del repo, `backend/`, `frontend/`.

Una memoria escrita durante trabajo en el backend suele citar rutas relativas a ese módulo.

```bash
for base in "" "backend/" "frontend/"; do
  [ -e "C:/Users/richa/capacitacion2026/claude-developers/$base<ruta>" ] && echo "existe en $base"
done
```

### 3. Caducada por fecha

Memorias de un estado transitorio que ya pasó: una migración terminada, un bug arreglado, un
"pendiente" que se resolvió. Se reconocen porque describen un **momento**, no una regla.

### 4. Contenido que el `git log` ya cuenta

Fixes pasados, estructura del código, qué commit hizo qué. Eso vive en la historia, no en memoria.

### 5. Enlaces `[[...]]` rotos

Un `[[nombre]]` que no corresponde a ningún archivo. No siempre es error —puede marcar algo por
escribir— pero si acumula, es señal de reorganización a medias.

---

## Flujo

0. **Buscar secretos** (categoría 0). Antes que nada.

1. **Inventariar**: listar `memory/*.md`, tamaño y fecha de `modified` del frontmatter.

2. **Verificar rutas citadas** — el chequeo más barato y el que más encuentra. Una pasada de `ls`
   sobre cada ruta mencionada.

3. **Contrastar contra el repo** las afirmaciones verificables. **Nunca asumir que la memoria tiene
   razón**: la memoria es la hipótesis, el repo es el hecho.

4. **Medir el costo**: líneas de `MEMORY.md`, cuántas describen convenciones que ya están en
   `CLAUDE.md`.

5. **Reportar** con el formato de abajo. **Parar acá.** No borrar, no editar.

6. Con la decisión del usuario: aplicar. Si una memoria es correcta pero está incompleta, se
   **actualiza** (no se borra y reescribe: se pierde el `originSessionId`).

---

## Formato del reporte

```
Memoria auditada: N archivos, MEMORY.md con N líneas.

SECRETOS EN TEXTO PLANO (urgente)
- <archivo>.md:<línea> — <qué credencial, sin transcribir el valor>
  Conocimiento a rescatar antes de borrar: <qué dice que el repo no>

CONTRADICE EL REPO (borrar o corregir)
- <archivo>.md — dice "<cita>" — verificado: <qué dice el repo, con ruta>

YA ESTÁ EN EL REPO (candidata a borrar)
- <archivo>.md — duplica <ruta del repo>, sección "<sección>"

CADUCADA (candidata a borrar)
- <archivo>.md — describe <estado transitorio> del <fecha>, ya resuelto en <evidencia>

ENLACES ROTOS
- <archivo>.md — [[nombre]] no existe

CORRECTAS Y ÚTILES (no tocar)
- <archivo>.md — <por qué sigue valiendo>

Propuesta: borrar N, actualizar N, dejar N.
MEMORY.md quedaría en ~N líneas (hoy N).
```

---

## Reglas

- **Nunca borrar sin aprobación explícita.** La memoria vive fuera del repo — no hay `git checkout`
  que la traiga de vuelta. Es el mismo motivo por el que se reporta antes de borrar archivos.
- **Toda afirmación del reporte se cita con evidencia**: ruta, línea, o el comando que lo verifica.
  "Parece desactualizada" no es un hallazgo.
- **Al borrar un archivo, borrar también su línea en `MEMORY.md`.** Un índice que apunta a un
  archivo inexistente es exactamente el problema que este skill combate.
- **Duda razonable → dejar.** El costo de una memoria de más es unas líneas de contexto; el de
  borrar algo que costó descubrir es re-derivarlo.
- **No proponer memorias nuevas.** Este skill limpia, no escribe.
