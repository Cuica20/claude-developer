# Design — Validaciones de negocio en la solicitud de préstamo

Estado: APROBADO

> Basado en `requirements.md` aprobado. Si algo aquí contradice o amplía ese documento, actualiza
> requirements.md primero y haz que el usuario lo confirme.

## Resumen de la solución

Se extraen las 3 reglas inline de `LoanController.createLoan()`
(`LoanController.java:56-83`) a clases `Validator` independientes en `domain/validation/`,
más una cuarta (RN-004) respaldada por un `CreditBureauService` simulado. Un orquestador
(`LoanEligibilityValidator`, patrón Strategy) reemplaza el bloque de validación inline: corre
las reglas síncronas, dispara la asíncrona en paralelo, agrega todos los errores de campo, y el
controller usa el `ErrorResponse` DTO ya existente (`ErrorResponse.java:20-22`) para responder —
hoy el controller construye su propio `Map.of("fieldErrors", errors)` a mano
(`LoanController.java:82`), sin pasar por `ErrorResponse`/`GlobalExceptionHandler`; este diseño
corrige esa inconsistencia sin tocar el contrato de `MethodArgumentNotValidException`, que ya usa
`ErrorResponse.ofFields` (`GlobalExceptionHandler.java:28-36`).

Alternativa descartada: usar `BusinessRuleException` (lanzar excepción en el primer validador que
falle). Se descarta porque US-6 exige acumular **todos** los errores en una sola respuesta —
`BusinessRuleException` solo modela un fallo a la vez (`ruleId` + `field` únicos,
`BusinessRuleException.java:8-9`) y su handler devuelve un solo `ruleId`/`message`
(`GlobalExceptionHandler.java:21-26`), no un mapa de campos.

## Componentes afectados

### Backend

**Validación** (`domain/validation/` — paquete nuevo):
- `FieldError.java` — record `(String field, String ruleId, String message)`.
- `LoanValidator.java` — interfaz: `Optional<FieldError> validate(LoanRequest request)`.
- `AsyncLoanValidator.java` — interfaz: `CompletableFuture<Optional<FieldError>> validateAsync(LoanRequest request)`.
- `AgeValidator.java` — `@Component implements LoanValidator` (RN-001). Misma lógica que
  `LoanController.java:61-64` (`Period.between(birthDate, LocalDate.now()).getYears() < 18`).
- `IncomeRatioValidator.java` — `@Component implements LoanValidator` (RN-002). Misma lógica que
  `LoanController.java:67-72`.
- `CreditScoreValidator.java` — `@Component implements LoanValidator` (RN-003). Misma lógica que
  `LoanController.java:74-79`.
- `CreditBureauValidator.java` — `@Component implements AsyncLoanValidator` (RN-004). Depende de
  `CreditBureauService` (inyectado por constructor).
- `LoanEligibilityValidator.java` — `@Component`, orquestador. Constructor recibe
  `List<LoanValidator>` y `List<AsyncLoanValidator>` (Spring inyecta todos los beans que
  implementan cada interfaz — así una regla nueva solo requiere un `@Component` más, sin tocar
  el orquestador). Expone `Map<String, String> validate(LoanRequest request)`.

**Servicio** (`domain/service/` — nuevo archivo, sin tocar `OrderService.java`, que es el
problema intencional del Módulo 1 y queda fuera de esta spec):
- `CreditBureauService.java` — interfaz: `CompletableFuture<Boolean> hasActiveDebt(String applicantEmail)`.

**Infraestructura** (`infrastructure/creditbureau/` — paquete nuevo, sigue la regla de
`backend/CLAUDE.md` de que el dominio no depende de infraestructura directamente):
- `StubCreditBureauService.java` — `@Component implements CreditBureauService`. Simulado: sin
  llamada HTTP real. Javadoc explícito: *"Stub de entrenamiento — reemplazar por un cliente real
  antes de producción"*.

**Excepciones** (`domain/exception/` — un archivo nuevo):
- `CreditBureauUnavailableException.java extends RuntimeException` (no `BusinessRuleException`,
  porque no es un rechazo de negocio sino una falla del sistema — US-4, caso IF). Se deja caer al
  handler genérico `GlobalExceptionHandler.handleGeneral()` (`GlobalExceptionHandler.java:50-55`),
  que ya responde 500 sin cambios adicionales al handler.

