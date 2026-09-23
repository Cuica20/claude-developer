# Checklists por área

## Dashboard (`/dashboard`)
- Stats cards (`.stat-card`) muestran total, pendientes, aprobadas, rechazadas, cartera —
  requiere backend levantado (`GET /api/loans/stats`)
- Si el backend no responde: `.alert-banner` visible con el mensaje de "Backend no disponible"
- Botón "+ Nueva solicitud" navega a `/loans`
- `<app-product-list />` carga sin errores en consola

## Solicitud de préstamo (`/loans`)
- Formulario (`form`) con los 7 campos: `applicantName`, `applicantEmail`, `birthDate`,
  `monthlyIncome`, `amount`, `termMonths`, `creditScore`
- Botón "Solicitar préstamo" deshabilitado mientras el formulario es inválido
  (`[disabled]="form.invalid || loading"`)
- Al violar una regla de negocio (RN-001 a RN-004), el `POST /api/loans` responde 400 y el
  `.field-error` correspondiente muestra el mensaje del servidor (no solo "Campo requerido")
- Ejemplos para forzar cada regla:
  - RN-001: `birthDate` con menos de 18 años
  - RN-002: `monthlyIncome` bajo respecto a `amount`/`termMonths`
  - RN-003: `creditScore` bajo para el `amount` solicitado
  - RN-004: `applicantEmail` terminado en `@moroso.test` (buró simulado)
- Con datos válidos: `POST /api/loans` responde 201 y se muestra `.success-view` con el N° de
  solicitud y la cuota mensual estimada
