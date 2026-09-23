import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LoanService } from '../../shared/services/loan.service';
import { ProductListComponent } from '../../shared/components/product-list/product-list.component';
import { LoanStats } from '../../shared/models/loan.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink, ProductListComponent],
  template: `
    <div class="dashboard">
      <div class="dashboard__header">
        <div>
          <h1 class="dashboard__title">Dashboard</h1>
          <p class="dashboard__subtitle">Gestión de solicitudes de préstamo</p>
        </div>
        <a routerLink="/loans" class="btn btn--accent">+ Nueva solicitud</a>
      </div>

      <!-- Stats cards -->
      <div class="stats-grid" *ngIf="stats">
        <div class="stat-card">
          <span class="stat-card__value">{{ stats.total }}</span>
          <span class="stat-card__label">Total solicitudes</span>
        </div>
        <div class="stat-card stat-card--pending">
          <span class="stat-card__value">{{ stats.pending }}</span>
          <span class="stat-card__label">Pendientes</span>
        </div>
        <div class="stat-card stat-card--approved">
          <span class="stat-card__value">{{ stats.approved }}</span>
          <span class="stat-card__label">Aprobadas</span>
        </div>
        <div class="stat-card stat-card--rejected">
          <span class="stat-card__value">{{ stats.rejected }}</span>
          <span class="stat-card__label">Rechazadas</span>
        </div>
        <div class="stat-card stat-card--portfolio">
          <span class="stat-card__value">{{ stats.portfolio | currency:'USD':'symbol':'1.0-0' }}</span>
          <span class="stat-card__label">Cartera total</span>
        </div>
      </div>

      <!-- Loading state -->
      <div *ngIf="loading" class="stats-grid">
        <div *ngFor="let _ of [1,2,3,4,5]" class="stat-card stat-card--skeleton">
          <div class="skeleton-bar skeleton-bar--lg"></div>
          <div class="skeleton-bar skeleton-bar--sm"></div>
        </div>
      </div>

      <!-- Backend not running — mensaje claro -->
      <div *ngIf="backendDown" class="alert-banner">
        <span class="alert-banner__icon">⚠️</span>
        <div>
          <strong>Backend no disponible</strong>
          <p>El servidor Spring Boot no está corriendo. Inícialo primero:</p>
          <code>cd backend &amp;&amp; ./mvnw spring-boot:run</code>
        </div>
      </div>

      <!-- Productos disponibles -->
      <section class="section">
        <h2 class="section__title">Productos disponibles</h2>
        <app-product-list />
      </section>
    </div>
  `,
  styles: [`
    .dashboard { padding: 2rem; max-width: 1100px; margin: 0 auto; }
    .dashboard__header {
      display: flex; align-items: flex-start;
      justify-content: space-between; margin-bottom: 1.5rem;
    }
    .dashboard__title  { font-size: 1.6rem; color: var(--color-primary); }
    .dashboard__subtitle { color: var(--color-text-muted); font-size: 0.9rem; margin-top: 2px; }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
      gap: 1rem; margin-bottom: 2rem;
    }
    .stat-card {
      background: #fff; border: 1px solid var(--color-border);
      border-radius: var(--radius-card); padding: 1.25rem;
      text-align: center; box-shadow: var(--shadow-sm);
    }
    .stat-card__value { display: block; font-size: 1.8rem; font-weight: 700; color: var(--color-primary); }
    .stat-card__label { display: block; font-size: 0.8rem; color: var(--color-text-muted); margin-top: 4px; }
    .stat-card--pending   .stat-card__value { color: #d97706; }
    .stat-card--approved  .stat-card__value { color: #16a34a; }
    .stat-card--rejected  .stat-card__value { color: var(--color-accent); }
    .stat-card--portfolio .stat-card__value { font-size: 1.2rem; }
    .stat-card--skeleton  { border-style: dashed; }
    .skeleton-bar { background: #e2e8f0; border-radius: 4px; animation: pulse 1.5s infinite; }
    .skeleton-bar--lg { height: 2rem; width: 60%; margin: 0 auto 8px; }
    .skeleton-bar--sm { height: 0.7rem; width: 80%; margin: 0 auto; }
    @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.4} }

    .alert-banner {
      display: flex; align-items: flex-start; gap: 1rem;
      background: #fffbeb; border: 1px solid #fcd34d; border-radius: var(--radius-md);
      padding: 1rem 1.25rem; margin-bottom: 2rem;
    }
    .alert-banner__icon { font-size: 1.5rem; flex-shrink: 0; }
    .alert-banner strong { color: #92400e; display: block; margin-bottom: 4px; }
    .alert-banner p { color: #78350f; font-size: 0.875rem; margin-bottom: 6px; }
    .alert-banner code {
      background: #fef3c7; color: #92400e; padding: 2px 8px;
      border-radius: 4px; font-size: 0.85rem;
    }
    .section { margin-top: 1.5rem; }
    .section__title { font-size: 1.2rem; color: var(--color-primary); margin-bottom: 1rem; }
  `],
})
export class DashboardComponent implements OnInit {
  private loanService = inject(LoanService);
  stats: LoanStats | null = null;
  loading = true;
  backendDown = false;

  ngOnInit(): void {
    this.loanService.getStats().subscribe({
      next: s => { this.stats = s; this.loading = false; },
      error: () => { this.backendDown = true; this.loading = false; },
    });
  }
}
