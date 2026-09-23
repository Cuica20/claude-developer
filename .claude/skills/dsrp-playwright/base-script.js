// Playwright base template — copiar a frontend/verify.js antes de ejecutar
// Completar la sección TODO con los pasos específicos de la verificación
// BORRAR verify.js tras cada uso
//
// No hay login: las rutas son públicas hoy (SecurityConfig con permitAll) y LoginComponent
// es un placeholder sin AuthService (TODO del Módulo 4). Cuando M4 esté implementado, agregar
// el flujo de login acá siguiendo el patrón de credenciales en .env.local (nunca hardcodeadas).

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = 'C:/Users/richa/AppData/Local/Temp/pw-verify';

// Solo llamar shot() cuando el output de consola no alcanza para concluir PASS/FAIL
// (layout roto, componente visual complejo, bug difícil de describir en texto)
async function shot(page, label) {
  if (!fs.existsSync(OUT)) fs.mkdirSync(OUT, { recursive: true });
  const ts = Date.now();
  const p = path.join(OUT, label + '-' + ts + '.png');
  await page.screenshot({ path: p, fullPage: false });
  console.log('SCREENSHOT:' + p);
}

// Helpers de aserción sin screenshot
async function has(page, sel)          { return (await page.$(sel)) !== null; }
async function count(page, sel)        { return (await page.$$(sel)).length; }
async function text(page, sel)         { const el = await page.$(sel); return el ? (await el.textContent()).trim() : null; }
async function attr(page, sel, a)      { const el = await page.$(sel); return el ? await el.getAttribute(a) : null; }

(async () => {
  const browser = await chromium.launch({ headless: false, slowMo: 120 });
  const ctx  = await browser.newContext({ viewport: { width: 1400, height: 900 } });
  const page = await ctx.newPage();

  // ── TODO: navegación y pasos específicos ──────────────────────────────────
  // await page.goto('http://localhost:4200/dashboard', { waitUntil: 'networkidle' });
  //
  // Preferir assertions de consola:
  //   console.log('HAS_X:' + await has(page, '.selector'));
  //   console.log('COUNT:' + await count(page, '.item'));
  //   console.log('TEXT:' + await text(page, 'h1'));
  //   const [res] = await Promise.all([page.waitForResponse(r => r.url().includes('/api/x')), page.click('.btn')]);
  //   console.log('STATUS:' + res.status());
  // Solo usar shot() si el output de texto no alcanza para concluir PASS/FAIL


  console.log('DONE');
  await browser.close();
})().catch(e => { console.error('ERROR:' + e.message); process.exit(1); });
