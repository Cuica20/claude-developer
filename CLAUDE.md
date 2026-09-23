# Proyecto de Capacitación: Claude Code for Developers

## Empresa
**Colores corporativos:** Azul `#051b58` · Rojo `#e00c49` · Fondo blanco `#ffffff`

## Descripción del proyecto
Aplicación de gestión de solicitudes de préstamo (`LoanApp`) usada durante el curso
*Claude Code for Developers*. Contiene código **intencional con problemas** que los
alumnos resolverán módulo a módulo con Claude Code.

## Estructura del repositorio
```
├── CLAUDE.md              ← Este archivo (contexto global)
├── docker-compose.yml     ← Stack local: PostgreSQL + backend + pgAdmin
├── backend/               ← Spring Boot 3.2 / Java 21
│   ├── CLAUDE.md          ← Convenciones específicas del backend
│   └── src/
├── frontend/              ← Angular 17 (standalone, signals)
│   ├── CLAUDE.md          ← Convenciones específicas del frontend
│   └── src/
├── docs/adr/              ← Architecture Decision Records
└── guia-claude-developers.html  ← Guía del instructor (26+ módulos)
```

## Stack tecnológico
| Capa      | Tecnología              | Versión   |
|-----------|-------------------------|-----------|
| Backend   | Java + Spring Boot      | 21 / 3.2  |
| Frontend  | Angular                 | 17+       |
| Estilos   | SCSS (BEM)              | —         |
| Tests BE  | JUnit 5 + Mockito       | —         |
| Tests FE  | Jasmine + TestBed       | —         |
| CI/CD     | GitHub Actions          | —         |
| DB        | H2 (dev) / PostgreSQL   | 15        |
| Docker    | Docker Compose          | —         |

## Convenciones de código (ambos proyectos)
- **Java:** inyección por constructor (nunca `@Autowired` en campo)
- **Java:** clases de dominio en `domain/`, controladores en `api/`
- **Angular:** un componente por archivo, componentes standalone
- **Angular:** BEM para CSS, variables CSS en lugar de valores hardcodeados
- **Tests:** nombres `should_[resultado]_when_[condición]`
- **Commits:** convención `feat:`, `fix:`, `refactor:`, `test:`

## Problemas intencionales por módulo
| Módulo | Archivo con problema            | Tipo de problema                              |
|--------|---------------------------------|-----------------------------------------------|
| M0     | `guia-claude-developers.html`   | Setup Claude Code + ciclo mental pide→decide  |
| M1     | `OrderService.java`             | God Class — viola SRP                         |
| M1     | `product-list.component.ts`     | Lógica HTTP en el componente                  |
| M2     | `LoanController.java`           | Validaciones inline sin patrón                |
| M3     | `LoanServiceTest.java`          | Tests incompletos, sin mocks                  |
| M4     | `SecurityConfig.java`           | Sin JWT, sin RBAC                             |
| M4     | `loan-form.component.ts`        | Sin guards, sin interceptor                   |
| M5     | `.github/workflows/ci.yml`      | Pipeline mínimo sin caché                     |
| M6     | `product-card.component.ts`     | Sin signals, sin accesibilidad                |
| M7     | `LoanCalculatorService.java`    | Bug de precisión: double vs BigDecimal        |
| M8     | `UserController.java`           | IDOR, datos sensibles expuestos, sin paginación|
| M9     | `V1__create_loans_table.sql`    | Sin índices, tipos incorretos, sin constraints|
| M10    | (Testcontainers + Playwright)   | Sin tests de integración ni E2E               |

## Módulos de referencia (no tienen "problema intencional")
| Sección              | Tema                                                         |
|----------------------|--------------------------------------------------------------|
| openapi              | springdoc-openapi, @Operation, YAML export                   |
| n1-queries           | N+1, @EntityGraph, JOIN FETCH                                |
| exception-handling   | @ControllerAdvice, ErrorResponse DTO                         |
| docker-compose       | Stack local, healthchecks, .env                              |
| pr-workflow          | Self code review, descripción de PR                          |
| seed-data            | Flyway V99__, datos realistas, profiles                      |
| adr                  | Architecture Decision Records en docs/adr/                   |
| observabilidad       | Spring Actuator, Micrometer, logs estructurados, alertas     |
| wiremock             | Mocking de APIs externas en tests de integración             |
| cc-hooks-mcps        | Claude Code hooks pre/post, MCPs, modo agente paralelo       |

## Utilitarios para ahorrar tokens (tools/)
Antes de explorar o leer archivos Java/TypeScript completos, usa estos scripts
(sin dependencias, solo lectura) en vez de Glob+Read archivo por archivo:

- **Explorar el repo / ubicar dónde está algo:**
  `python tools/project_map.py` (o `--grep <texto>`, `--mode controller`, `--backend-only`)
  en vez de recorrer `backend/src` o `frontend/src/app` con Glob.
- **Revisar un archivo Java o TypeScript sin leerlo completo:**
  `python tools/skeletonizer.py <archivo> --summary-only` primero; solo usa
  `Read` sobre el archivo completo si el resumen no basta (p. ej. vas a editar
  el cuerpo de un método).
- **Revisar migraciones Flyway (`V*__*.sql`):**
  `python tools/sql_skeletonizer.py <archivo>` para ver tablas/columnas/índices
  sin el SQL crudo, y `--check` para detectar tipos imprecisos, FKs sin índice
  o tablas sin PK (relevante para M9).

Detalle y ejemplos en `tools/README.md`. Estos scripts nunca modifican archivos.

## Lo que NO debe modificar Claude Code
- Archivos en `legacy/` → solo lectura
- `pom.xml` raíz → no agregar dependencias sin instrucción explícita
- `app.config.ts` → solo modificar en M4

## Comando de verificación rápida
```bash
# Backend
cd backend && mvn test

# Frontend
cd frontend && npm test -- --watch=false

# Stack completo con Docker
docker-compose up --build
# Backend en http://localhost:8080
# pgAdmin en http://localhost:5050
```
