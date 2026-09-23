# ADR-002: JWT para Autenticación Stateless

## Estado
Aceptado

## Contexto
LoanApp expone una API REST consumida por un frontend Angular. Necesitamos autenticación que funcione bien con el modelo stateless de REST y con el deploy separado de backend (Railway/Render) y frontend (Vercel).

## Decisión
Usar **JWT (JSON Web Tokens)** con firma HMAC-SHA256 para autenticación. Los tokens se almacenan en `localStorage` del cliente y se envían en el header `Authorization: Bearer`.

## Consecuencias

### Positivas
- Sin estado en el servidor (escala horizontalmente)
- Funciona correctamente con CORS (frontend en dominio distinto)
- Spring Security tiene soporte nativo con `spring-security-oauth2-resource-server`

### Negativas
- Revocación de tokens requiere una lista negra (no implementada en el curso)
- Si el secret se expone, todos los tokens quedan comprometidos
- `localStorage` es vulnerable a XSS (mejora: usar `httpOnly` cookie)

## Alternativas consideradas
- **Sesiones HTTP**: requiere sesión compartida entre instancias (más complejo en deploy)
- **OAuth2 con Google/GitHub**: más seguro pero añade dependencia externa innecesaria para el curso
