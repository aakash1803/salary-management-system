import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeListPage } from './employee-list-page';
import { PageResponse, EmployeeResponse } from '../models/employee.model';

describe('EmployeeListPage Component', () => {
  let httpMock: HttpTestingController;

  const mockPageResponse: PageResponse<EmployeeResponse> = {
    content: [
      {
        id: 1,
        employeeNumber: 'EMP-1001',
        firstName: 'Sarah',
        lastName: 'Jenkins',
        email: 'sarah.jenkins@company.com',
        country: 'United States',
        department: 'Engineering'
      },
      {
        id: 2,
        employeeNumber: 'EMP-1002',
        firstName: 'Marcus',
        lastName: 'Vance',
        email: 'marcus.vance@company.com',
        country: 'United States',
        department: 'Engineering'
      }
    ],
    page: 0,
    size: 5,
    totalElements: 12,
    totalPages: 3,
    first: true,
    last: false
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeListPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('requests the first page correctly mapping UI page number 1 to API page 0', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('5');
    req.flush(mockPageResponse);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.totalItems()).toBe(12);
    expect(component.totalPages()).toBe(3);
    expect(component.displayedEmployees().length).toBe(2);
    expect(component.rangeText()).toContain('Showing 1–5 of 12 employees');
  });

  it('passes search, country, and department filter values to the API', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    let req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush(mockPageResponse);

    const component = fixture.componentInstance;
    component.searchQuery.set('Sarah');
    component.selectedCountry.set('United States');
    component.selectedDepartment.set('Engineering');
    component.loadEmployees();

    req = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(req.request.params.get('search')).toBe('Sarah');
    expect(req.request.params.get('country')).toBe('United States');
    expect(req.request.params.get('department')).toBe('Engineering');
    expect(req.request.params.get('page')).toBe('0');
    req.flush(mockPageResponse);
  });

  it('updates pagination metadata and UI state when navigating to page 2', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    let req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush(mockPageResponse);

    const component = fixture.componentInstance;
    component.goToPage(2);

    req = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(req.request.params.get('page')).toBe('1');
    req.flush({
      ...mockPageResponse,
      page: 1,
      first: false,
      last: false
    });
    fixture.detectChanges();

    expect(component.currentPage()).toBe(2);
    expect(component.rangeText()).toContain('Showing 6–10 of 12 employees');
  });

  it('displays appropriate error state on HTTP failure rather than empty filter state', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush({ message: 'Server Error' }, { status: 500, statusText: 'Internal Server Error' });
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.errorMessage()).toBe('Server Error');
    expect(component.totalItems()).toBe(0);
    expect(component.displayedEmployees().length).toBe(0);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Unable to load employees');
    expect(compiled.textContent).toContain('Server Error');
    expect(compiled.textContent).not.toContain('No employees match your filters');
  });

  it('should open Add Employee modal when CTA button is clicked', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush(mockPageResponse);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openAddEmployeeModal();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-modal')).toBeTruthy();
    expect(compiled.querySelector('app-employee-form')).toBeTruthy();
  });
});
