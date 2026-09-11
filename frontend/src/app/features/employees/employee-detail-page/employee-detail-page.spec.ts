import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EmployeeDetailPage } from './employee-detail-page';

describe('EmployeeDetailPage Component', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeDetailPage],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should render employee profile and current compensation details', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sarah Jenkins');
    expect(compiled.textContent).toContain('EMP-1001');
    expect(compiled.textContent).toContain('Current Compensation');
    expect(compiled.textContent).toContain('Salary Change History');
  });

  it('should open Edit Employee modal and save changes', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openEditModal();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-modal')).toBeTruthy();
    expect(compiled.querySelector('app-employee-form')).toBeTruthy();

    component.onSaveEditEmployee({
      firstName: 'Sarah-Edited',
      lastName: 'Jenkins'
    });
    fixture.detectChanges();

    expect(component.employee().firstName).toBe('Sarah-Edited');
  });

  it('should open Add Salary Record modal and update salary history', () => {
    const fixture = TestBed.createComponent(EmployeeDetailPage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openSalaryModal();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-modal')).toBeTruthy();
    expect(compiled.querySelector('app-salary-form')).toBeTruthy();

    const today = new Date().toISOString().split('T')[0];
    component.onSaveSalaryRecord({
      amount: 180000,
      currency: 'USD',
      effectiveFrom: today
    });
    fixture.detectChanges();

    expect(component.employee().currentSalary).toBe(180000);
  });
});
