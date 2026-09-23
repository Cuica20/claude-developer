import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { LoanService } from '../../../shared/services/loan.service';
import { Loan } from '../../../shared/models/loan.model';

/**
 * TODO (M2 - Reglas de Negocio): Este formulario tiene validaciones
 * solo técnicas (required, min), pero le faltan las validaciones de negocio:
 * - RN-001: validar edad mínima de 18 años (no solo "campo requerido")
 * - RN-002: validar ratio ingreso/cuota >= 3 (validador de grupo)
 * - RN-003: validar score según monto (validador cruzado)
 * - RN-004: validar deuda vigente (validador asíncrono)
 *
 * TODO (M4): Sin guard → cualquier usuario puede acceder a esta ruta.
 */
@Component({
  selector: 'app-loan-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <!-- Pantalla de éxito -->
    <div *ngIf="savedLoan; else formView" class="success-view">
      <div class="success-card">
        <div class="success-card__icon">✅</div>
        <h2 class="success-card__title">¡Solicitud enviada!</h2>
        <div class="success-card__details">
          <div class="detail-row">
            <span class="detail-row__label">N° de solicitud</span>
            <span class="detail-row__value">#{{ savedLoan.id }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-row__label">Solicitante</span>
            <span class="detail-row__value">{{ savedLoan.applicantName }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-row__label">Monto</span>
            <span class="detail-row__value">{{ savedLoan.amount | currency }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-row__label">Cuota mensual estimada</span>
            <span class="detail-row__value detail-row__value--highlight">
              {{ savedLoan.monthlyInstallment | currency }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-row__label">Estado</span>
            <span class="status-badge status-badge--pending">{{ savedLoan.status }}</span>
          </div>
        </div>
        <div class="success-card__actions">
          <button (click)="newRequest()" class="btn btn--outline">Nueva solicitud</button>
          <button (click)="goToDashboard()" class="btn btn--primary">Ir al dashboard</button>
        </div>
      </div>
    </div>

    <!-- Formulario -->
    <ng-template #formView>
      <div class="loan-form-container">
        <div class="loan-form__header">
          <h2 class="loan-form__title">Solicitud de Préstamo</h2>
          <p class="loan-form__subtitle">Completa todos los campos para enviar tu solicitud</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-row">
            <div class="form-field">
              <label for="applicantName">Nombre completo</label>
              <input id="applicantName" type="text" formControlName="applicantName"
                     placeholder="Ej: María García">
              <span class="field-error" *ngIf="f['applicantName'].invalid && f['applicantName'].touched">
                Campo requerido
              </span>
            </div>
            <div class="form-field">
              <label for="applicantEmail">Email</label>
              <input id="applicantEmail" type="email" formControlName="applicantEmail"
                     placeholder="correo@ejemplo.com">
              <span class="field-error" *ngIf="f['applicantEmail'].hasError('email') && f['applicantEmail'].touched">
                Email inválido
              </span>
            </div>
          </div>

          <div class="form-row">
            <div class="form-field">
              <label for="birthDate">Fecha de nacimiento</label>
              <input id="birthDate" type="date" formControlName="birthDate">
              <!-- TODO (M2): validar RN-001 edad mínima 18 años -->
              <span class="field-error" *ngIf="f['birthDate'].invalid && f['birthDate'].touched">
                Campo requerido
              </span>
            </div>
            <div class="form-field">
              <label for="monthlyIncome">Ingreso mensual (USD)</label>
              <input id="monthlyIncome" type="number" formControlName="monthlyIncome"
                     placeholder="Ej: 3000" min="0" step="100">
              <!-- TODO (M2): validar RN-002 ratio ingreso/cuota >= 3 -->
            </div>
          </div>

          <div class="form-row">
            <div class="form-field">
              <label for="amount">Monto solicitado (USD)</label>
              <input id="amount" type="number" formControlName="amount"
                     placeholder="Ej: 15000" min="1000" step="1000">
            </div>
            <div class="form-field">
              <label for="termMonths">Plazo (meses)</label>
              <input id="termMonths" type="number" formControlName="termMonths"
                     placeholder="Ej: 36" min="6" max="360" step="6">
            </div>
          </div>

          <div class="form-field">
            <label for="creditScore">Score crediticio (300–850)</label>
            <input id="creditScore" type="number" formControlName="creditScore"
                   placeholder="Ej: 680" min="300" max="850">
            <!-- TODO (M2): validar RN-003 score mínimo según monto -->
          </div>

          <div *ngIf="errorMessage" class="alert alert--error">{{ errorMessage }}</div>

          <button type="submit" class="btn btn--primary"
                  [disabled]="form.invalid || loading"
                  style="width:100%;margin-top:8px">
            <span *ngIf="!loading">Solicitar préstamo</span>
            <span *ngIf="loading">Enviando...</span>
          </button>
        </form>
      </div>
    </ng-template>
  `,
  styles: [`
    .loan-form-container {
      max-width: 640px; margin: 40px auto; padding: 36px;
      background: #fff; border: 1px solid var(--color-border);
      border-radius: var(--radius-card); box-shadow: var(--shadow-md);
    }
    .loan-form__header { margin-bottom: 24px; }
    .loan-form__title  { color: var(--color-primary); font-size: 1.4rem; }
    .loan-form__subtitle { color: var(--color-text-muted); font-size: 0.875rem; margin-top: 4px; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .alert { margin-top: 12px; padding: 12px 16px; border-radius: var(--radius-md); font-weight: 500; }
    .alert--error { background:#fee2e2; color:#dc2626; border:1px solid #fca5a5; }

    .success-view { display: flex; align-items: center; justify-content: center; min-height: 80vh; padding: 2rem; }
    .success-card {
      max-width: 480px; width: 100%; background: #fff;
      border: 1px solid var(--color-border); border-radius: var(--radius-card);
      padding: 2.5rem; box-shadow: var(--shadow-md); text-align: center;
    }
    .success-card__icon  { font-size: 3rem; margin-bottom: 1rem; }
    .success-card__title { color: var(--color-primary); font-size: 1.5rem; margin-bottom: 1.5rem; }
    .success-card__details { text-align: left; border: 1px solid var(--color-border); border-radius: var(--radius-md); overflow: hidden; margin-bottom: 1.5rem; }
    .detail-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; border-bottom: 1px solid var(--color-border); }
    .detail-row:last-child { border-bottom: none; }
    .detail-row__label { font-size: 0.85rem; color: var(--color-text-muted); }
    .detail-row__value { font-weight: 600; }
    .detail-row__value--highlight { color: var(--color-primary); font-size: 1.1rem; }
    .status-badge { padding: 2px 10px; border-radius: 99px; font-size: 0.75rem; font-weight: 700; }
    .status-badge--pending { background: #fef3c7; color: #92400e; }
    .success-card__actions { display: flex; gap: 1rem; justify-content: center; }
  `],
})
export class LoanFormComponent {
  private fb          = inject(FormBuilder);
  private loanService = inject(LoanService);
  private router      = inject(Router);

  loading = false;
  errorMessage: string | null = null;
  savedLoan: Loan | null = null;

  // TODO (M2): agregar validadores de negocio: minAgeValidator, incomeRatioValidator,
  // creditScoreValidator, y el validador asíncrono CreditBureauValidator
  form = this.fb.group({
    applicantName:  ['', Validators.required],
    applicantEmail: ['', [Validators.required, Validators.email]],
    birthDate:      ['', Validators.required],
    monthlyIncome:  [null as number|null, [Validators.required, Validators.min(0)]],
    creditScore:    [null as number|null, [Validators.required, Validators.min(300), Validators.max(850)]],
    amount:         [null as number|null, [Validators.required, Validators.min(1000)]],
    termMonths:     [null as number|null, [Validators.required, Validators.min(6), Validators.max(360)]],
  });

  get f() { return this.form.controls; }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = null;

    this.loanService.create(this.form.value as any).subscribe({
      next: (loan) => {
        this.savedLoan = loan;
        this.loading = false;
        this.form.reset();
      },
      error: (err) => {
        this.errorMessage = err.error?.message ?? err.error?.error ?? 'Error al enviar la solicitud';
        this.loading = false;
      },
    });
  }

  newRequest(): void {
    this.savedLoan = null;
    this.errorMessage = null;
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
