import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="navbar">
      <div class="navbar__brand">
        <span class="navbar__logo">💼</span>
        <span class="navbar__title">LoanApp</span>
        <span class="navbar__subtitle">Capacitación</span>
      </div>
      <nav class="navbar__links" aria-label="Navegación principal">
        <a routerLink="/dashboard" routerLinkActive="navbar__link--active" class="navbar__link">Dashboard</a>
        <a routerLink="/loans"     routerLinkActive="navbar__link--active" class="navbar__link">Solicitar</a>
        <a routerLink="/login"     routerLinkActive="navbar__link--active" class="navbar__link">Login</a>
      </nav>
    </header>

    <main class="main-content">
      <router-outlet />
    </main>
  `,
  styles: [`
    .navbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 2rem;
      height: 60px;
      background: var(--color-primary);
      color: #fff;
      box-shadow: 0 2px 8px rgba(0,0,0,.2);
    }
    .navbar__brand {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }
    .navbar__logo { font-size: 1.4rem; }
    .navbar__title {
      font-size: 1.1rem;
      font-weight: 700;
      letter-spacing: 0.5px;
    }
    .navbar__subtitle {
      font-size: 0.75rem;
      opacity: 0.7;
      margin-left: 4px;
    }
    .navbar__links { display: flex; gap: 1rem; }
    .navbar__link {
      color: rgba(255,255,255,0.85);
      text-decoration: none;
      font-size: 0.875rem;
      font-weight: 500;
      padding: 0.375rem 0.75rem;
      border-radius: 6px;
      transition: background 0.15s;
    }
    .navbar__link:hover { background: rgba(255,255,255,.12); color: #fff; }
    .navbar__link--active { background: var(--color-accent); color: #fff; }
    .main-content { min-height: calc(100vh - 60px); background: var(--color-bg-subtle); }
  `],
})
export class AppComponent {}
