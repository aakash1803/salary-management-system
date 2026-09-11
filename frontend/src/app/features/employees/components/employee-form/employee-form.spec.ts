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

  it('should emit saveForm when form is valid', () => {
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
    expect(emittedData).toBeTruthy();
    expect(emittedData.firstName).toBe('John');
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
