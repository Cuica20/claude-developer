import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Product } from '../../models/product.model';

/**
 * TODO (M1 - Refactorización): Este componente viola SRP.
 * - Tiene lógica de fetch, filtro y ordenamiento mezclada
 * - No usa ChangeDetectionStrategy.OnPush
 * - No usa signals (input/output)
 * - El template debería delegar en ProductCardComponent
 *
 * Objetivo: extraer a ProductService, usar signals y OnPush
 */
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="product-list">
      <div *ngIf="loading" class="product-list__loading">Cargando productos...</div>
      <div *ngIf="error"   class="product-list__error">{{ error }}</div>

      <div class="product-grid">
        <!-- TODO (M6): reemplazar por <app-product-card> con ARIA y BEM -->
        <div *ngFor="let p of products" class="product-item"
             [class.product-item--featured]="p.featured"
             [class.product-item--out]="!p.inStock">
          <img [src]="p.imageUrl" alt="{{ p.name }}"
               style="width:100%;height:160px;object-fit:cover;border-radius:8px 8px 0 0">
          <div style="padding:16px">
            <span *ngIf="p.featured" style="background:#fef3c7;color:#92400e;
                  font-size:0.7rem;font-weight:700;padding:2px 8px;border-radius:99px">
              DESTACADO
            </span>
            <h3 style="font-size:1rem;font-weight:700;color:#051b58;margin:8px 0 4px">{{ p.name }}</h3>
            <p style="font-size:0.85rem;color:#64748b;margin-bottom:8px">{{ p.description }}</p>
            <p style="font-size:0.85rem">
              Hasta <strong>{{ p.maxAmount | currency }}</strong> · {{ p.maxTermMonths }} meses
            </p>
            <p style="color:#051b58;font-weight:700;margin:4px 0">{{ p.monthlyRate }}% mensual</p>
            <!-- TODO: sin aria-label, sin accesibilidad de teclado -->
            <button (click)="select(p)"
                    [disabled]="!p.inStock"
                    style="background:#051b58;color:#fff;padding:8px 16px;border:none;
                           border-radius:6px;width:100%;margin-top:12px;cursor:pointer;
                           opacity:{{ p.inStock ? 1 : 0.5 }}">
              {{ p.inStock ? 'Solicitar' : 'No disponible' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .product-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 1rem;
    }
    .product-item {
      border: 1px solid #e2e8f0;
      border-radius: 10px;
      overflow: hidden;
      background: #fff;
      box-shadow: 0 1px 3px rgba(0,0,0,.06);
    }
    .product-item--featured { border-color: #fbbf24; }
    .product-item--out      { opacity: 0.65; }
    .product-list__loading  { color: #64748b; padding: 8px 0; }
    .product-list__error    { color: #e00c49; padding: 8px 0; }
  `],
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  loading = true;
  error: string | null = null;

  // TODO: extraer a ProductService (M1)
  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    // TODO: mover a ProductService.getActiveProducts()
    this.http.get<Product[]>('/api/products').subscribe({
      next: (list) => {
        this.products = list
          .filter(p => p.active)
          .sort((a, b) => a.name.localeCompare(b.name));
        this.loading = false;
      },
      error: () => {
        this.error = 'Error al cargar productos (¿está corriendo el backend en :8080?)';
        this.loading = false;
      },
    });
  }

  select(product: Product): void {
    // TODO: navegar a /loans con el producto preseleccionado
    console.log('Producto seleccionado:', product.name);
  }
}
