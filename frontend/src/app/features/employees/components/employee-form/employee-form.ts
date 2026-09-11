import { Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Employee } from '../../models/employee.model';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="empForm" (ngSubmit)="onSubmit()" class="employee-form" novalidate>
      <div class="form-grid">
        <!-- Employee Number -->
        <div class="form-group">
          <label for="empNumber" class="form-label">Employee Number *</label>
          <input
            id="empNumber"
            type="text"
            formControlName="employeeNumber"
            class="form-input"
            [class.is-invalid]="empForm.controls.employeeNumber.invalid && empForm.controls.employeeNumber.touched"
            placeholder="e.g. EMP-1015"
          />
          @if (empForm.controls.employeeNumber.invalid && empForm.controls.employeeNumber.touched) {
            <span class="form-error">Employee Number is required.</span>
          }
        </div>

        <!-- Email -->
        <div class="form-group">
          <label for="empEmail" class="form-label">Email Address *</label>
          <input
            id="empEmail"
            type="email"
            formControlName="email"
            class="form-input"
            [class.is-invalid]="empForm.controls.email.invalid && empForm.controls.email.touched"
            placeholder="john.doe@company.com"
          />
          @if (empForm.controls.email.invalid && empForm.controls.email.touched) {
            <span class="form-error">Valid email address is required.</span>
          }
        </div>

        <!-- First Name -->
        <div class="form-group">
          <label for="firstName" class="form-label">First Name *</label>
          <input
            id="firstName"
            type="text"
            formControlName="firstName"
            class="form-input"
            [class.is-invalid]="empForm.controls.firstName.invalid && empForm.controls.firstName.touched"
            placeholder="First name"
          />
          @if (empForm.controls.firstName.invalid && empForm.controls.firstName.touched) {
            <span class="form-error">First Name is required.</span>
          }
        </div>

        <!-- Last Name -->
        <div class="form-group">
          <label for="lastName" class="form-label">Last Name *</label>
          <input
            id="lastName"
            type="text"
            formControlName="lastName"
            class="form-input"
            [class.is-invalid]="empForm.controls.lastName.invalid && empForm.controls.lastName.touched"
            placeholder="Last name"
          />
          @if (empForm.controls.lastName.invalid && empForm.controls.lastName.touched) {
            <span class="form-error">Last Name is required.</span>
          }
        </div>

        <!-- Country Select -->
        <div class="form-group">
          <label for="country" class="form-label">Country *</label>
          <select
            id="country"
            formControlName="country"
            class="form-select"
            [class.is-invalid]="empForm.controls.country.invalid && empForm.controls.country.touched"
          >
            <option value="">Select Country</option>
            @for (c of countries; track c) {
              <option [value]="c">{{ c }}</option>
            }
          </select>
          @if (empForm.controls.country.invalid && empForm.controls.country.touched) {
            <span class="form-error">Country selection is required.</span>
          }
        </div>

        <!-- Department Select -->
        <div class="form-group">
          <label for="department" class="form-label">Department *</label>
          <select
            id="department"
            formControlName="department"
            class="form-select"
            [class.is-invalid]="empForm.controls.department.invalid && empForm.controls.department.touched"
          >
            <option value="">Select Department</option>
            @for (d of departments; track d) {
              <option [value]="d">{{ d }}</option>
            }
          </select>
          @if (empForm.controls.department.invalid && empForm.controls.department.touched) {
            <span class="form-error">Department selection is required.</span>
          }
        </div>
      </div>

      <div class="form-actions">
        <button type="button" class="btn btn-secondary" (click)="cancelForm.emit()">
          Cancel
        </button>
        <button type="submit" class="btn btn-primary">
          Save Employee
        </button>
      </div>
    </form>
  `,
  styles: [`
    .employee-form {
      display: flex;
      flex-direction: column;
      gap: var(--space-md);
    }
    .form-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: var(--space-md);
    }
    @media (min-width: 640px) {
      .form-grid {
        grid-template-columns: repeat(2, 1fr);
      }
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
export class EmployeeForm {
  readonly mode = input<'CREATE' | 'EDIT'>('CREATE');
  readonly employee = input<Employee | null>(null);

  readonly saveForm = output<Partial<Employee>>();
  readonly cancelForm = output<void>();

  readonly countries = ['United States', 'Germany', 'United Kingdom', 'Canada', 'Japan', 'France', 'India'];
  readonly departments = ['Engineering', 'Product', 'Finance', 'Human Resources', 'Marketing', 'Operations'];

  readonly empForm = new FormGroup({
    employeeNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    firstName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    lastName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    country: new FormControl('United States', { nonNullable: true, validators: [Validators.required] }),
    department: new FormControl('Engineering', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    effect(() => {
      const emp = this.employee();
      if (emp && this.mode() === 'EDIT') {
        this.empForm.patchValue({
          employeeNumber: emp.employeeNumber,
          firstName: emp.firstName,
          lastName: emp.lastName,
          email: emp.email,
          country: emp.country,
          department: emp.department,
        });
      } else if (this.mode() === 'CREATE' && !emp) {
        // Auto-generate a default emp number if empty
        const nextId = 'EMP-' + Math.floor(1015 + Math.random() * 800);
        this.empForm.reset({
          employeeNumber: nextId,
          firstName: '',
          lastName: '',
          email: '',
          country: 'United States',
          department: 'Engineering'
        });
      }
    });
  }

  onSubmit() {
    if (this.empForm.invalid) {
      this.empForm.markAllAsTouched();
      return;
    }

    const value = this.empForm.getRawValue();
    this.saveForm.emit(value);
  }
}
