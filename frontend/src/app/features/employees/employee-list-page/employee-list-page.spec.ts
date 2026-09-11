import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EmployeeListPage } from './employee-list-page';

describe('EmployeeListPage Component', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeListPage],
      providers: [provideRouter([])]
    }).compileComponents();
  });

  it('should render employee table with correct headers and controls', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Employee Directory');
    expect(compiled.querySelector('input#empSearch')).toBeTruthy();
    expect(compiled.querySelector('select#countryFilter')).toBeTruthy();
    expect(compiled.querySelector('select#deptFilter')).toBeTruthy();
    expect(compiled.querySelectorAll('table.data-table tbody tr').length).toBeGreaterThan(0);
  });

  it('should derive total items and range text dynamically', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.totalItems()).toBe(12);
    expect(component.rangeText()).toContain('Showing 1–5 of 12 employees');
  });

  it('should open Add Employee modal when CTA button is clicked', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    component.openAddEmployeeModal();
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-modal')).toBeTruthy();
    expect(compiled.querySelector('app-employee-form')).toBeTruthy();
  });

  it('should add a new mock employee when EmployeeForm emits saveForm', () => {
    const fixture = TestBed.createComponent(EmployeeListPage);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    const initialCount = component.totalItems();

    component.onSaveNewEmployee({
      employeeNumber: 'EMP-9999',
      firstName: 'NewTest',
      lastName: 'User',
      email: 'newtest.user@company.com',
      country: 'United States',
      department: 'Engineering',
      currentSalary: 100000,
      currency: 'USD'
    });
    fixture.detectChanges();

    expect(component.totalItems()).toBe(initialCount + 1);
  });
});
