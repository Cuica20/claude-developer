# Frontend — LoanApp (Angular 17+)

## Estructura de carpetas
```
src/app/
├── core/
│   ├── guards/          ← AuthGuard, RoleGuard (funcionales)
│   └── interceptors/    ← JwtInterceptor, ErrorInterceptor
├── features/
│   ├── loan/            ← Módulo de solicitudes
│   │   ├── loan-form/
│   │   └── loan-list/
│   └── dashboard/
└── shared/
    ├── components/      ← Componentes reutilizables (ProductCard, etc.)
    ├── services/        ← Servicios de datos
    └── models/          ← Interfaces TypeScript
```

## Convenciones Angular
- Todos los componentes son **standalone** (no NgModule)
- Usar **signals** (`input()`, `output()`, `signal()`) en nuevos componentes
- **ChangeDetection.OnPush** obligatorio en componentes de lista/card
- CSS con metodología **BEM**: `.loan-card`, `.loan-card__title`, `.loan-card--active`
- Variables CSS en `styles.scss`: `--color-primary: #051b58; --color-accent: #e00c49`

## Colores corporativos (usar en CSS/SCSS)
```scss
// styles.scss
:root {
  --color-primary: #051b58;   // Azul corporativo
  --color-accent:  #e00c49;   // Rojo corporativo
  --color-bg:      #ffffff;
  --color-text:    #1e293b;
}
```

## Comandos útiles
```bash
npm start                           # Dev server en :4200
npm test -- --watch=false           # Tests una vez
npm test -- --code-coverage         # Tests + cobertura
npm run build -- --configuration production
ng generate component features/loan/loan-form --standalone
```

## Variables de entorno (environment.ts)
```typescript
export const environment = {
  apiUrl: 'http://localhost:8080/api',
  production: false,
};
```
