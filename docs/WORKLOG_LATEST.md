# Worklog — Utilitarios Python para ahorrar tokens (tools/)

**Fecha:** 2026-09-22
**Autor:** Richard Cuicapuza (con Claude Code)

## Objetivo

Reducir el consumo de tokens cuando Claude Code revisa archivos Java/TypeScript
de este repo, ya que `CLAUDE.md` se carga en cada sesión y las tareas suelen
requerir explorar controllers, entidades, servicios y componentes Angular
completos solo para entender su estructura.

Referencia de partida: `C:\Users\richa\dsrp\tools\skeletonizer.py` (otro
proyecto), portado y adaptado al stack de LoanApp (Spring Boot 21/3.2 +
Angular 17 standalone).

## Qué se creó

### `tools/skeletonizer.py`
Colapsa cuerpos de métodos en archivos Java y TypeScript, conservando
anotaciones, campos, firmas y (para controllers) los endpoints con verbo HTTP
y path resuelto. Portado del proyecto de referencia con agregados propios de
este repo:
- Nuevos modos detectados: `validator`, `guard`, `interceptor`, `test`.
- Detección de Angular signals (`signal()`, `input()`, `output()`, `computed()`)
  en el resumen de componentes/servicios.

Uso típico:
```bash
python tools/skeletonizer.py backend/.../LoanController.java --summary-only
python tools/skeletonizer.py --dir backend/src/main/java --ext java --stats
python tools/skeletonizer.py --find-field applicantEmail --dir backend/src/main/java
```

### `tools/project_map.py` (nuevo)
Índice de todo el repo en una sola pasada: recorre `backend/src` y
`frontend/src/app`, imprime una línea por archivo Java/TS con su modo
detectado y símbolo principal, más la lista de migraciones Flyway. Reemplaza
el patrón "Glob + Read archivo por archivo" cuando no se sabe dónde está algo.

```bash
python tools/project_map.py
python tools/project_map.py --mode controller
python tools/project_map.py --grep Loan
```

### `tools/sql_skeletonizer.py` (nuevo)
Resume migraciones Flyway (`V*__*.sql`): tablas, columnas con flags
(NOT NULL/DEFAULT/PK/FK/UNIQUE), constraints e índices, sin el SQL crudo.
Incluye `--check`, heurísticas alineadas al problema intencional de M9:
- Columna FK sin índice.
- Tabla sin PRIMARY KEY.
- Columnas dinero-like (`amount`, `monthly_income`, etc.) con tipo impreciso
  (`DOUBLE`/`FLOAT`) o no numérico (`VARCHAR`/`TEXT`).

Verificado contra el repo real: `--check` sobre
`backend/src/main/resources/db/migration` detecta que `amount`,
`monthly_income` y `monthly_installment` en `V1__create_loans_table.sql`
están en `VARCHAR` en vez de `NUMERIC/DECIMAL` — el problema exacto que M9
espera que el alumno encuentre y corrija en `V2`.

### `tools/README.md`
Documentación de uso de los tres scripts con ejemplos.

## Cambios en `CLAUDE.md` (raíz)

Se agregó la sección **"Utilitarios para ahorrar tokens (tools/)"**, indicando
a Claude Code cuándo preferir estos scripts sobre `Glob`/`Read` directo:
explorar el repo → `project_map.py`; revisar un archivo Java/TS sin leerlo
completo → `skeletonizer.py --summary-only`; revisar migraciones →
`sql_skeletonizer.py`.

## Prueba de activación

Se lanzó un agente fresco (sin contexto de la sesión donde se crearon los
tools) con una tarea realista: "entender los endpoints de `LoanController`
antes de agregar un endpoint de cancelación de préstamo".

**Resultado:**
- El agente sí leyó `CLAUDE.md` y `tools/README.md`, y conocía los tres
  scripts antes de decidir cómo explorar.
- Para esa tarea puntual (1 controller ya localizado + grep sobre 3 archivos)
  eligió `Read`/`Grep` directo en lugar de los scripts, con el criterio
  explícito de que `project_map.py`/`skeletonizer.py` están pensados para
  volumen (todo el repo o lotes >10 archivos), no para 1-3 archivos.
- Confirmó que para una exploración amplia ("decenas de controllers/servicios
  sin saber dónde buscar") sí habría corrido `project_map.py --mode controller`
  primero.

**Decisión:** se deja el comportamiento tal cual — es un criterio razonable
(Read/Grep ya es barato para pocos archivos; los tools rinden cuando hay
volumen). No se endureció la instrucción en `CLAUDE.md`.

## Archivos tocados

```
CLAUDE.md                       (+ sección "Utilitarios para ahorrar tokens")
tools/skeletonizer.py           (nuevo)
tools/project_map.py            (nuevo)
tools/sql_skeletonizer.py       (nuevo)
tools/README.md                 (nuevo)
docs/WORKLOG_LATEST.md          (nuevo, este archivo)
```

## Pendiente / posibles siguientes pasos

- No se cubrió `.scss` ni `.html` porque en este repo los componentes Angular
  usan template/estilos inline en el `.ts` (no hace falta por ahora).
- Si en el futuro se agregan templates/estilos separados, extender
  `skeletonizer.py` o crear un script dedicado.
- Medir en una sesión real de curso (M1-M10) cuánto ahorro de tokens da
  `project_map.py --auto-summary`/`skeletonizer.py --stats` en la práctica.
