import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

// TODO (M4): implementar LoginComponent completo con AuthService
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div style="max-width:360px;margin:80px auto;padding:32px;
         border:1px solid #e2e8f0;border-radius:12px;text-align:center">
      <h2 style="color:#051b58;margin-bottom:24px">Iniciar sesión</h2>
      <p style="color:#64748b">TODO (M4): Implementar con AuthService + JWT</p>
    </div>
  `,
})
export class LoginComponent {}
