---
name: dsrp-entity-generator
description: >-
  Genera la cadena completa entidad JPA -> repository -> service -> DTO -> controller ->
  interfaz TypeScript para LoanApp, validando primero contra las migraciones Flyway existentes
  los riesgos DDL (NOT NULL sin default, renombre = pérdida de datos).
  Triggers: "nueva entidad", "nueva tabla", "agregar campo a", "crea el CRUD de".
---

# Skill: dsrp-entity-generator (LoanApp)

## Rol
Convertir las reglas de `backend/CLAUDE.md` (controladores sin lógica de negocio, inyección por
constructor, DTO nunca expone la entidad, excepciones que extienden `BusinessRuleException`) en
un checklist ejecutable — no solo texto que se puede pasar por alto.

## Cuándo NO usarlo
- Si el usuario pide **solo** la entidad (sin CRUD completo), confirmar alcance antes de generar
  los 6 pasos — no sobre-generar.
- Si el contrato es no trivial (nuevo dominio, integración externa, reglas de negocio con
  ambigüedad), primero la skill `spec-driven-dev`, luego este skill implementa lo ya aprobado.

## Pre-flight obligatorio (antes de proponer código)

### 1. Existencia
En este orden:
1. `grep -ril "<campo o entidad>" backend/src/main/java/com/capacitacion/loanapp/domain/model/`
2. `ls backend/src/main/resources/db/migration/` — leer las migraciones existentes
   (`V1__create_loans_table.sql`, `V2__fix_data_types_and_indexes.sql`, …) para ver el esquema real.

Si la entidad/campo ya existe → reportar su ubicación y **detener**. No proponer duplicado.

### 2. Riesgo DDL — este proyecto usa Flyway, no `ddl-auto=update`

`application.yml` tiene `ddl-auto: none` — el esquema lo controlan las migraciones en
`db/migration/`, no Hibernate. Todo cambio de esquema requiere una migración nueva
`V{N+1}__<descripcion>.sql` (siguiente número tras la última existente).

| Operación | Riesgo | Acción |
|---|---|---|
| Agregar columna nullable | Seguro | Migración con `ADD COLUMN ... NULL` |
| Agregar columna NOT NULL sin default | **Falla al aplicar la migración sobre datos existentes** | Agregar con default, o en dos migraciones (nullable → backfill → NOT NULL) |
| Renombrar columna | **Pérdida de datos si no se migra el valor** | Nunca `DROP` + `ADD`; usar `RENAME COLUMN` o migrar el dato explícitamente. **Bloquear y reportar si el pedido implica perder datos.** |
| Cambiar tipo de columna | Riesgo | Verificar compatibilidad contra los datos de seed (`V99__` si existe, o `DataInitializer`) |
| Eliminar columna | **Pérdida de datos** | Reportar al usuario antes de proceder |

No hay una BD viva accesible por MCP para este proyecto (a diferencia de `mysql-dsrp`) — la
verificación se hace leyendo las migraciones y, si hace falta correrla, con
`mvn spring-boot:run` contra el H2 de dev o revisando `docker-compose.yml` (perfil con
PostgreSQL).

### 3. Naming
Seguir la convención ya usada en `V1__create_loans_table.sql`: nombres de tabla y columna en
`snake_case` minúsculas (`loans`, `applicant_name`), no PascalCase. Confirmar contra la migración
más reciente antes de asumir.

## Cadena generada (completa, con una sola puerta al final)

Los 6 artefactos se generan **de corrido**, sin pedir aprobación entre paso y paso — esta skill
solo se activa cuando el contrato ya está definido (por un spec aprobado o porque el pedido es
trivial), así que la cadena es mecánica.

**La puerta va al final, y es informada:** con los 6 archivos escritos, correr:
```bash
cd backend && mvn test
cd frontend && npm test -- --watch=false
```
Si algo falla, corregir antes de dar la tarea por terminada — no se commitea con tests rotos.

Dos excepciones donde **sí** se para antes de generar:
- **Renombre o eliminación de columna con pérdida de datos** → bloqueo duro del pre-flight. Nunca
  se genera sin confirmación explícita.
- **El usuario pidió solo la entidad**, no el CRUD → confirmar alcance antes de generar los 6.

| Paso | Artefacto | Convención (ver `backend/CLAUDE.md` / `frontend/CLAUDE.md`) |
|---|---|---|
| 1 | `db/migration/V{N}__<descripcion>.sql` | snake_case, siguiente número tras la última migración |
| 2 | `domain/model/<Entidad>.java` | POJO con Lombok (`@Data @Builder`), sin lógica de negocio |
| 3 | `infrastructure/persistence/<Entidad>Repository.java` | `JpaRepository<Entidad, Long>` |
| 4 | `domain/service/<Entidad>Service.java` | lógica de negocio, inyección por constructor (nunca `@Autowired` en campo) |
| 5 | `api/dto/<Entidad>Request.java` + `api/dto/<Entidad>Response.java` | records, nunca exponer la entidad directamente |
| 6 | `api/controller/<Entidad>Controller.java` | solo `@GetMapping`/`@PostMapping`, sin lógica de negocio |
| 7 | `frontend/shared/models/<entidad>.model.ts` + `frontend/shared/services/<entidad>.service.ts` | interfaz TS + service Angular standalone |

Backend primero, frontend después.

## Bloqueo de renombre/eliminación con pérdida de datos (verificado)

Si el pre-flight detecta que la solicitud es un renombre o eliminación de una columna existente
(no un campo nuevo):
1. Detener inmediatamente, antes de generar cualquier archivo.
2. Reportar: columna actual, cambio propuesto, y qué migración la creó originalmente.
3. Proponer alternativa: agregar la columna nueva en una migración, migrar el dato con `UPDATE`
   en la misma o siguiente migración, deprecar la columna vieja después (no eliminarla de
   inmediato).

## Seguridad
- Este repo tiene `SecurityConfig.java` con `anyRequest().permitAll()` (problema intencional del
  Módulo 4, ver `CLAUDE.md` raíz) — no hay reglas de autorización por ruta que ajustar todavía.
  Cuando M4 implemente JWT + RBAC, este paso debe agregar la regla explícita para el nuevo
  controller antes de cualquier matcher genérico `/api/**`.

## Integración
- Si el contrato no es trivial (nuevo dominio, reglas de negocio con ambigüedad), usar primero la
  skill `spec-driven-dev` de este repo — no generar código sobre un contrato sin aprobar.

## Fuera de alcance (v1)
- Componentes Angular completos (formularios, listas) — solo la interfaz TS y el service; la UI
  se arma a mano o con otra skill.
- Tests automáticos generados en cadena — se agregan como parte del checklist de cada paso, no
  como un paso 8 separado.
