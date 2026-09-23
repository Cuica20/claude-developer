# Requirements — Validaciones de negocio en la solicitud de préstamo

Estado: APROBADO

## Contexto

El flujo de solicitud de préstamo desde el dashboard ya existe de punta a punta: el botón
"+ Nueva solicitud" (`dashboard.component.ts:19`) navega a `/loans`
(`app.routes.ts:17-22`), que carga `LoanFormComponent`
(`loan-form.component.ts`) y este llama a `LoanService.create()`
(`loan.service.ts:20-22`) contra `POST /api/loans`
(`LoanController.java:53-98`).

Lo que falta es lo marcado como problema intencional del Módulo 2: las reglas de negocio
(RN-001 a RN-003) están escritas **inline dentro de `LoanController.createLoan()`**
(`LoanController.java:56-83`), mezclando HTTP con lógica de negocio, sin reutilización ni
tests unitarios propios. El frontend además comenta una RN-004 (deuda vigente,
`loan-form.component.ts:14`) que no tiene ninguna implementación ni contrato en el backend.

Esta spec cubre extraer esas reglas a validadores independientes en `domain/validation/`
(según `backend/CLAUDE.md`), agregar RN-004 con un buró de crédito simulado, y exponer al
frontend errores de campo consistentes que ya sabe renderizar (`loan-form.component.ts:73-101`
usa `field-error` por campo).

## Alcance

**Incluye:**
- Extraer RN-001 (edad mínima), RN-002 (ratio ingreso/cuota) y RN-003 (score según monto) de
  `LoanController` a clases `Validator` independientes en `domain/validation/`.
- Agregar RN-004 (deuda vigente) como validador asíncrono, respaldado por un
  `CreditBureauService` **simulado** (sin integración externa real).
- Un orquestador (`LoanEligibilityValidator`, patrón Strategy) que corre las 4 reglas y agrega
  todos los errores de campo en una sola respuesta, igual que hoy.
- Mantener el contrato HTTP actual: `POST /api/loans` responde `400` con
  `{"fieldErrors": {...}}` cuando hay violaciones de negocio (no de formato/tipo, esas ya las
  cubre `@Valid` en `LoanRequest.java`).
- Tests unitarios por validador (`LoanServiceTest.java` hoy está incompleto — ver M3).

**No incluye:**
- Un buró de crédito real ni credenciales/integración externa (RN-004 es un stub).
- Cambios al formulario del frontend más allá de que siga mostrando los `field-error` que ya
  renderiza — no se agregan campos nuevos a `LoanRequest`.
- Guards de ruta / auth en `/loans` (eso es M4, ya marcado como TODO en `app.routes.ts:21`).
- Persistir el resultado del chequeo de buró en la tabla `loans` (RN-004 se evalúa en el momento
  de la solicitud, no se guarda historial).

## User stories

### US-1: Rechazo por edad mínima
Como solicitante, quiero que el sistema rechace mi solicitud si soy menor de 18 años, para
que no se procese un préstamo que no puedo contraer legalmente.

**Criterios de aceptación (EARS):**
- WHEN el solicitante tiene menos de 18 años (calculado desde `birthDate` a la fecha actual)
  THE SYSTEM SHALL rechazar la solicitud con `400` y el error de campo
  `birthDate: "RN-001: El solicitante debe ser mayor de 18 años"`.
- WHEN el solicitante tiene 18 años o más THE SYSTEM SHALL continuar evaluando el resto de
  reglas.

### US-2: Rechazo por ingreso insuficiente
Como analista de riesgo, quiero que el sistema exija un ingreso mensual de al menos 3 veces
la cuota mensual del préstamo, para reducir el riesgo de mora.

**Criterios de aceptación (EARS):**
- WHEN `monthlyIncome < (amount / termMonths) * 3` THE SYSTEM SHALL rechazar la solicitud con
  `400` y el error de campo `monthlyIncome` indicando el ingreso mínimo requerido.
- WHEN `monthlyIncome >= (amount / termMonths) * 3` THE SYSTEM SHALL continuar evaluando el
  resto de reglas.

### US-3: Rechazo por score insuficiente según monto
Como analista de riesgo, quiero exigir un score crediticio más alto para montos mayores a
50,000, para limitar la exposición en préstamos grandes.

**Criterios de aceptación (EARS):**
- WHEN `amount > 50000` AND `creditScore < 700` THE SYSTEM SHALL rechazar la solicitud con
  `400` y el error de campo `creditScore` indicando el score mínimo (700).
