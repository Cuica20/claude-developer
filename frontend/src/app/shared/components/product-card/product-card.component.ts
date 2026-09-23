import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { Product } from '../../models/product.model';

/**
 * TODO (M6 - UX/UI): Este componente necesita mejoras:
 * - Migrar @Input()/@Output() a signals: input.required<Product>() y output<Product>()
 * - Agregar ChangeDetectionStrategy.OnPush
 * - Agregar ARIA labels (aria-label, role, aria-disabled)
 * - El CSS está hardcodeado (no usa variables CSS ni BEM)
 * - Sin estado "sin stock" accesible por teclado
 * - Sin loading skeleton
 *
 * Ver COMPONENT_SPEC.md para el diseño final esperado.
 */
@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  template: `
    <div [style.border]="product.featured ? '2px solid #fbbf24' : '1px solid #e2e8f0'"
         style="border-radius:10px;overflow:hidden;max-width:280px;background:#fff">
      <!-- TODO: alt debería ser descriptivo (M6) -->
      <img [src]="product.imageUrl" alt="imagen"
           style="width:100%;height:160px;object-fit:cover">
      <div style="padding:16px">
        <h3 style="font-size:1rem;color:#051b58;font-weight:700">{{ product.name }}</h3>
        <p style="color:#64748b;font-size:0.85rem;margin:4px 0 8px">{{ product.description }}</p>
        <p style="color:#051b58;font-weight:700">{{ product.monthlyRate }}% mensual</p>
        <!-- TODO: sin aria-label, sin aria-disabled (M6) -->
        <button
          (click)="addToCart.emit(product)"
          [disabled]="!product.inStock"
          style="background:#051b58;color:#fff;border:none;padding:8px 16px;
                 border-radius:6px;width:100%;margin-top:12px;cursor:pointer">
          {{ product.inStock ? 'Solicitar' : 'No disponible' }}
        </button>
      </div>
    </div>
  `,
})
export class ProductCardComponent {
  // TODO (M6): migrar a input.required<Product>() y output<Product>()
  @Input() product!: Product;
  @Output() addToCart = new EventEmitter<Product>();
}
