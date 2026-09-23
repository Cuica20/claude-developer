# Tasks — Validaciones de negocio en la solicitud de préstamo

Estado: IMPLEMENTADO

> Basado en `design.md` aprobado. Marca cada tarea con `[x]` al completarla — este documento es
> el tracker de avance, no solo el plan inicial. Cuando todas las tareas queden marcadas, cambia
> `Estado: DRAFT` → `Estado: IMPLEMENTADO` arriba.

- [x] **T1 — Crear el contrato de validación (`FieldError`, `LoanValidator`, `AsyncLoanValidator`)**
  - Cubre: design "Componentes afectados → Validación"
  - Cambios: `backend/.../domain/validation/FieldError.java`,
    `backend/.../domain/validation/LoanValidator.java`,
    `backend/.../domain/validation/AsyncLoanValidator.java` (interfaces/record nuevos)
  - Verificación: `mvn compile` (sin lógica todavía, solo el contrato)

- [x] **T2 — RN-001: `AgeValidator`**
  - Cubre: requirements US-1 / design "AgeValidator"
  - Cambios: `backend/.../domain/validation/AgeValidator.java` (nuevo, misma lógica que
    `LoanController.java:61-64`)
  - Verificación: nuevo `AgeValidatorTest.java` — casos: menor de 18, exactamente 18 (cumpleaños
    hoy), mayor de 18. `mvn test -Dtest=AgeValidatorTest`

- [x] **T3 — RN-002: `IncomeRatioValidator`**
  - Cubre: requirements US-2 / design "IncomeRatioValidator"
  - Cambios: `backend/.../domain/validation/IncomeRatioValidator.java` (nuevo, misma lógica que
    `LoanController.java:67-72`)
  - Verificación: nuevo `IncomeRatioValidatorTest.java` — casos: ingreso insuficiente, ingreso
    exacto al mínimo, ingreso suficiente, cuota con decimales infinitos (ej. `amount=10000,
    termMonths=3`). `mvn test -Dtest=IncomeRatioValidatorTest`

- [x] **T4 — RN-003: `CreditScoreValidator`**
  - Cubre: requirements US-3 / design "CreditScoreValidator"
  - Cambios: `backend/.../domain/validation/CreditScoreValidator.java` (nuevo, misma lógica que
    `LoanController.java:74-79`)
  - Verificación: nuevo `CreditScoreValidatorTest.java` — casos: score bajo con monto <= 50000,
    score bajo con monto > 50000, `amount` exactamente en 50000 (usa umbral 600), score
    suficiente. `mvn test -Dtest=CreditScoreValidatorTest`

- [x] **T5 — RN-004: `CreditBureauService` (interfaz) + `StubCreditBureauService` (stub) + `CreditBureauValidator`**
  - Cubre: requirements US-4 / design "Servicio", "Infraestructura", "Validación"
  - Cambios: `backend/.../domain/service/CreditBureauService.java` (interfaz nueva),
    `backend/.../infrastructure/creditbureau/StubCreditBureauService.java` (nuevo, con Javadoc de
    "stub de entrenamiento"), `backend/.../domain/validation/CreditBureauValidator.java` (nuevo),
    `backend/.../domain/exception/CreditBureauUnavailableException.java` (nuevo)
  - Verificación: nuevo `CreditBureauValidatorTest.java` (mockeando `CreditBureauService`) —
    casos: con deuda (`Optional<FieldError>` presente), sin deuda (`Optional.empty()`), y falla
    interna del servicio propaga `CreditBureauUnavailableException`. Nuevo
    `StubCreditBureauServiceTest.java` — verifica la regla determinística
    (`@moroso.test` → `true`, cualquier otro email → `false`).
    `mvn test -Dtest=CreditBureauValidatorTest,StubCreditBureauServiceTest`

- [x] **T6 — Orquestador `LoanEligibilityValidator`**
  - Cubre: requirements US-5, US-6 / design "LoanEligibilityValidator", "Casos borde"
  - Cambios: `backend/.../domain/validation/LoanEligibilityValidator.java` (nuevo) — recibe
    `List<LoanValidator>` y `List<AsyncLoanValidator>` por constructor, corre síncronas +
    asíncrona en paralelo (async primero, `.join()` al final), agrega `Map<String, String>`
  - Verificación: nuevo `LoanEligibilityValidatorTest.java` (con validadores fake/mock) — casos:
    ninguna regla falla (mapa vacío), una regla falla, las 4 reglas fallan a la vez (US-6), la
    asíncrona falla con excepción → se propaga sin capturarse como error de campo.
    `mvn test -Dtest=LoanEligibilityValidatorTest`

- [x] **T7 — Integrar el orquestador en `LoanController` y usar `ErrorResponse.ofFields`**
  - Cubre: requirements US-5, US-6 / design "API", "Compatibilidad"
  - Cambios: `backend/.../api/controller/LoanController.java` — constructor recibe
    `LoanEligibilityValidator` además de `OrderService`; se reemplaza el bloque
    `Map<String,String> errors = ...` (líneas 56-83) por la llamada al orquestador; la respuesta
    400 usa `ErrorResponse.ofFields(400, fieldErrors)` en vez del `Map.of("fieldErrors", errors)`
    manual
  - Verificación: test de integración `LoanControllerTest.java` (nuevo, con
    `@WebMvcTest` o `MockMvc` + mocks de `OrderService`/`LoanEligibilityValidator`) — casos:
    `POST /api/loans` con datos que violan varias reglas devuelve 400 con el shape completo de
    `ErrorResponse` y todos los `fieldErrors` esperados; datos válidos devuelve 201 sin cambios.
    `mvn test -Dtest=LoanControllerTest`

- [x] **T8 — Frontend: mostrar errores de campo del servidor en `loan-form.component.ts`**
  - Cubre: requirements US-6 / design "Frontend"
  - Cambios: `frontend/.../loan-form/loan-form.component.ts` — en el callback `error` de
    `onSubmit()`, mapear `err.error?.fieldErrors` a `setErrors({ server: msg })` en cada
    `FormControl`; agregar el `span.field-error` faltante para `monthlyIncome`; extender los 4
    spans existentes (`applicantName` no aplica — no tiene regla de negocio, queda igual) para
    mostrar `f['x'].errors?.['server']`
  - Verificación: nuevo spec `loan-form.component.spec.ts` (o extender uno existente si aparece)
    con `HttpTestingController` — casos: respuesta 400 con `fieldErrors` puebla los mensajes por
    campo; respuesta 500 (buró caído) muestra el banner genérico `errorMessage`.
    `npm test -- --watch=false`

## Orden sugerido

T1 → (T2, T3, T4, T5 en paralelo — son validadores independientes) → T6 (depende de T1-T5) → T7
(depende de T6) → T8 (depende de T7, necesita el shape real de `ErrorResponse` del backend).

## Definición de hecho (para toda la feature)

- [x] Todos los criterios de aceptación de `requirements.md` (US-1 a US-6) tienen un test que los cubre
- [x] `mvn test` (backend, tests nuevos) y `npm test -- --watch=false` (frontend) pasan
- [x] No quedan asunciones sin confirmar de `requirements.md` (RN-004 ya fue confirmada: stub por
  dominio de email `@moroso.test`)
