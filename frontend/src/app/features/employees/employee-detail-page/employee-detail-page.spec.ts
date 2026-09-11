import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeDetailPage } from './employee-detail-page';
import { EmployeeResponse, EmployeeRequest } from '../models/employee.model';

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

  it('shows loading state while fetching employee details and does not show not-found prematurely', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.isLoading()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-loading-state')).toBeTruthy();
    expect(compiled.textContent).not.toContain('Employee Not Found');

    const req = httpMock.expectOne('/api/employees/1');
    req.flush(mockEmployee);
  });

  it('extracts correct numeric employee ID from route input and loads employee successfully', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/employees/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockEmployee);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.isLoading()).toBe(false);
    expect(component.employee()?.id).toBe(1);
    expect(component.employee()?.firstName).toBe('Sarah');

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sarah Jenkins');
    expect(compiled.textContent).toContain('EMP-1001');
    expect(compiled.textContent).toContain('United States');
    expect(compiled.textContent).toContain('Engineering');
  });

  it('does not fabricate salary data when EmployeeResponse contains no salary fields', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/employees/1');
    req.flush(mockEmployee);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No Active Salary');
    expect(compiled.textContent).toContain('No salary records found for this employee');
  });

  it('displays 404 not-found state when API returns HTTP 404', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '999');
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/employees/999');
    req.flush({ message: 'Employee not found' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.isLoading()).toBe(false);
    expect(component.isNotFound()).toBe(true);

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Employee Not Found');
  });

  it('displays generic API error state on non-404 HTTP failures', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/employees/1');
    req.flush({ message: 'Internal Server Error' }, { status: 500, statusText: 'Server Error' });
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

    const req = httpMock.expectOne('/api/employees/1');
    req.flush(mockEmployee);
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

    const req = httpMock.expectOne('/api/employees/1');
    req.flush(mockEmployee);
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

    const getReq = httpMock.expectOne('/api/employees/1');
    getReq.flush(mockEmployee);
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

    const getReq = httpMock.expectOne('/api/employees/1');
    getReq.flush(mockEmployee);
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
});
