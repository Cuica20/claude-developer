---
name: spec-driven-dev
description: >
  Guía para desarrollar features nuevas en LoanApp (backend Spring Boot o frontend Angular)
  usando spec-driven development: primero se escribe una especificación (requirements → design
  → tasks), se aprueba con el usuario en cada fase, y solo entonces se implementa. Úsala siempre
  que se pida construir, agregar o rediseñar una funcionalidad no trivial (un endpoint nuevo, un
  módulo, una pantalla, una entidad de dominio, un flujo de negocio) — no para bugfixes triviales,
  typos, o cambios de una línea. Dispara también si el usuario menciona "spec", "especificación",
  "spec-driven", "requirements", "plan de implementación" o "antes de programar quiero un plan".
---

# Spec-Driven Development para LoanApp

## Por qué

Este repo es un proyecto de capacitación: el código "roto" es intencional y cada módulo enseña un
patrón. Saltar directo a escribir código en un repo así tiende a reproducir el mismo problema que
se está enseñando a evitar (God classes, lógica sin tests, endpoints sin contrato claro). Escribir
la spec primero obliga a decidir el comportamiento y el contrato *antes* de que el código imponga
una solución por inercia, y le da al usuario un punto de aprobación barato antes de una tarea que
puede tocar varios archivos.

## Cuándo activarse

Actívate para cualquier feature o módulo nuevo con más de un archivo involucrado o con lógica de
negocio real: un endpoint nuevo, una entidad de dominio, un flujo (ej. aprobación de préstamo),
una pantalla Angular con su componente + servicio, una integración externa.

**No la uses** para: arreglar un bug puntual, renombrar algo, ajustar un estilo CSS, corregir un
test que falla, o cualquier cambio que el usuario ya describió con precisión total (ya no hay
ambigüedad que resolver con una spec).

Si tienes dudas sobre si aplica, pregunta al usuario en una frase — no asumas silenciosamente.

## Antes de empezar — revisar specs existentes

No abras un spec nuevo sobre algo que ya tiene uno. Antes de la Fase 1:

```bash
ls spec/
grep -ril "<palabra clave de la feature>" spec/
```

- Si encuentras un spec sobre el mismo módulo con `Estado: IMPLEMENTADO`, esto es una extensión,
  no una feature nueva — considera si conviene un feature-slug nuevo o seguir en el mismo.
- Si encuentras uno con `Estado: DRAFT` o `APROBADO` que nunca llegó a `IMPLEMENTADO`, es
  probablemente el que hay que retomar. Pregunta al usuario: ¿retomarlo tal cual, actualizarlo con
  el pedido actual, o de verdad es algo distinto y corresponde uno nuevo? Dos specs `APROBADO`
  vivos sobre el mismo módulo son dos contratos que pueden contradecirse.

## El flujo: 3 fases con aprobación explícita

Cada fase produce un documento en `spec/<feature-slug>/`. **No avances a la siguiente fase sin
que el usuario apruebe la actual explícitamente** ("sí", "dale", "aprobado", o una corrección que
tú aplicas y vuelves a presentar). Esto es lo más importante de la skill: el valor de spec-driven
development viene de los puntos de parada, no de los documentos en sí.

`<feature-slug>` es un nombre corto en kebab-case derivado del pedido del usuario, ej.
`aprobacion-prestamos`, `filtro-solicitudes-por-estado`.

Cada documento lleva una línea `Estado: DRAFT` justo debajo del título. Pasa a `APROBADO` cuando
el usuario aprueba esa fase; `requirements.md` y `design.md` no vuelven a cambiar de estado
después de eso, pero `tasks.md` pasa a `IMPLEMENTADO` cuando todas sus tareas quedan marcadas
`[x]`. Este es el mecanismo barato para que cualquiera (tú en una sesión futura, u otra persona)
sepa en qué quedó una feature con solo abrir la carpeta, sin releer el historial de chat.

### Fase 1 — Requirements (`spec/<feature-slug>/requirements.md`)

Qué debe hacer la feature y por qué, en lenguaje de negocio — todavía sin arquitectura ni nombres
de clases. Usa el template `references/requirements-template.md`. Escribe los criterios de
aceptación en formato EARS (When/If ... the system shall ...) porque son verificables sin
ambigüedad y se convierten directo en tests.

Investiga antes de preguntar: lo que ya se puede inferir del código existente o de los `CLAUDE.md`
del repo no se pregunta. Para lo que sí es ambiguo (reglas de negocio, casos borde, quién tiene
permiso de qué), pregunta — máximo 4 preguntas, todas juntas, no en rondas sucesivas. No inventes
una regla y la marques como asunción de pasada: una asunción no confirmada es una spec incorrecta
con apariencia de completa.

Al terminar, presenta y para:

```
Requirements escrito en: spec/<feature-slug>/requirements.md

Resumen:
- User stories: N
- Criterios de aceptación: N
- Asunciones sin confirmar: N (si hay alguna, listarlas)

¿Apruebas los requirements? Responde "sí" para pasar a diseño, o indícame qué ajustar.
```

**STOP acá. No escribas design.md todavía.**

### Fase 2 — Design (`spec/<feature-slug>/design.md`)

