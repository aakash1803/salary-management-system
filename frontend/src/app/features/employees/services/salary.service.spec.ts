import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SalaryService } from './salary.service';
import { SalaryRequest, SalaryResponse } from '../models/salary.model';

describe('SalaryService', () => {
  let service: SalaryService;
  let httpMock: HttpTestingController;

  const mockSalaryResponse: SalaryResponse = {
    id: 1,
    employeeId: 1,
    amount: 75000,
    currency: 'USD',
    effectiveFrom: '2026-01-01',
    createdAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SalaryService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(SalaryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch current salary for an employee with withCredentials', () => {
    service.getCurrentSalary(1).subscribe(salary => {
      expect(salary).toEqual(mockSalaryResponse);
    });

    const req = httpMock.expectOne('/api/employees/1/salary');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);
    req.flush(mockSalaryResponse);
  });

  it('should fetch salary history for an employee with withCredentials', () => {
    const mockHistory: SalaryResponse[] = [mockSalaryResponse];

    service.getSalaryHistory(1).subscribe(history => {
      expect(history.length).toBe(1);
      expect(history[0]).toEqual(mockSalaryResponse);
    });

    const req = httpMock.expectOne('/api/employees/1/salary/history');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);
    req.flush(mockHistory);
  });

  it('should add a salary record with correct POST request body and withCredentials', () => {
    const request: SalaryRequest = {
      amount: 85000,
      currency: 'USD',
      effectiveFrom: '2026-06-01'
    };

    const createdResponse: SalaryResponse = {
      id: 2,
      employeeId: 1,
      ...request,
      createdAt: '2026-06-01T00:00:00Z'
    };

    service.addSalary(1, request).subscribe(res => {
      expect(res).toEqual(createdResponse);
    });

    const req = httpMock.expectOne('/api/employees/1/salary');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    expect(req.request.withCredentials).toBe(true);
    req.flush(createdResponse);
  });

  it('should handle 404 when no current salary exists for employee', () => {
    service.getCurrentSalary(99).subscribe({
      next: () => expect.fail('should have failed'),
      error: err => {
        expect(err.status).toBe(404);
      }
    });

    const req = httpMock.expectOne('/api/employees/99/salary');
    req.flush({ message: 'No salary currently effective for employee with id: 99' }, { status: 404, statusText: 'Not Found' });
  });

  it('should handle 400 validation error when adding salary with non-positive amount', () => {
    const invalidRequest: SalaryRequest = {
      amount: 0,
      currency: 'USD',
      effectiveFrom: '2026-01-01'
    };

    service.addSalary(1, invalidRequest).subscribe({
      next: () => expect.fail('should have failed'),
      error: err => {
        expect(err.status).toBe(400);
      }
    });

    const req = httpMock.expectOne('/api/employees/1/salary');
    req.flush({ message: 'Salary amount must be positive' }, { status: 400, statusText: 'Bad Request' });
  });
});
