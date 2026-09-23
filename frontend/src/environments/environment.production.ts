// En Vercel: configurar variable de entorno VITE_API_URL o NG_APP_API_URL
// en Settings → Environment Variables del proyecto.
// Valor: https://tu-backend.railway.app/api
//
// Angular usa NG_APP_* como variables de entorno en build time (Angular 16+).
// Si prefieres un enfoque diferente, Claude Code puede adaptar esto.

export const environment = {
  production: true,
  // Lee la variable de entorno inyectada por Vercel en build time
  // Fallback a '/api' para no romper si no está configurada
  apiUrl: (window as any).__env?.apiUrl
    ?? process.env['NG_APP_API_URL']
    ?? 'https://loanapp-api.railway.app/api',
};
