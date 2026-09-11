import { Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Employee, EmployeeRequest, EmployeeResponse } from '../../models/employee.model';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './employee-form.html',
  styleUrl: './employee-form.scss'
})
export class EmployeeForm {
  readonly mode = input<'CREATE' | 'EDIT'>('CREATE');
  readonly employee = input<EmployeeResponse | Employee | null>(null);
  readonly isLoading = input<boolean>(false);
  readonly errorMessage = input<string | null>(null);

  readonly saveForm = output<EmployeeRequest>();
  readonly cancelForm = output<void>();

  readonly countries = [
    'Canada',
    'France',
    'Germany',
    'India',
    'Japan',
    'United Kingdom',
    'United States'
  ];
  readonly departments = [
    'Engineering',
    'Executive',
    'Finance',
    'Human Resources',
    'Marketing',
    'Operations',
    'Product',
    'Sales',
    'Technology'
  ];

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
        this.empForm.reset({
          employeeNumber: '',
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

    const rawValue = this.empForm.getRawValue();
    const request: EmployeeRequest = {
      employeeNumber: rawValue.employeeNumber.trim(),
      firstName: rawValue.firstName.trim(),
      lastName: rawValue.lastName.trim(),
      email: rawValue.email.trim(),
      country: rawValue.country,
      department: rawValue.department
    };

    this.saveForm.emit(request);
  }
}
