import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * TODO (M4 - Seguridad): Implementar guard real con AuthService.
 * Actualmente permite acceso a cualquier ruta.
 * Ver SECURITY_SPEC.md para los requisitos.
 */
export const authGuard: CanActivateFn = (_route, _state) => {
  // TODO: const auth = inject(AuthService); return auth.isLoggedIn();
  return true;   // TODO (M4): eliminar esta línea
};
