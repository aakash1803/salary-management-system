import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { DashboardPage } from './dashboard-page';
import { DashboardService } from '../services/dashboard.service';
import { DashboardResponse } from '../models/dashboard.model';

describe('DashboardPage Component', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let dashboardServiceMock: { getDashboard: ReturnType<typeof vi.fn> };

  const mockResponse: DashboardResponse = {
    totalEmployees: 10000,
    salaryMetricsByCurrency: [
      { currency: 'USD', minimum: 35000, maximum: 220000, average: 95000 },
      { currency: 'EUR', minimum: 30000, maximum: 180000, average: 80000 }
    ],
    employeesByCountry: [
      { country: 'United States', employeeCount: 6000 },
      { country: 'Germany', employeeCount: 4000 }
    ],
    salaryDistribution: [
      {
        currency: 'USD',
        bands: [
          { range: '30,000 - 59,999.99', employeeCount: 2000 },
          { range: '60,000 - 99,999.99', employeeCount: 5000 },
          { range: '100,000 - 149,999.99', employeeCount: 3000 }
        ]
      }
    ],
    payrollByCountry: [
      { country: 'United States', currency: 'USD', totalPayroll: 570000000 },
      { country: 'Germany', currency: 'EUR', totalPayroll: 320000000 }
    ],
    payrollByCurrency: [
      { currency: 'USD', totalPayroll: 570000000 },
      { currency: 'EUR', totalPayroll: 320000000 }
    ]
  };

  beforeEach(async () => {
    dashboardServiceMock = {
      getDashboard: vi.fn().mockReturnValue(of(mockResponse))
    };

    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        { provide: DashboardService, useValue: dashboardServiceMock }
      ]
    }).compileComponents();
  });

  it('should display loading state initially', () => {
    fixture = TestBed.createComponent(DashboardPage);
    expect(fixture.componentInstance.loading()).toBe(true);
  });

  it('should render API metrics and total employee count when loaded', () => {
    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('10000');
    expect(compiled.textContent).toContain('Total Employees');
    expect(compiled.textContent).toContain('United States');
    expect(compiled.textContent).toContain('6000 employees (60%)');
    expect(compiled.textContent).toContain('Germany');
    expect(compiled.textContent).toContain('4000 employees (40%)');

    // Verify no old mock values remain
    expect(compiled.textContent).not.toContain('148');
    expect(compiled.textContent).not.toContain('$84,250');
    expect(compiled.textContent).not.toContain('$195,000');
  });

  it('should handle multi-currency salary metrics correctly', () => {
    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('$95,000');
    expect(compiled.textContent).toContain('$35,000');
    expect(compiled.textContent).toContain('$220,000');

    // Switch currency to EUR
    fixture.componentInstance.selectedCurrency.set('EUR');
    fixture.detectChanges();

    expect(compiled.textContent).toContain('€80,000');
    expect(compiled.textContent).toContain('€30,000');
    expect(compiled.textContent).toContain('€180,000');
  });

  it('should render country distribution, salary distribution, and payroll tables from API', () => {
    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Employee Distribution by Country');
    expect(compiled.textContent).toContain('Salary Band Breakdown');
    expect(compiled.textContent).toContain('Global Payroll Summary by Country & Currency');
    expect(compiled.textContent).toContain('Total Payroll Summary by Currency');
    expect(compiled.textContent).toContain('$570,000,000');
    expect(compiled.textContent).toContain('€320,000,000');
  });

  it('should render error state and support retry action', () => {
    dashboardServiceMock.getDashboard.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Server Error' }))
    );

    fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Failed to load dashboard data');

    // Test retry
    dashboardServiceMock.getDashboard.mockReturnValue(of(mockResponse));
    const retryBtn = compiled.querySelector('button') as HTMLButtonElement;
    expect(retryBtn).not.toBeNull();
    retryBtn.click();
    fixture.detectChanges();

    expect(compiled.textContent).toContain('10000');
    expect(compiled.textContent).not.toContain('Failed to load dashboard data');
  });

  it('should format currency amounts cleanly', () => {
    fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;

    expect(component.formatCurrency(125000, 'USD')).toContain('125,000');
    expect(component.formatCurrency(null, 'USD')).toBe('N/A');
  });
});