**API** (`api/controller/LoanController.java`):
- Se reemplaza el bloque `Map<String, String> errors = ...` (líneas 56-83) por una llamada a
  `loanEligibilityValidator.validate(req)`.
- Constructor pasa de recibir solo `OrderService` (`LoanController.java:35-39`) a recibir también
  `LoanEligibilityValidator` (inyección por constructor, sin `@Autowired` en campo, según
  `backend/CLAUDE.md`).
- La respuesta de error usa `ErrorResponse.ofFields(400, fieldErrors)` en vez del `Map.of(...)`
  manual actual.

### Frontend

- **Componente** `loan-form.component.ts` (`features/loan/loan-form/`): en el callback `error`
  de `onSubmit()` (línea 202-205 hoy), si `err.error?.fieldErrors` viene presente, mapear cada
  entrada a su `FormControl` correspondiente con `this.form.get(field)?.setErrors({ server: msg })`,
  además de mantener el `errorMessage` genérico como fallback (para errores 500, ej. buró caído).
- Los 4 `<span class="field-error">` que ya existen para `applicantName`, `applicantEmail`,
  `birthDate`, `creditScore` (`loan-form.component.ts:73-101,117-122`) se extienden para también
  mostrar `f['x'].errors?.['server']` — coincide exactamente con los 4 campos que usan RN-001 a
  RN-004 (`birthDate`, `monthlyIncome`, `creditScore`, `applicantEmail`). Se agrega un
  `field-error` nuevo para `monthlyIncome` (hoy no tiene ninguno, línea 96-101).
- **Servicio** `loan.service.ts`: sin cambios — `create()` ya propaga el `HttpErrorResponse` tal
  cual (`loan.service.ts:20-22`).
- **Rutas**: sin cambios.

## Contrato de API

```
POST /api/loans
Request:
{
  "applicantName": "María García",
  "applicantEmail": "maria@moroso.test",
  "birthDate": "2010-05-01",
  "monthlyIncome": 1500,
  "creditScore": 550,
  "amount": 60000,
  "termMonths": 36
}

Response: 400 (antes: {"fieldErrors": {...}} sin envolver — ver Compatibilidad)
{
  "status": 400,
  "error": "ValidationError",
  "message": "Errores de validación",
  "ruleId": null,
  "fieldErrors": {
    "birthDate": "RN-001: El solicitante debe ser mayor de 18 años",
    "monthlyIncome": "RN-002: Ingreso insuficiente. Minimo requerido: 5555.56",
    "creditScore": "RN-003: Score insuficiente. Minimo: 700",
    "applicantEmail": "RN-004: El solicitante registra deuda vigente en el buró de crédito"
  },
  "timestamp": "2026-09-22T10:00:00"
}

Response: 201 (solicitud válida — sin cambios respecto a hoy, LoanController.java:97)
{ "id": 42, "applicantName": "...", "status": "PENDING", "monthlyInstallment": 1850.32, ... }

Response: 500 (si el CreditBureauService simulado lanza una falla interna)
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error interno del servidor",
  "ruleId": null,
  "fieldErrors": null,
  "timestamp": "2026-09-22T10:00:00"
}
```

## Modelo de datos

Sin cambios. `Loan` (`Loan.java`) y la tabla `loans` no se modifican — el resultado del chequeo
de buró no se persiste (confirmado en Alcance/No incluye de `requirements.md`).

## Compatibilidad

- **No rompe** el DTO `LoanRequest` (`LoanRequest.java`) ni `LoanResponse` — mismos campos,
  mismas validaciones de formato (`@Valid`).
- **Cambia la forma del body de error 400 por regla de negocio**: hoy es
  `{"fieldErrors": {...}}` plano (`LoanController.java:82`); pasa a ser el `ErrorResponse`
  completo (`status/error/message/ruleId/fieldErrors/timestamp`). El frontend hoy no lee ninguno
  de esos campos aparte de un intento de `err.error?.message` que siempre caía al fallback
  genérico "Error al enviar la solicitud" (`loan-form.component.ts:203`), porque `message` no
  existía en el body viejo — es decir, el frontend actual **no depende** de la forma vieja, así
  que este cambio no rompe nada existente, solo mejora la información disponible. El código
  frontend nuevo (ver arriba) sí empieza a leer `fieldErrors`.
- El endpoint `GET /api/loans/stats` y `GET /api/loans` no se tocan.

## Validación y observabilidad

