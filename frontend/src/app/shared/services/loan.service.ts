import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Loan, LoanRequest, LoanStats } from '../models/loan.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class LoanService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/loans`;

  getAll(): Observable<Loan[]> {
    return this.http.get<Loan[]>(this.base);
  }

  getById(id: number): Observable<Loan> {
    return this.http.get<Loan>(`${this.base}/${id}`);
  }

  create(req: LoanRequest): Observable<Loan> {
    return this.http.post<Loan>(this.base, req);
  }

  getStats(): Observable<LoanStats> {
    return this.http.get<LoanStats>(`${this.base}/stats`);
  }
}
