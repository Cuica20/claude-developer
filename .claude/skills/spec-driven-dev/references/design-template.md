# Design — <nombre de la feature>

Estado: DRAFT

> Basado en `requirements.md` aprobado. Si algo aquí contradice o amplía ese documento, actualiza
> requirements.md primero y haz que el usuario lo confirme.
>
> Toda afirmación sobre el código existente en este documento se cita con `archivo:línea`. Lo no
> verificado se escribe como pregunta, no como hecho.

## Resumen de la solución
<2-4 frases: el enfoque elegido y por qué, si había alternativas obvias descartarlas en una línea.>

## Componentes afectados

### Backend (si aplica)
- **Dominio** (`domain/model`): entidades/value objects nuevos o modificados.
- **Servicio** (`domain/service`): lógica de negocio, firma de métodos públicos.
- **Validación** (`domain/validation`): reglas que se validan y dónde.
- **Excepciones** (`domain/exception`): nuevas excepciones, todas extendiendo `BusinessRuleException`.
- **API** (`api/controller`, `api/dto`): endpoints, verbos HTTP, request/response DTOs.
- **Persistencia** (`infrastructure/persistence`): repositorios, cambios de esquema/migración.

### Frontend (si aplica)
- **Componente(s)**: nombre, standalone, dónde vive.
- **Servicio(s)**: llamadas HTTP, manejo de estado (signals).
- **Rutas/guards**: si aplica.

## Contrato de API
Para cada endpoint nuevo o modificado, con ejemplo JSON real (no solo la forma abstracta):

```
<MÉTODO> <ruta>
Request:  <forma del DTO / query params>
{ "ejemplo": "request real" }

Response: <código HTTP>
{ "ejemplo": "response real" }

Errores:  <código HTTP> cuando <condición>
```

## Modelo de datos
<Cambios de entidad/tabla: campos nuevos, tipos, constraints, índices. Si hay migración Flyway,
nombre del archivo `V__*.sql` que le corresponde.>

## Compatibilidad
<¿Este cambio rompe algo que ya existe? Un campo nuevo opcional en una respuesta existente no
rompe nada; renombrar, quitar, o volver obligatorio un campo sí. Si rompe algo, dilo explícito y
qué consumidor (otro endpoint, componente Angular, test) se ve afectado — cítalo con
`archivo:línea`.>

## Validación y observabilidad
<Qué se valida y dónde (`domain/validation`, `@Valid` en el controller, etc.). Si el cambio lo
amerita: qué se loguea, qué métrica o alerta aplica (ver tema `observabilidad` en el `CLAUDE.md`
raíz). Si no aplica nada más allá de lo obvio, decirlo en una línea en vez de omitir la sección.>

## Casos borde → cómo se resuelven
<Retoma cada caso borde de requirements.md y dice explícitamente cómo lo maneja este diseño.>

## Decisiones de arquitectura
<Cualquier decisión no obvia (por qué este patrón y no otro, por qué esta librería). Si amerita
un ADR según `docs/adr/`, dilo aquí en vez de duplicar el contenido.>

## Riesgos / impacto en código existente
<Qué otro código se ve afectado o podría romperse; qué tests existentes hay que revisar. Cada
riesgo que dependa de cómo está hecho el código hoy va citado con `archivo:línea`.>
