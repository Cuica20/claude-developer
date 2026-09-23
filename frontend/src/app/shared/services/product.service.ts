import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Product } from '../models/product.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/products`;

  /**
   * Retorna todos los productos activos, ordenados por nombre.
   * Extrae la lógica de filtro/orden que hoy está en ProductListComponent (M1 problem).
   */
  getActiveProducts(): Observable<Product[]> {
    return this.http.get<Product[]>(this.base).pipe(
      map(products =>
        products
          .filter(p => p.active)
          .sort((a, b) => a.name.localeCompare(b.name))
      )
    );
  }
}
