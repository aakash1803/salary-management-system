import { Component, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-salary-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './salary-form.html',
  styleUrl: './salary-form.scss'
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
