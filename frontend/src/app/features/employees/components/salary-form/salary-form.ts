import { Component, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-salary-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="salaryForm" (ngSubmit)="onSubmit()" class="salary-form" novalidate>
      <div class="callout-info">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="info-icon"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
        <span>Salary changes are stored as new records. Previous salary history is preserved.</span>
      </div>

      <div class="form-group">
        <label for="amount" class="form-label">Salary Amount *</label>
        <div class="input-with-prefix">
          <input
            id="amount"
            type="number"
            formControlName="amount"
            class="form-input"
            [class.is-invalid]="salaryForm.controls.amount.invalid && salaryForm.controls.amount.touched"
            placeholder="e.g. 95000"
            min="1"
            step="100"
          />
        </div>
        @if (salaryForm.controls.amount.invalid && salaryForm.controls.amount.touched) {
          <span class="form-error">Salary amount is required and must be greater than 0.</span>
        }
      </div>

      <div class="form-group">
        <label for="currency" class="form-label">Currency *</label>
        <select
          id="currency"
          formControlName="currency"
          class="form-select"
          [class.is-invalid]="salaryForm.controls.currency.invalid && salaryForm.controls.currency.touched"
        >
          @for (c of currencies; track c) {
            <option [value]="c">{{ c }}</option>
          }
        </select>
        @if (salaryForm.controls.currency.invalid && salaryForm.controls.currency.touched) {
          <span class="form-error">Currency selection is required.</span>
        }
      </div>

      <div class="form-group">
        <label for="effectiveFrom" class="form-label">Effective From *</label>
        <input
          id="effectiveFrom"
          type="date"
          formControlName="effectiveFrom"
          class="form-input"
          [class.is-invalid]="salaryForm.controls.effectiveFrom.invalid && salaryForm.controls.effectiveFrom.touched"
        />
        @if (salaryForm.controls.effectiveFrom.invalid && salaryForm.controls.effectiveFrom.touched) {
          <span class="form-error">Effective date is required.</span>
        }
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" (click)="cancelForm.emit()">
          Cancel
        </button>
        <button type="submit" class="btn btn-primary">
          Add Salary Record
        </button>
      </div>
    </form>
  `,
  styles: [`
    .salary-form {
      display: flex;
      flex-direction: column;
      gap: var(--space-md);
    }
    .callout-info {
      display: flex;
      align-items: flex-start;
      gap: var(--space-xs);
      padding: var(--space-sm) var(--space-md);
      background-color: var(--color-info-bg);
      color: var(--color-info);
      border: 1px solid var(--color-primary-border);
      border-radius: var(--radius-md);
      font-size: var(--font-size-xs);
      line-height: 1.4;
    }
    .info-icon {
      flex-shrink: 0;
      margin-top: 1px;
    }
    .form-actions {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: var(--space-sm);
      margin-top: var(--space-md);
      padding-top: var(--space-md);
      border-top: 1px solid var(--color-border);
    }
  `]
})
export class SalaryForm {
  readonly defaultCurrency = input<string>('USD');
  readonly saveForm = output<{ amount: number; currency: string; effectiveFrom: string }>();
  readonly cancelForm = output<void>();

  readonly currencies = ['USD', 'EUR', 'GBP', 'CAD', 'AUD', 'INR', 'JPY'];

  readonly todayStr = new Date().toISOString().split('T')[0];

  readonly salaryForm = new FormGroup({
    amount: new FormControl<number | null>(null, { validators: [Validators.required, Validators.min(0.01)] }),
    currency: new FormControl('USD', { nonNullable: true, validators: [Validators.required] }),
    effectiveFrom: new FormControl(this.todayStr, { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    // Default currency if provided
    if (this.defaultCurrency()) {
      this.salaryForm.patchValue({ currency: this.defaultCurrency() });
    }
  }

  onSubmit() {
    if (this.salaryForm.invalid) {
      this.salaryForm.markAllAsTouched();
      return;
    }

    const val = this.salaryForm.getRawValue();
    this.saveForm.emit({
      amount: Number(val.amount),
      currency: val.currency,
      effectiveFrom: val.effectiveFrom
    });
  }
}
