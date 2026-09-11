import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { DashboardService } from './dashboard.service';
import { DashboardResponse } from '../models/dashboard.model';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  const mockDashboardResponse: DashboardResponse = {
    totalEmployees: 100,
    salaryMetricsByCurrency: [
      { currency: 'USD', minimum: 50000, maximum: 150000, average: 90000 },
      { currency: 'EUR', minimum: 45000, maximum: 120000, average: 75000 }
    ],
    employeesByCountry: [
      { country: 'United States', employeeCount: 60 },
      { country: 'Germany', employeeCount: 40 }
    ],
    salaryDistribution: [
      {
        currency: 'USD',
        bands: [
          { range: '30,000 - 59,999.99', employeeCount: 20 },
          { range: '60,000 - 99,999.99', employeeCount: 40 }
        ]
      }
    ],
    payrollByCountry: [
      { country: 'United States', currency: 'USD', totalPayroll: 5400000 }
    ],
    payrollByCurrency: [
      { currency: 'USD', totalPayroll: 5400000 }
    ]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        DashboardService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch dashboard data with withCredentials: true', () => {
    service.getDashboard().subscribe(response => {
      expect(response).toEqual(mockDashboardResponse);
      expect(response.totalEmployees).toBe(100);
      expect(response.salaryMetricsByCurrency.length).toBe(2);
    });

    const req = httpMock.expectOne('/api/dashboard');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);

    req.flush(mockDashboardResponse);
  });

  it('should propagate HTTP errors correctly', () => {
    let errorResponse: any = null;
    service.getDashboard().subscribe({
      next: () => {},
      error: (err) => {
        errorResponse = err;
      }
    });

    const req = httpMock.expectOne('/api/dashboard');
    req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });
    expect(errorResponse.status).toBe(500);
  });
});
