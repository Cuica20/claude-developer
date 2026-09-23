import { Routes } from '@angular/router';

// TODO (M4): agregar authGuard y roleGuard a las rutas protegidas

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full',
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    // TODO: canActivate: [authGuard]
  },
  {
    path: 'loans',
    loadComponent: () =>
      import('./features/loan/loan-form/loan-form.component').then(m => m.LoanFormComponent),
    // TODO: canActivate: [authGuard]
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login.component').then(m => m.LoginComponent),
  },
  { path: '**', redirectTo: 'dashboard' },
];
