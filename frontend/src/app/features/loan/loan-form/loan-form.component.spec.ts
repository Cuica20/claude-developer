import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { LoanFormComponent } from './loan-form.component';
import { environment } from '../../../../environments/environment';

describe('LoanFormComponent', () => {
  let fixture: ComponentFixture<LoanFormComponent>;
  let component: LoanFormComponent;
  let httpMock: HttpTestingController;

  const validFormValue = {
    applicantName: 'María García',
    applicantEmail: 'maria@test.com',
    birthDate: '1990-05-01',
    monthlyIncome: 3000,
    creditScore: 700,
    amount: 15000,
    termMonths: 36,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoanFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(LoanFormComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  function submitValidForm(): void {
    component.form.setValue(validFormValue);
    component.onSubmit();
  }

  it('should_populateFieldErrors_when_serverReturns400WithFieldErrors', () => {
    submitValidForm();

    const req = httpMock.expectOne(`${environment.apiUrl}/loans`);
    req.flush(
      {
        status: 400,
        error: 'ValidationError',
        message: 'Errores de validación',
        ruleId: null,
        fieldErrors: {
          birthDate: 'RN-001: El solicitante debe ser mayor de 18 años',
          creditScore: 'RN-003: Score insuficiente. Minimo: 700',
        },
        timestamp: '2026-09-22T10:00:00',
      },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(component.f['birthDate'].getError('server')).toBe(
      'RN-001: El solicitante debe ser mayor de 18 años'
    );
    expect(component.f['creditScore'].getError('server')).toBe(
      'RN-003: Score insuficiente. Minimo: 700'
    );
    expect(component.errorMessage).toBeNull();
    expect(component.loading).toBeFalse();
  });

  it('should_showGenericBanner_when_serverReturns500WithoutFieldErrors', () => {
    submitValidForm();

    const req = httpMock.expectOne(`${environment.apiUrl}/loans`);
    req.flush(
      {
        status: 500,
        error: 'Internal Server Error',
        message: 'Error interno del servidor',
        ruleId: null,
        fieldErrors: null,
        timestamp: '2026-09-22T10:00:00',
      },
      { status: 500, statusText: 'Internal Server Error' }
    );

    expect(component.errorMessage).toBe('Error interno del servidor');
    expect(component.loading).toBeFalse();
  });

  it('should_clearPreviousServerErrors_when_resubmitting', () => {
    submitValidForm();
    httpMock
      .expectOne(`${environment.apiUrl}/loans`)
      .flush(
        { fieldErrors: { creditScore: 'RN-003: Score insuficiente. Minimo: 700' } },
        { status: 400, statusText: 'Bad Request' }
      );
    expect(component.f['creditScore'].hasError('server')).toBeTrue();

    submitValidForm();

    expect(component.f['creditScore'].hasError('server')).toBeFalse();
    httpMock.expectOne(`${environment.apiUrl}/loans`).flush({ id: 1, status: 'PENDING' }, { status: 201, statusText: 'Created' });
  });

  it('should_showSuccessView_when_requestSucceeds', () => {
    submitValidForm();

    const req = httpMock.expectOne(`${environment.apiUrl}/loans`);
    req.flush({ id: 42, applicantName: 'María García', status: 'PENDING', monthlyInstallment: 500 });

    expect(component.savedLoan?.id).toBe(42);
    expect(component.loading).toBeFalse();
  });
});
