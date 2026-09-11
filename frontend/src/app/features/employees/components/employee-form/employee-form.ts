import { Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Employee } from '../../models/employee.model';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './employee-form.html',
  styleUrl: './employee-form.scss'
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
