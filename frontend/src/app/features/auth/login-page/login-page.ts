import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss'
})
export class LoginPage {
  private readonly router = inject(Router);

  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly loginForm = new FormGroup({
    username: new FormControl('hr.manager', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3)]
    }),
    password: new FormControl('password123', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(6)]
    })
  });

  onSubmit() {
    this.errorMessage.set(null);

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.errorMessage.set('Please fill in all required fields correctly.');
      return;
    }

    this.isLoading.set(true);

    setTimeout(() => {
      this.isLoading.set(false);
      this.router.navigate(['/dashboard']);
    }, 600);
  }

  toggleValidationErrorDemo() {
    if (this.errorMessage()) {
      this.errorMessage.set(null);
    } else {
      this.loginForm.controls.username.setValue('');
      this.loginForm.controls.password.setValue('');
      this.loginForm.markAllAsTouched();
      this.errorMessage.set('Invalid username or password. (Demo validation error state)');
    }
  }
}
