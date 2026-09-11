import { TestBed } from '@angular/core/testing';
import { SalaryForm } from './salary-form';

describe('SalaryForm Component', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SalaryForm]
    }).compileComponents();
  });

  it('should render salary form controls and explanation text', () => {
    const fixture = TestBed.createComponent(SalaryForm);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('#amount')).toBeTruthy();
    expect(compiled.querySelector('#currency')).toBeTruthy();
    expect(compiled.querySelector('#effectiveFrom')).toBeTruthy();
    expect(compiled.textContent).toContain('Salary changes are stored as new records');
  });

  it('should validate amount > 0 and require fields', () => {
    const fixture = TestBed.createComponent(SalaryForm);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    component.salaryForm.controls.amount.setValue(0);
    component.onSubmit();
    expect(component.salaryForm.invalid).toBe(true);

    component.salaryForm.controls.amount.setValue(85000);
    expect(component.salaryForm.valid).toBe(true);
  });

  it('should emit saveForm with numeric amount and dates on valid submit', () => {
    const fixture = TestBed.createComponent(SalaryForm);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    let emittedData: any = null;
    component.saveForm.subscribe((data) => {
      emittedData = data;
    });

    component.salaryForm.controls.amount.setValue(110000);
    component.salaryForm.controls.currency.setValue('EUR');
    component.salaryForm.controls.effectiveFrom.setValue('2025-06-01');

    component.onSubmit();
    expect(emittedData).toBeTruthy();
    expect(emittedData.amount).toBe(110000);
    expect(emittedData.currency).toBe('EUR');
  });

  it('should emit cancelForm on cancel click', () => {
    const fixture = TestBed.createComponent(SalaryForm);
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
