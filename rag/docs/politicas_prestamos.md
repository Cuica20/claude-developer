# Políticas de Préstamos — LoanApp

## Requisitos del solicitante

### Edad mínima
El solicitante debe tener al menos **18 años** al momento de la solicitud.
Se verifica con la fecha de nacimiento registrada en el sistema.
Solicitudes de menores de 18 años son rechazadas automáticamente (código RN-001).

### Ingresos mínimos
La cuota mensual no puede superar el **35% del ingreso mensual declarado**.
Ejemplo: ingreso de $3.000/mes → cuota máxima de $1.050/mes.
Esta regla se aplica según la normativa RN-002.

### Credit Score
- Score ≥ 700: aprobación automática si se cumplen las demás reglas
- Score 600–699: revisión manual por el equipo de riesgo
- Score < 600: rechazo automático (código RN-003)

## Montos y plazos

### Montos disponibles
- Mínimo: $1.000
- Máximo: $500.000
- Unidad mínima de solicitud: $500

### Plazos disponibles
- Mínimo: 6 meses
- Máximo: 120 meses (10 años)
- Los plazos disponibles son: 6, 12, 24, 36, 48, 60, 84 y 120 meses

## Tasas de interés vigentes

| Plazo       | Tasa anual |
|-------------|-----------|
| 6–12 meses  | 8.5%      |
| 13–36 meses | 10.2%     |
| 37–60 meses | 11.8%     |
| 61–120 meses| 13.5%     |

Las tasas son fijas durante toda la vida del préstamo.
No existen tasas variables en el portafolio actual.

## Proceso de aprobación

### Tiempos
- Solicitudes de aprobación automática: máximo 2 horas hábiles
- Solicitudes de revisión manual: máximo 3 días hábiles
- Desembolso post-aprobación: 1 día hábil

### Estados del préstamo
1. **PENDING**: Solicitud recibida, en evaluación
2. **APPROVED**: Aprobado, pendiente de firma de contrato
3. **DISBURSED**: Fondos transferidos a la cuenta del solicitante
4. **REJECTED**: Solicitud rechazada

## Documentación requerida

El solicitante debe proveer durante el proceso de onboarding:
- Documento de identidad vigente
- Comprobante de ingresos (últimos 3 meses)
- Extracto bancario (últimos 6 meses)
- Comprobante de domicilio reciente

Los documentos se cargan en la plataforma en formato PDF.
LoanApp no acepta documentos en formatos editables (Word, Excel).
