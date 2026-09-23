import { HttpInterceptorFn } from '@angular/common/http';

/**
 * TODO (M4 - Seguridad): Adjuntar el JWT a todas las peticiones autenticadas.
 * Actualmente el interceptor no hace nada.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  // TODO: const token = localStorage.getItem('token');
  // const cloned = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
  return next(req);  // TODO (M4): reemplazar con cloned
};
