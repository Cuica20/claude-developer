# Tasks — <nombre de la feature>

Estado: DRAFT

> Basado en `design.md` aprobado. Marca cada tarea con `[x]` al completarla — este documento es
> el tracker de avance, no solo el plan inicial. Cuando todas las tareas queden marcadas, cambia
> `Estado: DRAFT` → `Estado: IMPLEMENTADO` arriba.

- [ ] **T1 — <título atómico>**
  - Cubre: requirements US-<n> / design <sección>
  - Cambios: `<archivo(s) a crear o modificar>`
  - Verificación: `<comando o test que lo confirma, ej. "mvn test -Dtest=LoanServiceTest">`

- [ ] **T2 — <título atómico>**
  - Cubre: ...
  - Cambios: ...
  - Verificación: ...

- [ ] **T3 — <título atómico>**
  - ...

## Orden sugerido
<Si el orden no es simplemente T1→T2→T3, o hay tareas que se pueden hacer en paralelo, acláralo
aquí (ej. "T4 y T5 son independientes, T6 depende de ambas").>

## Definición de hecho (para toda la feature)
- [ ] Todos los criterios de aceptación de `requirements.md` tienen un test que los cubre
- [ ] `mvn test` / `npm test -- --watch=false` pasan
- [ ] No quedan asunciones sin confirmar de `requirements.md`
