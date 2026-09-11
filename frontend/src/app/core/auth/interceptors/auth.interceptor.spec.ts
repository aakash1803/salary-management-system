import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let router: Router;
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        AuthService,
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    authService = TestBed.inject(AuthService);
    vi.spyOn(router, 'navigate');
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('sends requests withCredentials: true', () => {
    httpClient.get('/api/employees').subscribe();

    const req = httpMock.expectOne('/api/employees');
    expect(req.request.withCredentials).toBe(true);
    req.flush([]);
  });

  it('does NOT attach Authorization header', () => {
    httpClient.get('/api/employees').subscribe();

    const req = httpMock.expectOne('/api/employees');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('redirects to /login and clears auth state on HTTP 401 for normal protected requests', () => {
    const clearSpy = vi.spyOn(authService, 'clearAuthState');

    httpClient.get('/api/protected-data').subscribe({
      error: (err) => {
        expect(err.status).toBe(401);
      }
    });

    const req = httpMock.expectOne('/api/protected-data');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(clearSpy).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('avoids redirect loop when 401 is received from /api/auth/login', () => {
    const clearSpy = vi.spyOn(authService, 'clearAuthState');

    httpClient.post('/api/auth/login', { username: 'hr', password: 'bad' }).subscribe({
      error: (err) => {
        expect(err.status).toBe(401);
      }
    });

    const req = httpMock.expectOne('/api/auth/login');
    req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

    expect(clearSpy).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('avoids redirect loop when 401 is received from /api/auth/me', () => {
    const clearSpy = vi.spyOn(authService, 'clearAuthState');

    httpClient.get('/api/auth/me').subscribe({
      error: (err) => {
        expect(err.status).toBe(401);
      }
    });

    const req = httpMock.expectOne('/api/auth/me');
    req.flush({ message: 'Unauthenticated' }, { status: 401, statusText: 'Unauthorized' });

    expect(clearSpy).not.toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
