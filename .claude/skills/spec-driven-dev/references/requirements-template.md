# Requirements — <nombre de la feature>

Estado: DRAFT

## Contexto
<1-3 frases: qué problema resuelve, quién lo pidió/lo usa, por qué ahora.>

## Alcance
**Incluye:** <qué sí cubre esta feature>
**No incluye:** <qué queda explícitamente afuera — evita scope creep durante la implementación>

## User stories

### US-1: <título corto>
Como <rol>, quiero <acción>, para <beneficio>.

**Criterios de aceptación (EARS):**
- WHEN <evento/condición> THE SYSTEM SHALL <comportamiento observable>
- IF <condición de borde o error> THEN THE SYSTEM SHALL <comportamiento>

### US-2: <título corto>
...

## Reglas de negocio
<Reglas que no son obvias del CRUD básico: validaciones, permisos, estados, cálculos. Si alguna
regla fue una asunción tuya (no confirmada por el usuario), márcala explícitamente como
"ASUNCIÓN — confirmar" en vez de mezclarla con las reglas confirmadas.>

## Casos borde considerados
- <caso borde 1 y qué debe pasar>
- <caso borde 2 y qué debe pasar>

## Fuera de alcance / futuro
<Cosas que se mencionaron pero se decidió no hacer ahora, para que no se pierdan pero tampoco
bloqueen esta iteración.>
