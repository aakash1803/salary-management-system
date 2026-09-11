import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="login-container">
      <div class="login-card card">
        <div class="login-header">
          <div class="login-brand-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="4" width="20" height="16" rx="2"></rect>
              <line x1="6" y1="12" x2="18" y2="12"></line>
              <line x1="12" y1="6" x2="12" y2="18"></line>
            </svg>
          </div>
          <h1 class="login-title">Salary Management System</h1>
          <p class="login-subtitle">Sign in to access your HR & Payroll dashboard</p>
        </div>

        @if (errorMessage()) {
          <div class="alert alert-error" role="alert">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>
            <span>{{ errorMessage() }}</span>
          </div>
        }

        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="login-form" novalidate>
          <div class="form-group">
            <label for="username" class="form-label">Username</label>
            <input
              id="username"
              type="text"
              formControlName="username"
              class="form-input"
              [class.is-invalid]="loginForm.controls.username.invalid && loginForm.controls.username.touched"
              placeholder="Enter username"
              autocomplete="username"
              required
            />
            @if (loginForm.controls.username.invalid && loginForm.controls.username.touched) {
              <span class="form-error">Username must be at least 3 characters.</span>
            }
          </div>

          <div class="form-group">
            <label for="password" class="form-label">Password</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              class="form-input"
              [class.is-invalid]="loginForm.controls.password.invalid && loginForm.controls.password.touched"
              placeholder="Enter password"
              autocomplete="current-password"
              required
            />
            @if (loginForm.controls.password.invalid && loginForm.controls.password.touched) {
              <span class="form-error">Password must be at least 6 characters.</span>
            }
          </div>

          <button
            type="submit"
            class="btn btn-primary login-btn"
            [disabled]="isLoading()"
          >
            @if (isLoading()) {
              <span class="spinner-sm"></span>
              <span>Signing in...</span>
            } @else {
              <span>Sign In</span>
            }
          </button>

          <div class="demo-helpers">
            <button type="button" class="btn-demo" (click)="toggleValidationErrorDemo()">
              Toggle Validation Error State Demo
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      width: 100%;
      max-width: 420px;
      padding: var(--space-md);
    }
    .login-card {
      padding: var(--space-2xl) var(--space-xl);
      box-shadow: var(--shadow-lg);
    }
    .login-header {
      text-align: center;
      margin-bottom: var(--space-xl);
    }
    .login-brand-icon {
      width: 52px;
      height: 52px;
      background-color: var(--color-primary-light);
      color: var(--color-primary);
      border-radius: var(--radius-lg);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      margin-bottom: var(--space-md);
      border: 1px solid var(--color-primary-border);
    }
    .login-title {
      font-size: var(--font-size-xl);
      font-weight: 700;
      color: var(--color-text-main);
      letter-spacing: -0.01em;
    }
    .login-subtitle {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      margin-top: var(--space-2xs);
    }
    .alert {
      display: flex;
      align-items: center;
      gap: var(--space-xs);
      padding: var(--space-sm) var(--space-md);
      border-radius: var(--radius-md);
      font-size: var(--font-size-xs);
      margin-bottom: var(--space-md);
    }
    .alert-error {
      background-color: var(--color-error-bg);
      color: var(--color-error);
      border: 1px solid var(--color-error-border);
    }
    .login-form {
      display: flex;
      flex-direction: column;
    }
    .login-btn {
      width: 100%;
      padding: var(--space-sm) var(--space-md);
      margin-top: var(--space-xs);
    }
    .spinner-sm {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255, 255, 255, 0.4);
      border-top-color: #ffffff;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }
    .demo-helpers {
      margin-top: var(--space-lg);
      padding-top: var(--space-md);
      border-top: 1px dashed var(--color-border);
      text-align: center;
    }
    .btn-demo {
      background: none;
      border: none;
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
      text-decoration: underline;
      cursor: pointer;
    }
    .btn-demo:hover {
      color: var(--color-primary);
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
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
