import { TestBed } from '@angular/core/testing';
import { provideRouter, withComponentInputBinding, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { routes } from './app.routes';
import { AuthService } from './core/auth/services/auth.service';

describe('App Shell', () => {
  let authService: AuthService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter(routes, withComponentInputBinding()),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    authService = TestBed.inject(AuthService);
    // Mock user profile as logged in for route tests
    (authService as any).currentUserSignal.set({ username: 'hr.manager' });
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render application layout shell on authenticated pages', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    await router.navigateByUrl('/dashboard');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.sidebar')).toBeTruthy();
    expect(compiled.querySelector('.topbar')).toBeTruthy();
    expect(compiled.querySelector('.brand-title')?.textContent).toContain('Salary Management');
  });

  it('should hide application shell on login page', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    await router.navigateByUrl('/login');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.sidebar')).toBeFalsy();
    expect(compiled.querySelector('.topbar')).toBeFalsy();
    expect(compiled.querySelector('.auth-wrapper')).toBeTruthy();
  });

  it('navigates to /dashboard when accessing default route', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const router = TestBed.inject(Router);
    await router.navigateByUrl('/');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('HR Payroll Dashboard');
  });

  it('navigates to /employees/:id and renders employee details', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const httpMock = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/employees/1');
    fixture.detectChanges();

    const req = httpMock.expectOne('/api/employees/1');
    req.flush({
      id: 1,
      employeeNumber: 'EMP-1001',
      firstName: 'Sarah',
      lastName: 'Jenkins',
      email: 'sarah.jenkins@company.com',
      country: 'United States',
      department: 'Engineering'
    });

    httpMock.expectOne('/api/employees/1/salary').flush({ message: 'No salary' }, { status: 404, statusText: 'Not Found' });
    httpMock.expectOne('/api/employees/1/salary/history').flush([]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sarah Jenkins');
  });
});
