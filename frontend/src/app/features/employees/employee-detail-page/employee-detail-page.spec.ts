import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeDetailPage } from './employee-detail-page';
import { EmployeeResponse, EmployeeRequest } from '../models/employee.model';
import { SalaryRequest, SalaryResponse } from '../models/salary.model';

describe('EmployeeDetailPage Component', () => {
  let httpMock: HttpTestingController;

  const mockEmployee: EmployeeResponse = {
    id: 1,
    employeeNumber: 'EMP-1001',
    firstName: 'Sarah',
    lastName: 'Jenkins',
    email: 'sarah.jenkins@company.com',
    country: 'United States',
    department: 'Engineering'
  };

  const mockSalary: SalaryResponse = {
    id: 10,
    employeeId: 1,
    amount: 95000,
    currency: 'USD',
    effectiveFrom: '2026-01-01',
    createdAt: '2026-01-01T00:00:00Z'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeDetailPage],
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

  function flushEmployeeRequests(
    empId = 1,
    empResponse = mockEmployee,
    currentSalaryResponse: SalaryResponse | null = null,
    salaryHistoryResponse: SalaryResponse[] = []
  ) {
    const empReq = httpMock.expectOne(`/api/employees/${empId}`);
    empReq.flush(empResponse);

    const currentReq = httpMock.expectOne(`/api/employees/${empId}/salary`);
    if (currentSalaryResponse) {
      currentReq.flush(currentSalaryResponse);
    } else {
      currentReq.flush({ message: 'No salary' }, { status: 404, statusText: 'Not Found' });
    }

    const historyReq = httpMock.expectOne(`/api/employees/${empId}/salary/history`);
    historyReq.flush(salaryHistoryResponse);
  }

  it('shows loading state while fetching employee details and does not show not-found prematurely', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.isLoading()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-loading-state')).toBeTruthy();
    expect(compiled.textContent).not.toContain('Employee Not Found');

    flushEmployeeRequests(1, mockEmployee, null, []);
  });

  it('extracts correct numeric employee ID from route input and loads employee details and salary data successfully', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, mockSalary, [mockSalary]);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.isLoading()).toBe(false);
    expect(component.employee()?.id).toBe(1);
    expect(component.employee()?.firstName).toBe('Sarah');
    expect(component.currentSalary()?.amount).toBe(95000);
    expect(component.salaryHistory().length).toBe(1);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sarah Jenkins');
    expect(compiled.textContent).toContain('$95,000');
  });

  it('handles current salary 404 cleanly with No Active Salary badge and no page-level error', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, null, []);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.currentSalary()).toBeNull();
    expect(component.currentSalaryError()).toBeNull();
    expect(component.errorMessage()).toBeNull();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No Active Salary');
    expect(compiled.textContent).not.toContain('No History');
  });

  it('surfaces current salary 500 error in Current Compensation card without failing page load', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const empReq = httpMock.expectOne('/api/employees/1');
    empReq.flush(mockEmployee);

    httpMock.expectOne('/api/employees/1/salary').flush(
      { message: 'Salary Service Error' },
      { status: 500, statusText: 'Internal Server Error' }
    );
    httpMock.expectOne('/api/employees/1/salary/history').flush([]);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.currentSalary()).toBeNull();
    expect(component.currentSalaryError()).toBe('Salary Service Error');
    expect(component.errorMessage()).toBeNull();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Salary Service Error');
  });

  it('surfaces salary history 500 error in history table instead of displaying no records', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const empReq = httpMock.expectOne('/api/employees/1');
    empReq.flush(mockEmployee);

    httpMock.expectOne('/api/employees/1/salary').flush(mockSalary);
    httpMock.expectOne('/api/employees/1/salary/history').flush(
      { message: 'History Database Error' },
      { status: 500, statusText: 'Internal Server Error' }
    );
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.salaryHistoryError()).toBe('History Database Error');
    expect(component.errorMessage()).toBeNull();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('History Database Error');
    expect(compiled.textContent).not.toContain('No salary records found');
  });

  it('renders active salary highlight card and history table normally when both APIs succeed', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, mockSalary, [mockSalary]);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.currentSalary()?.amount).toBe(95000);
    expect(component.salaryHistory().length).toBe(1);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('$95,000');
    expect(compiled.textContent).toContain('Active');
  });

  it('displays No Active Salary badge and renders Future record when only future-dated salary exists', () => {
    const futureSalary: SalaryResponse = {
      id: 99,
      employeeId: 1,
      amount: 120000,
      currency: 'USD',
      effectiveFrom: '2099-01-01',
      createdAt: '2026-01-01T00:00:00Z'
    };

    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, null, [futureSalary]);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.currentSalary()).toBeNull();
    expect(component.salaryHistory().length).toBe(1);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No Active Salary');
    expect(compiled.textContent).not.toContain('No History');
    expect(compiled.textContent).toContain('Future');
    expect(compiled.textContent).toContain('$120,000');
  });

  it('displays 404 not-found state when Employee API returns HTTP 404', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '999');
    fixture.detectChanges();

    const empReq = httpMock.expectOne('/api/employees/999');
    empReq.flush({ message: 'Employee not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.isLoading()).toBe(false);
    expect(component.isNotFound()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Employee Not Found');
  });

  it('displays generic API error state on non-404 HTTP failures for employee fetch', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const empReq = httpMock.expectOne('/api/employees/1');
    empReq.flush({ message: 'Internal Server Error' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBe('Internal Server Error');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Unable to load employee details');
    expect(compiled.textContent).toContain('Internal Server Error');
  });

  it('opens Edit Employee modal when button is clicked', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, null, []);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openEditModal();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-modal')).toBeTruthy();
    expect(compiled.querySelector('app-employee-form')).toBeTruthy();
  });

  it('opens Add Salary Record modal when button is clicked', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, null, []);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openSalaryModal();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-modal')).toBeTruthy();
    expect(compiled.querySelector('app-salary-form')).toBeTruthy();
  });

  it('calls EmployeeService.updateEmployee with correct PUT payload, closes modal and updates displayed employee on success', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, null, []);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openEditModal();
    fixture.detectChanges();
    expect(component.isEditModalOpen()).toBe(true);

    const updateRequest: EmployeeRequest = {
      employeeNumber: 'EMP-1001',
      firstName: 'Sarah Updated',
      lastName: 'Jenkins',
      email: 'sarah.updated@company.com',
      country: 'United States',
      department: 'Executive'
    };

    component.onSaveEditEmployee(updateRequest);
    expect(component.isSaving()).toBe(true);

    const putReq = httpMock.expectOne('/api/employees/1');
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body).toEqual(updateRequest);
    expect(putReq.request.body.id).toBeUndefined();

    const updatedEmployeeResponse: EmployeeResponse = {
      id: 1,
      ...updateRequest
    };
    putReq.flush(updatedEmployeeResponse);
    fixture.detectChanges();

    expect(component.isSaving()).toBe(false);
    expect(component.isEditModalOpen()).toBe(false);
    expect(component.employee()?.firstName).toBe('Sarah Updated');
    expect(component.employee()?.email).toBe('sarah.updated@company.com');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sarah Updated Jenkins');
  });

  it('keeps Edit Employee modal open and displays error message when API returns 409 conflict on update', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, null, []);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openEditModal();
    fixture.detectChanges();

    const updateRequest: EmployeeRequest = {
      employeeNumber: 'EMP-1001',
      firstName: 'Sarah',
      lastName: 'Jenkins',
      email: 'existing.email@company.com',
      country: 'United States',
      department: 'Engineering'
    };

    component.onSaveEditEmployee(updateRequest);
    expect(component.isSaving()).toBe(true);

    const putReq = httpMock.expectOne('/api/employees/1');
    expect(putReq.request.method).toBe('PUT');
    putReq.flush(
      { message: 'Email address is already taken' },
      { status: 409, statusText: 'Conflict' }
    );
    fixture.detectChanges();

    expect(component.isSaving()).toBe(false);
    expect(component.isEditModalOpen()).toBe(true);
    expect(component.saveError()).toBe('Email address is already taken');
    expect(component.employee()?.email).toBe('sarah.jenkins@company.com');
  });

  it('calls SalaryService.addSalary with correct POST payload, closes modal and reloads salary data on success', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, null, []);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openSalaryModal();
    fixture.detectChanges();
    expect(component.isSalaryModalOpen()).toBe(true);

    const salaryRequest: SalaryRequest = {
      amount: 105000,
      currency: 'USD',
      effectiveFrom: '2026-06-01'
    };

    component.onSaveSalaryRecord(salaryRequest);
    expect(component.isSavingSalary()).toBe(true);

    const postReq = httpMock.expectOne('/api/employees/1/salary');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body).toEqual(salaryRequest);

    const createdSalary: SalaryResponse = {
      id: 20,
      employeeId: 1,
      ...salaryRequest,
      createdAt: '2026-06-01T00:00:00Z'
    };
    postReq.flush(createdSalary);

    // Expect re-fetch of current salary and salary history
    httpMock.expectOne('/api/employees/1/salary').flush(createdSalary);
    httpMock.expectOne('/api/employees/1/salary/history').flush([createdSalary]);
    fixture.detectChanges();

    expect(component.isSavingSalary()).toBe(false);
    expect(component.isSalaryModalOpen()).toBe(false);
    expect(component.currentSalary()?.amount).toBe(105000);
    expect(component.salaryHistory().length).toBe(1);
  });

  it('keeps Add Salary Record modal open and displays error message when API returns 400 validation failure', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    flushEmployeeRequests(1, mockEmployee, null, []);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openSalaryModal();
    fixture.detectChanges();

    const invalidSalaryRequest: SalaryRequest = {
      amount: 0,
      currency: 'USD',
      effectiveFrom: '2026-01-01'
    };

    component.onSaveSalaryRecord(invalidSalaryRequest);
    expect(component.isSavingSalary()).toBe(true);

    const postReq = httpMock.expectOne('/api/employees/1/salary');
    expect(postReq.request.method).toBe('POST');
    postReq.flush(
      { message: 'Salary amount must be positive' },
      { status: 400, statusText: 'Bad Request' }
    );
    fixture.detectChanges();

    expect(component.isSavingSalary()).toBe(false);
    expect(component.isSalaryModalOpen()).toBe(true);
    expect(component.salaryError()).toBe('Salary amount must be positive');
  });
});
