# LoanApp — Proyecto de Capacitación Claude Code for Developers

Aplicación de gestión de solicitudes de préstamo utilizada en el curso.
Contiene **código intencional con problemas** que los alumnos resolverán módulo a módulo con Claude Code.

---

## Requisitos previos

| Herramienta | Versión mínima | Descarga |
|-------------|---------------|----------|
| **Java**    | 21            | https://adoptium.net/ |
| **Maven**   | 3.9+          | https://maven.apache.org/download.cgi *(o usar `./mvnw`)* |
| **Node.js** | 18.13+        | https://nodejs.org/ |
| **npm**     | 9+            | incluido con Node.js |

Verificar instalación:
```bash
java -version     # debe mostrar 21.x
mvn -version      # debe mostrar 3.9.x  (opcional si usas ./mvnw)
node -version     # debe mostrar v18.x o v20.x
npm -version      # debe mostrar 9.x o 10.x
```

---

## Levantar el Backend (Spring Boot)

```bash
cd backend

# Opción A — con Maven instalado globalmente
mvn spring-boot:run

# Opción B — con el wrapper incluido (no requiere Maven instalado)
# macOS / Linux: dar permisos de ejecución la primera vez
chmod +x mvnw
./mvnw spring-boot:run

# Windows CMD
mvnw.cmd spring-boot:run
```

El servidor arranca en **http://localhost:8080**

| URL | Descripción |
|-----|-------------|
| http://localhost:8080/api/loans | Lista de solicitudes (JSON) |
| http://localhost:8080/api/products | Productos disponibles (JSON) |
| http://localhost:8080/api/loans/stats | Estadísticas del portafolio |
| http://localhost:8080/h2-console | Consola H2 (BD en memoria) |

> **Consola H2:** JDBC URL = `jdbc:h2:mem:loandb` · Usuario: `sa` · Contraseña: *(vacío)*

La base de datos se inicializa automáticamente con 8 solicitudes de ejemplo.

---

## Levantar el Frontend (Angular 17)

```bash
cd frontend
npm install        # solo la primera vez (~2 min)
npm start          # inicia el servidor de desarrollo
```

La aplicación estará en **http://localhost:4200**

> El frontend hace proxy automático de `/api` → `http://localhost:8080`.
> Levanta primero el backend, luego el frontend.

---

## Estructura del proyecto

```
claude-developers/
├── README.md              ← Este archivo
├── CLAUDE.md              ← Contexto global para Claude Code
├── backend/               ← Spring Boot 3.2 / Java 21
│   ├── CLAUDE.md          ← Convenciones del backend
│   ├── mvnw               ← Maven Wrapper (Unix)
│   ├── mvnw.cmd           ← Maven Wrapper (Windows)
│   └── src/
│       ├── main/java/com/capacitacion/loanapp/
│       │   ├── api/            ← Controladores y DTOs
│       │   ├── domain/         ← Modelo, servicios, excepciones
│       │   └── infrastructure/ ← Repositorios, configuración
│       └── test/
└── frontend/              ← Angular 17 (standalone, signals)
    ├── CLAUDE.md          ← Convenciones del frontend
    └── src/app/
        ├── features/      ← Dashboard, LoanForm, Login
        ├── shared/        ← Modelos, servicios, componentes
        └── core/          ← Guards, interceptores
```

---

## Módulos del curso y archivos con problemas intencionales

| Módulo | Problema intencional | Archivo |
|--------|---------------------|---------|
| **M1** Refactorización | God Class — viola SRP | `OrderService.java` |
| **M1** Refactorización | Lógica HTTP en componente | `product-list.component.ts` |
| **M2** Reglas de negocio | Validaciones inline sin patrón | `LoanController.java` |
| **M3** Testing | Tests incompletos, sin mocks | `LoanServiceTest.java` |
| **M4** Seguridad | Sin JWT, sin RBAC | `SecurityConfig.java` |
| **M4** Seguridad | Sin guard, sin interceptor | `loan-form.component.ts` |
| **M5** CI/CD | Pipeline mínimo sin caché | `.github/workflows/ci.yml` |
| **M6** UX/UI | Sin signals, sin accesibilidad | `product-card.component.ts` |

---

## Comandos útiles con Claude Code

```bash
# En la raíz del proyecto:
claude "Analiza OrderService.java y explica qué principio SOLID viola"
claude "Genera un SPEC.md para refactorizar LoanController"
claude "Escribe tests unitarios para OrderService con mocks de Mockito"
```

---

## Colores corporativos (para ejercicios de UI)

| Token | Valor | Uso |
|-------|-------|-----|
| `--color-primary` | `#051b58` | Azul corporativo |
| `--color-accent`  | `#e00c49` | Rojo corporativo |
| `--color-bg`      | `#ffffff` | Fondo blanco |
