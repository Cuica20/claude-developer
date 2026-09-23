# tools/ — utilitarios para ahorrar tokens al revisar código con Claude

Scripts standalone en Python (sin dependencias externas). Pensados para correr
*antes* de pedirle a Claude que revise algo, o para que Claude mismo los
invoque vía Bash cuando necesite explorar el repo sin leer archivos completos.

## project_map.py — índice de todo el repo en una pasada

Reemplaza el patrón "Glob + Read archivo por archivo". Lista cada archivo
Java/TS de `backend/` y `frontend/` con su modo detectado (controller,
entity, service, dto, component, validator...) y su símbolo principal.

```bash
python tools/project_map.py                     # todo el repo
python tools/project_map.py --backend-only
python tools/project_map.py --mode controller    # solo controllers
python tools/project_map.py --grep Loan          # filtra por path/símbolo
```

Úsalo al arrancar una tarea grande ("¿dónde está la lógica de X?") para
decidir qué archivos vale la pena leer completos, antes de gastar tokens
leyéndolos todos.

## skeletonizer.py — colapsa cuerpos de métodos, deja firmas

Para Java y TypeScript (incluye componentes/servicios Angular standalone).
Quita cuerpos de métodos, imports y comentarios; conserva anotaciones,
campos, firmas y (para controllers) los endpoints con su verbo/path.

```bash
python tools/skeletonizer.py backend/.../LoanController.java --summary-only
python tools/skeletonizer.py frontend/.../loan-form.component.ts
python tools/skeletonizer.py --dir backend/src/main/java --ext java --stats
python tools/skeletonizer.py --find-field applicantEmail --dir backend/src/main/java
```

Flags útiles: `--summary-only`, `--fields-only`, `--endpoints-only`,
`--public-only`, `--auto-summary` (fuerza summary-only si `--dir` trae >10
archivos).

## sql_skeletonizer.py — migraciones Flyway sin ruido

Extrae tablas/columnas/constraints/índices de los `V*__*.sql`, e incluye un
modo `--check` con heurísticas alineadas al problema intencional de M9
(columnas de dinero como VARCHAR/DOUBLE, FKs sin índice, tablas sin PK).

```bash
python tools/sql_skeletonizer.py backend/src/main/resources/db/migration/V1__create_loans_table.sql
python tools/sql_skeletonizer.py --dir backend/src/main/resources/db/migration --check
```

## Notas

- Ninguno modifica archivos: son de solo lectura/lectura-y-resumen.
- `project_map.py` importa `skeletonizer.py` como librería (mismo directorio).
- No cubren `.scss` ni `.html` porque en este repo los componentes Angular
  usan template/estilos inline en el `.ts`.
