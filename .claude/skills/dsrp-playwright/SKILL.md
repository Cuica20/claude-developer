---
name: dsrp-playwright
description: Verificación visual con Playwright en el frontend de LoanApp (localhost:4200). Genera un script Node.js, lo ejecuta desde frontend/ y reporta con screenshots.
triggers:
  keywords:
    - usa playwright
    - usar playwright
    - verifica con playwright
    - playwright
    - verificar que funcione
    - verificar visualmente
    - abre el browser
---

# LoanApp Playwright Verifier

## Setup

Leer `skills/dsrp-playwright/playwright_setup.md` para gotchas y estado de auth antes de generar
cualquier script. Confirmar que backend (`:8080`) y frontend (`:4200`) estén levantados.

## Flujo

1. **Identificar URLs** — extraer del mensaje del usuario. Si no hay ninguna, preguntar.
2. **Generar script** — copiar `skills/dsrp-playwright/base-script.js` como `frontend/verify.js`, completar la sección TODO con navegación y asserts para cada URL pedida.
3. **Ejecutar** — `cd frontend; node verify.js 2>&1`
4. **Reportar** — leer cada screenshot con Read (soporta PNG). Usar formato estándar del skill `verify` (PASS / FAIL / BLOCKED + Steps + Findings).
5. **Limpiar** — borrar `frontend/verify.js` siempre.

## Reglas

- `waitForURL` solo acepta string o regex — nunca función predicate
- **Screenshots son el último recurso** — usar solo cuando el output de consola no alcanza para concluir PASS/FAIL (layout roto, bug visual). Para todo lo demás: `console.log` con `has()`, `count()`, `text()`, `waitForResponse()`.
- Si se toman screenshots, leerlos con Read antes de reportar
- Si la URL pertenece a un área conocida, consultar `skills/dsrp-playwright/checklists.md` para saber qué assertions hacer

## Assertions sin screenshot (preferidos)

```js
console.log('HAS_X:'   + await has(page, '.selector'));       // elemento existe
console.log('COUNT:'   + await count(page, '.item'));          // cantidad de elementos
console.log('TEXT:'    + await text(page, 'h1'));              // contenido de texto
console.log('ATTR:'    + await attr(page, 'input', 'disabled')); // atributo
// POST status sin screenshot:
const [res] = await Promise.all([
  page.waitForResponse(r => r.url().includes('/api/x')),
  page.click('.btn-save')
]);
console.log('STATUS:' + res.status());
```