Cómo se va a construir, dado lo aprobado en requirements. Usa el template
`references/design-template.md`. Antes de escribir esta fase, lee las convenciones que ya rigen
el código que vas a tocar:

- Cambios de backend → lee `backend/CLAUDE.md` (estructura de paquetes `api/domain/infrastructure`,
  inyección por constructor, excepciones que extienden `BusinessRuleException`).
- Cambios de frontend → lee `frontend/CLAUDE.md` (componentes standalone, signals, BEM).
- Cualquier decisión de arquitectura no trivial que tome aquí es candidata a un ADR — si el
  proyecto ya usa `docs/adr/` para eso, menciónalo al usuario en vez de duplicar el criterio.

**Toda afirmación sobre el código existente se cita con `archivo:línea`.** Lo no verificado se
escribe como pregunta, no como hecho — es la diferencia entre un diseño que se sostiene y uno que
asume cosas que ya cambiaron.

El design debe fijar el contrato real: firmas de métodos/endpoints con ejemplos JSON concretos de
request/response, forma de los DTOs, modelo de datos o cambios de esquema, y qué pasa en cada caso
borde que salió en requirements. También decide si el cambio es compatible hacia atrás (un campo
nuevo opcional en una respuesta existente no rompe nada; renombrar o quitar uno sí) y qué
validación/observabilidad aplica (qué se valida y dónde, qué se loguea — ver el tema
`observabilidad` en el `CLAUDE.md` raíz si el cambio lo amerita). No hace falta pseudocódigo línea
por línea — lo que hace falta es que no queden decisiones de contrato abiertas para cuando alguien
empiece a picar código.

Al terminar, presenta y para:

```
Design escrito en: spec/<feature-slug>/design.md

Resumen:
- Endpoints nuevos/modificados: N
- Entidades/tablas afectadas: N
- Componentes frontend: N
- Cambios rompen compatibilidad: sí/no
- Riesgos identificados: N

¿Apruebas el design? Responde "sí" para pasar a tasks, o indícame qué ajustar.
```

**STOP acá. No escribas tasks.md todavía.**

### Fase 3 — Tasks (`spec/<feature-slug>/tasks.md`)

Checklist de implementación derivado del design ya aprobado. Usa el template
`references/tasks-template.md`. Cada tarea debe ser:

- **Atómica**: un cambio revisable de una sola vez, no "implementar todo el backend".
- **Trazable**: referencia qué parte de requirements/design cubre.
- **Verificable**: dice cómo se comprueba (qué test, qué comando `mvn test` / `npm test`).

Ordena las tareas en una secuencia ejecutable (ej. entidad → repositorio → servicio → controller →
tests → frontend), no por capa arbitraria.

Al terminar, presenta y para:

```
Tasks escrito en: spec/<feature-slug>/tasks.md

N tareas, orden: <resumen de la secuencia>

¿Apruebas el plan de tareas? Responde "sí" para empezar a implementar, o indícame qué ajustar.
```

**STOP acá. No escribas código todavía.**

### Implementación

Solo después de aprobar tasks.md empiezas a escribir código. Ve marcando cada tarea como hecha
(`- [x]`) en `tasks.md` a medida que la completas — así el documento sirve de tracker real del
avance, no solo de plan inicial. Cuando la última tarea quede marcada, cambia
`Estado: DRAFT` → `Estado: IMPLEMENTADO` en el encabezado de `tasks.md`.

Si durante la implementación descubres que design.md o requirements.md estaban equivocados o
incompletos, para, actualiza el documento correspondiente, y confírmalo con el usuario antes de
seguir — no arregles la spec calladamente después del hecho.

Los commits, ramas y push siguen las reglas generales de git del entorno (confirmación explícita
antes de cualquier acción que las requiera): esta skill no las cambia ni las automatiza.

## Notas

- Si el usuario ya trae una spec propia (pegada en el chat o en un archivo), no la reescribas
  desde cero: adáptala a los tres documentos y sigue el flujo de aprobación desde ahí.
- Si el usuario pide explícitamente saltarse el proceso ("sin spec, ve directo al código"),
  respeta eso — la skill no debe imponerse sobre una instrucción explícita en contra.
- Los documentos quedan versionados en git junto al código: son parte del historial del proyecto,
  no un scratchpad temporal.

## Lo que esta skill nunca debe hacer

- Escribir código antes de que `tasks.md` esté aprobado.
- Saltarse cualquiera de los tres STOP — son gates duros, no sugerencias.
- Hacer más de 4 preguntas por fase, o preguntar algo que ya está en un `CLAUDE.md` del repo.
- Crear un spec nuevo cuando ya existe uno sin implementar sobre el mismo módulo, sin antes
  preguntar al usuario qué hacer con el existente.
- Afirmar algo sobre el código existente en `design.md` sin haber abierto el archivo y citado
  `archivo:línea`.
- Marcar una asunción de negocio como si fuera un hecho confirmado.
- Corregir en silencio una desviación respecto de la spec aprobada durante la implementación.
- Crear ramas, hacer commits o push por su cuenta — eso lo rige el protocolo general de git, no
  esta skill.
