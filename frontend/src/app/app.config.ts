import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';

// TODO (M4): agregar withInterceptors([jwtInterceptor, errorInterceptor])

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),   // TODO: agregar interceptores en M4
    provideAnimations(),
  ],
};
