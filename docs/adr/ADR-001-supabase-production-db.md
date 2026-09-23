# ADR-001: Supabase como Base de Datos de Producción

## Estado
Aceptado

## Contexto
LoanApp necesita una base de datos PostgreSQL en producción. El equipo es pequeño (2-4 personas), el presupuesto es limitado y no queremos gestionar infraestructura de base de datos propia.

## Decisión
Usar **Supabase** como proveedor de PostgreSQL administrado para el ambiente de producción.

## Consecuencias

### Positivas
- Backups automáticos incluidos
- SSL/TLS configurado por defecto
- Panel de administración web (alternativa a pgAdmin)
- Tier gratuito suficiente para el curso
- Row Level Security disponible si se necesita

### Negativas
- Vendor lock-in a Supabase
- El tier gratuito pausa la BD tras 1 semana de inactividad
- Latencia depende de la región del proyecto

## Alternativas consideradas
- **PostgreSQL en Railway**: similar costo, menos funcionalidades de seguridad
- **PostgreSQL propio en VPS**: requiere gestión de backups y parches
- **H2 en producción**: no viable para datos reales (in-memory)
