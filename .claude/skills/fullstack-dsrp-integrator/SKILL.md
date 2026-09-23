---
name: fullstack-dsrp-integrator
description: Orchestrate changes that span backend and frontend. Use when a request touches both a Spring endpoint/DTO and an Angular service/template. Delegates to backend and frontend domain skills after contract alignment.
triggers:
  keywords:
    - contrato DTO
    - sincronizar frontend backend
    - el backend devuelve
    - el frontend espera
    - campo falta en DTO
    - rollout
    - campo nuevo en entidad
    - nuevo campo en respuesta
    - agrega el campo al frontend
    - agrega el campo al backend
---

# Fullstack DSRP Integrator (Workspace)

## Purpose

Prevent contract mismatches when a change crosses the backend/frontend boundary.

## When to activate

- A new entity field must appear in the frontend.
- A backend DTO shape changes and the Angular service must be updated.
- A new endpoint must be consumed by an Angular service.
- A CRM or dashboard feature requires both a backend controller and a frontend component.
- The error is a mismatch: backend returns field X but frontend expects field Y.

## Workflow

1. **Check recent history first**
   ```bash
   cd backend && git log --oneline -10
   cd frontend && git log --oneline -10
   ```

2. **Identify the contract — no investigar a mano.**

   Si el trabajo viene de un spec `APROBADO` (ver skill `spec-driven-dev`), **el contrato ya
   existe**: está en `design.md`, sección "Contrato de API". Usarlo tal cual — no re-mapear lo que
   el spec ya fijó con firmas de DTO y ejemplos JSON.

   Si no hay spec aprobado, mapear el contrato leyendo directamente:
   ```
   backend: api/dto/<X>Request.java, api/dto/<X>Response.java, api/controller/<X>Controller.java
   frontend: shared/models/<x>.model.ts, shared/services/<x>.service.ts
   ```

3. **Map the gap** — nadie más lo hace por vos.
   - List fields present in backend DTO but absent in TS interface (or vice versa).
   - Mark each field as required vs optional.
   - Verify endpoint path and HTTP method are identical on both sides.

4. **Implement backend first**
   - Controller en `api/controller/`, DTOs en `api/dto/`, lógica de negocio en `domain/service/`
     o `domain/validation/` (ver `backend/CLAUDE.md`).

5. **Align frontend**
   - Update `shared/models/<x>.model.ts` if the field belongs there.
   - Update the Angular service (`shared/services/<x>.service.ts`) return type.
   - Update template bindings; use `*ngIf` or an explicit guard for optional fields.

6. **Verificar antes del commit.**
   - `cd backend && mvn test`
   - `cd frontend && npm test -- --watch=false`
   - Si el cambio toca una ruta pública/protegida, revisar el paso de Security check abajo.

7. **Report integration risk**

## Contract checklist

- Endpoint path and HTTP method are explicit and identical between backend and Angular service.
- Response fields used in template are typed (no `any`).
- Optional fields have `?` in the TS interface.
- New required fields are not added to existing response DTOs without coordinating rollout.
- If only one side was updated, report the remaining integration risk explicitly.

## Security check

Este repo hoy tiene `SecurityConfig.java` con `anyRequest().permitAll()` (problema intencional del
Módulo 4 — ver `CLAUDE.md` raíz, tabla de problemas por módulo). Mientras M4 no esté implementado,
cualquier endpoint nuevo hereda ese `permitAll()` sin configuración adicional — no hay
`requestMatchers` que ajustar todavía. Una vez que M4 agregue JWT + RBAC, este paso debe volver a
exigir una regla explícita por ruta antes de cualquier matcher genérico `/api/**`.
