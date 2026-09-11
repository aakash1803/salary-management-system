import { TestBed } from '@angular/core/testing';
import { EmployeeForm } from './employee-form';

describe('EmployeeForm Component', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeForm]
    }).compileComponents();
  });

  it('should render form fields with default CREATE values', () => {
    const fixture = TestBed.createComponent(EmployeeForm);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#empNumber')).toBeTruthy();
    expect(compiled.querySelector('#firstName')).toBeTruthy();
    expect(compiled.querySelector('#lastName')).toBeTruthy();
    expect(compiled.querySelector('#empEmail')).toBeTruthy();
    expect(compiled.querySelector('#country')).toBeTruthy();
    expect(compiled.querySelector('#department')).toBeTruthy();
  });

  it('should validate required fields and prevent invalid submission', () => {
    const fixture = TestBed.createComponent(EmployeeForm);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.empForm.controls.firstName.setValue('');
    component.onSubmit();
    fixture.detectChanges();

    expect(component.empForm.invalid).toBe(true);
  });

  it('should emit exact EmployeeRequest payload without id or salary fields when valid', () => {
    const fixture = TestBed.createComponent(EmployeeForm);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    let emittedData: any = null;
    component.saveForm.subscribe((data) => {
      emittedData = data;
    });

    component.empForm.controls.employeeNumber.setValue('EMP-999');
    component.empForm.controls.firstName.setValue('John');
    component.empForm.controls.lastName.setValue('Doe');
    component.empForm.controls.email.setValue('john.doe@company.com');
    component.empForm.controls.country.setValue('United States');
    component.empForm.controls.department.setValue('Engineering');

    component.onSubmit();
    expect(emittedData).toEqual({
      employeeNumber: 'EMP-999',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john.doe@company.com',
      country: 'United States',
      department: 'Engineering'
    });
    expect(emittedData.id).toBeUndefined();
    expect(emittedData.currentSalary).toBeUndefined();
  });

  it('should render error alert message when errorMessage input is passed', () => {
    const fixture = TestBed.createComponent(EmployeeForm);
    fixture.componentRef.setInput('errorMessage', 'Employee number already exists');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.alert-error')?.textContent).toContain('Employee number already exists');
  });

  it('should disable submit and cancel buttons when isLoading input is true', () => {
    const fixture = TestBed.createComponent(EmployeeForm);
    fixture.componentRef.setInput('isLoading', true);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const submitBtn = compiled.querySelector('button[type="submit"]') as HTMLButtonElement;
    const cancelBtn = compiled.querySelector('button[type="button"]') as HTMLButtonElement;

    expect(submitBtn.disabled).toBe(true);
    expect(cancelBtn.disabled).toBe(true);
    expect(submitBtn.textContent).toContain('Saving...');
  });

  it('should emit cancelForm when cancel button is clicked', () => {
    const fixture = TestBed.createComponent(EmployeeForm);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    let cancelled = false;
    component.cancelForm.subscribe(() => {
      cancelled = true;
    });

    const cancelBtn = fixture.nativeElement.querySelector('button[type="button"]') as HTMLButtonElement;
    cancelBtn.click();
    expect(cancelled).toBe(true);
  });
});
