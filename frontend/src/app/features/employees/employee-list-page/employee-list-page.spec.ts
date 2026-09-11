import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { EmployeeListPage } from './employee-list-page';
import { PageResponse, EmployeeResponse, EmployeeRequest } from '../models/employee.model';

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
        department: 'Engineering',
        currentSalary: 95000,
        currency: 'USD'
      },
      {
        id: 2,
        employeeNumber: 'EMP-1002',
        firstName: 'Marcus',
        lastName: 'Vance',
        email: 'marcus.vance@company.com',
        country: 'United States',
        department: 'Engineering',
        currentSalary: null,
        currency: null
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

  it('renders current salary and currency for an employee that has an active salary record', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush(mockPageResponse);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('tbody tr');
    expect(rows[0].textContent).toContain('$95,000');
    expect(rows[0].textContent).toContain('USD');
  });

  it('renders a placeholder for an employee with no active salary record rather than inventing data', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush(mockPageResponse);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const rows = compiled.querySelectorAll('tbody tr');
    const secondRowCells = rows[1].querySelectorAll('td');
    // Current Salary is the 6th column, Currency the 7th (Employee #, Name, Email, Country,
    // Department, Current Salary, Currency, Actions).
    expect(secondRowCells[5].textContent?.trim()).toBe('—');
    expect(secondRowCells[6].textContent?.trim()).toBe('—');
  });

  it('debounces rapid search input resulting in only one API request after 300ms', () => {
    vi.useFakeTimers();

    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    // Initial load request
    let req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush(mockPageResponse);

    const component = fixture.componentInstance;

    // Rapid search input sequence
    component.onSearchChange({ target: { value: 'S' } } as any);
    vi.advanceTimersByTime(100);
    component.onSearchChange({ target: { value: 'Sa' } } as any);
    vi.advanceTimersByTime(100);
    component.onSearchChange({ target: { value: 'Sar' } } as any);
    vi.advanceTimersByTime(100);
    component.onSearchChange({ target: { value: 'Sarah' } } as any);

    // No request yet before 300ms has elapsed from last keystroke
    httpMock.expectNone((r) => r.url === '/api/employees');

    // Fast-forward remaining 300ms
    vi.advanceTimersByTime(300);

    // Single request fired with final search value
    req = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(req.request.params.get('search')).toBe('Sarah');
    req.flush(mockPageResponse);

    vi.useRealTimers();
  });

  it('does not trigger an API request for identical consecutive search values', () => {
    vi.useFakeTimers();

    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    let req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush(mockPageResponse);

    const component = fixture.componentInstance;

    // Initial search
    component.onSearchChange({ target: { value: 'Sarah' } } as any);
    vi.advanceTimersByTime(300);

    req = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(req.request.params.get('search')).toBe('Sarah');
    req.flush(mockPageResponse);

    // Repeated search input with identical value
    component.onSearchChange({ target: { value: 'Sarah' } } as any);
    vi.advanceTimersByTime(300);

    // distinctUntilChanged suppresses second request
    httpMock.expectNone((r) => r.url === '/api/employees');

    vi.useRealTimers();
  });

  it('triggers API request immediately on country or department change without debounce', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    let req = httpMock.expectOne((r) => r.url === '/api/employees');
    req.flush(mockPageResponse);

    const component = fixture.componentInstance;

    // Immediate country filter
    component.onCountryChange({ target: { value: 'United States' } } as any);

    req = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(req.request.params.get('country')).toBe('United States');
    req.flush(mockPageResponse);

    // Immediate department filter
    component.onDepartmentChange({ target: { value: 'Engineering' } } as any);

    req = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(req.request.params.get('department')).toBe('Engineering');
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

  it('calls EmployeeService.createEmployee with correct POST payload, closes modal and reloads employee list on success', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const initialReq = httpMock.expectOne((r) => r.url === '/api/employees');
    initialReq.flush(mockPageResponse);

    const component = fixture.componentInstance;
    component.openAddEmployeeModal();
    fixture.detectChanges();
    expect(component.isAddModalOpen()).toBe(true);

    const newEmpRequest: EmployeeRequest = {
      employeeNumber: 'EMP-2000',
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice.smith@company.com',
      country: 'Canada',
      department: 'Finance'
    };

    component.onSaveNewEmployee(newEmpRequest);
    expect(component.isSaving()).toBe(true);

    const postReq = httpMock.expectOne('/api/employees');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body).toEqual(newEmpRequest);
    expect(postReq.request.body.id).toBeUndefined();

    const createdEmpResponse: EmployeeResponse = {
      id: 10,
      ...newEmpRequest
    };
    postReq.flush(createdEmpResponse);

    expect(component.isSaving()).toBe(false);
    expect(component.isAddModalOpen()).toBe(false);

    // List reloaded on page 1
    const reloadReq = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(reloadReq.request.params.get('page')).toBe('0');
    reloadReq.flush(mockPageResponse);
  });

  it('keeps Add Employee modal open and displays error message when API returns 409 conflict', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const initialReq = httpMock.expectOne((r) => r.url === '/api/employees');
    initialReq.flush(mockPageResponse);

    const component = fixture.componentInstance;
    component.openAddEmployeeModal();
    fixture.detectChanges();

    const duplicateRequest: EmployeeRequest = {
      employeeNumber: 'EMP-1001',
      firstName: 'Sarah',
      lastName: 'Jenkins',
      email: 'sarah.jenkins@company.com',
      country: 'United States',
      department: 'Engineering'
    };

    component.onSaveNewEmployee(duplicateRequest);
    expect(component.isSaving()).toBe(true);

    const postReq = httpMock.expectOne('/api/employees');
    expect(postReq.request.method).toBe('POST');
    postReq.flush(
      { message: 'Employee number or email already exists' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(component.isSaving()).toBe(false);
    expect(component.isAddModalOpen()).toBe(true);
    expect(component.saveError()).toBe('Employee number or email already exists');
  });
});