- Las 3 reglas síncronas corren en el hilo de la request (igual costo que hoy, solo movidas de
  lugar). La regla asíncrona (RN-004) se dispara con `CompletableFuture.supplyAsync(...)` sobre
  el pool común (`ForkJoinPool.commonPool()`) mientras las síncronas ya corrieron — no se
  serializa innecesariamente.
- `LoanEligibilityValidator.validate()` loguea a nivel `WARN` un resumen de qué reglas fallaron
  por solicitud (mismo nivel que hoy usa el controller implícitamente vía
  `log.info` en el éxito, `LoanController.java:96`) — para fallos se agrega
  `log.warn("Solicitud rechazada por reglas de negocio: {}", fieldErrors.keySet())`.
- Si `CreditBureauService` falla, se loguea `ERROR` con la excepción antes de relanzar como
  `CreditBureauUnavailableException`, para diferenciarlo en logs de un rechazo de negocio normal.

## Casos borde → cómo se resuelven

- **Cuota con decimales infinitos** (`amount=10000, termMonths=3`): `IncomeRatioValidator`
  reutiliza exactamente el mismo cálculo con `RoundingMode.HALF_UP` a 2 decimales que hoy
  (`LoanController.java:67-68`) — sin cambio de comportamiento numérico.
- **`birthDate` en el límite exacto de 18 años**: `AgeValidator` reutiliza `Period.getYears()`
  sin cambios (`LoanController.java:61`) — ya resuelve el caso correctamente hoy.
- **`amount` exactamente en 50,000**: `CreditScoreValidator` mantiene la comparación estricta
  `amount.compareTo(BigDecimal.valueOf(50_000)) > 0` (`LoanController.java:75`) — 50,000 exacto
  usa el umbral de 600, igual que hoy.
- **Todas las reglas fallan a la vez**: `LoanEligibilityValidator` no hace fail-fast — corre las
  4 reglas siempre y agrega todos los `Optional<FieldError>` presentes en un solo `Map`.
- **RN-004 asíncrona no debe bloquear las síncronas**: el orquestador dispara
  `creditBureauValidator.validateAsync(req)` primero (retorna de inmediato un
  `CompletableFuture`), luego corre las 3 síncronas, y al final hace `.join()` sobre el future
  antes de agregar su resultado — así el tiempo total es `max(síncronas, asíncrona)`, no la suma.

## Decisiones de arquitectura

- **Strategy vía `List<Interface>` autoinyectado por Spring** en vez de un `switch`/`if` en el
  orquestador: agregar una regla nueva (ej. RN-005 futura) es agregar una clase `@Component`, sin
  tocar `LoanEligibilityValidator`. Es el patrón que el propio TODO del controller pide
  (`LoanController.java:28`: "Patrón recomendado: Strategy + LoanEligibilityValidator").
- **`CompletableFuture` explícito en vez de `@Async`/`@EnableAsync`**: el proyecto no tiene
  `@EnableAsync` configurado hoy (verificado — no hay ninguna ocurrencia en el código). Usar
  `CompletableFuture.supplyAsync` evita agregar configuración global de Spring solo para un stub
  de entrenamiento; si más adelante se agregan más validadores asíncronos reales, ahí sí conviene
  revisar un `TaskExecutor` dedicado (fuera de alcance).
- **Stub de buró en `infrastructure/creditbureau/` implementando una interfaz de `domain/service/`**:
  sigue la regla de `backend/CLAUDE.md` ("los servicios NO dependen de clases de infraestructura
  directamente, usa interfaces"), aunque hoy no haya una integración real — deja el punto de
  extensión ya armado para cuando la haya.

## Riesgos / impacto en código existente

- `LoanController.java:35-39` cambia su constructor (nuevo parámetro
  `LoanEligibilityValidator`) — no rompe nada porque Spring resuelve la inyección, pero cualquier
  test que instancie `LoanController` manualmente (no encontrado ninguno hoy en
  `backend/src/test/`) tendría que actualizarse.
- `LoanServiceTest.java` (`LoanServiceTest.java:26-59`) prueba `OrderService`, no
  `LoanController` ni las reglas de negocio — no se ve afectado por este cambio, pero sigue
  siendo el problema intencional del Módulo 3 (tests incompletos), fuera de alcance aquí.
- El stub `StubCreditBureauService` es determinístico por dominio de email
  (`@moroso.test` → con deuda). Riesgo pedagógico: si un alumno usa ese dominio "sin querer" en
  una prueba manual, el 400 puede parecer un bug — se documenta con Javadoc explícito en la
  clase.