- WHEN `amount <= 50000` AND `creditScore < 600` THE SYSTEM SHALL rechazar la solicitud con
  `400` y el error de campo `creditScore` indicando el score mínimo (600).
- WHEN el score cumple el mínimo correspondiente THE SYSTEM SHALL continuar evaluando el resto
  de reglas.

### US-4: Rechazo por deuda vigente (buró simulado)
Como analista de riesgo, quiero verificar contra un buró de crédito si el solicitante tiene
deuda vigente en mora, para no aprobar préstamos a quien ya tiene problemas de pago.

**Criterios de aceptación (EARS):**
- WHEN el `CreditBureauService` (simulado) reporta que el solicitante tiene deuda vigente
  THE SYSTEM SHALL rechazar la solicitud con `400` y el error de campo
  `applicantEmail: "RN-004: El solicitante registra deuda vigente en el buró de crédito"`.
- WHEN el buró reporta que no tiene deuda vigente THE SYSTEM SHALL continuar evaluando el resto
  de reglas.
- IF el `CreditBureauService` simulado tarda o falla internamente THEN THE SYSTEM SHALL tratarlo
  como error 5xx (no como rechazo de negocio) — no se aprueba "por defecto" ante una falla del
  buró.

### US-5: Solicitud válida
Como solicitante, quiero recibir mi préstamo creado con estado `PENDING` cuando cumplo todas
las reglas, para saber que mi solicitud fue registrada.

**Criterios de aceptación (EARS):**
- WHEN se cumplen RN-001, RN-002, RN-003 y RN-004 THE SYSTEM SHALL crear el préstamo con
  `status = PENDING` y responder `201` con el `LoanResponse`, igual que el comportamiento
  actual (`LoanController.java:85-97`).

### US-6: Errores de negocio acumulados
Como solicitante, quiero ver todos los errores de mi solicitud a la vez (no uno por uno), para
corregir el formulario en un solo intento.

**Criterios de aceptación (EARS):**
- WHEN la solicitud viola más de una regla de negocio simultáneamente THE SYSTEM SHALL devolver
  todos los `fieldErrors` correspondientes en la misma respuesta `400`, igual que el
  comportamiento actual del bloque `errors` en `LoanController.java:58-82`.

## Reglas de negocio

- **RN-001** — Edad mínima 18 años, calculada con `Period.between(birthDate, hoy).getYears()`
  (igual que hoy en `LoanController.java:61`).
- **RN-002** — `monthlyIncome >= cuota * 3`, donde `cuota = amount / termMonths` redondeado a 2
  decimales con `HALF_UP` (igual que hoy en `LoanController.java:67-68`).
- **RN-003** — Score mínimo: 700 si `amount > 50000`, si no 600 (igual que hoy en
  `LoanController.java:75`).
- **RN-004** — ASUNCIÓN — confirmar: el `CreditBureauService` simulado determina "deuda
  vigente" de forma determinística para poder probar ambos caminos sin una BD externa. Regla
  propuesta: los solicitantes cuyo `applicantEmail` termina en `@moroso.test` se simulan **con**
  deuda vigente; cualquier otro email se simula **sin** deuda. Esto es solo para fines de
  entrenamiento/demo — se documentará como tal en el código (no es una regla de negocio real).

## Casos borde considerados

- Cuota calculada con división exacta vs. con decimales (ej. `amount=10000, termMonths=3` →
  `3333.33...`) — se mantiene el redondeo `HALF_UP` a 2 decimales ya usado hoy.
- `birthDate` en el límite exacto de 18 años (cumpleaños hoy) — cuenta como 18, no se rechaza
  (`Period.getYears()` ya resuelve esto correctamente hoy).
- `amount` exactamente en 50,000 — usa el umbral "más bajo" (600), porque la condición actual es
  estrictamente `> 50000`.
- Todas las reglas fallan a la vez — se devuelven los 4 `fieldErrors` juntos (US-6).
- El validador de buró (RN-004) es asíncrono por diseño (aunque el stub responda rápido), para
  que el patrón sea extensible a una integración real después — no debe bloquear el hilo HTTP
  principal de forma distinta a como ya lo hacen las demás reglas síncronas.

## Fuera de alcance / futuro

- Integración real con un buró de crédito externo (proveedor, autenticación, rate limits,
  timeouts reales) — RN-004 queda simulada hasta que se defina esa integración en una spec
  aparte.
- Guards de ruta / autenticación en `/loans` (M4).
- Persistencia de historial de chequeos de buró.
- Cambios al esquema de `LoanRequest`/`Loan` (no se agregan campos nuevos).
