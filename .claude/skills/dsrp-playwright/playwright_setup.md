---
name: playwright-setup
description: Setup de Playwright para verificaciones en localhost:4200 (LoanApp)
---

## Login

Hoy no hace falta: las rutas son públicas (`SecurityConfig.java` con `anyRequest().permitAll()`)
y `LoginComponent` todavía es un placeholder sin `AuthService` (TODO del Módulo 4,
`login.component.ts:5,14`). El script no navega a `/login` ni maneja credenciales.

**Cuando se implemente M4** (JWT real + guards), este archivo y `base-script.js` deben
actualizarse para agregar el flujo de login, siguiendo el mismo patrón: credenciales en
`.env.local` (gitignored), nunca hardcodeadas en el script.

## Setup técnico

- **Playwright instalado en**: `frontend/node_modules/playwright` (si no está, `cd frontend && npm i -D playwright` primero)
- **Ejecutar desde**: `frontend/` (para que `require('playwright')` resuelva)
- **Comando**: `cd frontend; node verify.js`
- **Screenshots en**: `C:\Users\richa\AppData\Local\Temp\pw-verify\`
- **Backend debe estar corriendo**: `cd backend && mvn spring-boot:run` (puerto 8080) — el frontend
  llama a `http://localhost:8080/api` (`environment.ts`)
- **Frontend debe estar corriendo**: `cd frontend && npm start` (puerto 4200)

## Gotchas conocidos

- `waitForURL` NO acepta función predicate con `url.includes()` — usar regex: `await page.waitForURL(/patron/)`
- El script debe copiarse a `frontend/verify.js` para que `require('playwright')` resuelva contra
  `frontend/node_modules`
- Borrar `verify.js` del repo tras cada uso
- Si el backend no está levantado, el dashboard muestra el banner "Backend no disponible"
  (`dashboard.component.ts:55-62`) en vez de las stats — no es un bug del frontend

## Rutas importantes para verificar

| Página | URL |
|---|---|
| Dashboard | `http://localhost:4200/dashboard` |
| Solicitud de préstamo | `http://localhost:4200/loans` |
| Login (placeholder, M4) | `http://localhost:4200/login` |
