import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('successful login updates authenticated state and user profile', () => {
    expect(service.isAuthenticated()).toBe(false);

    service.login({ username: 'hr.manager', password: 'password123' }).subscribe((res) => {
      expect(res.username).toBe('hr.manager');
    });

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush({ username: 'hr.manager', expiresInSeconds: 3600 });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()?.username).toBe('hr.manager');
  });

  it('failed login does not authenticate user', () => {
    expect(service.isAuthenticated()).toBe(false);

    service.login({ username: 'wrong', password: 'bad' }).subscribe({
      error: (err) => {
        expect(err.status).toBe(401);
      }
    });

    const req = httpMock.expectOne('/api/auth/login');
    req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
  });

  it('/me restores authenticated state', () => {
    service.fetchCurrentUser().subscribe((user) => {
      expect(user?.username).toBe('hr.manager');
    });

    const req = httpMock.expectOne('/api/auth/me');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);
    req.flush({ username: 'hr.manager' });

    expect(service.isAuthenticated()).toBe(true);
    expect(service.currentUser()?.username).toBe('hr.manager');
  });

  it('unauthenticated /me results in unauthenticated state', () => {
    service.fetchCurrentUser().subscribe((user) => {
      expect(user).toBeNull();
    });

    const req = httpMock.expectOne('/api/auth/me');
    req.flush({ message: 'Unauthenticated' }, { status: 401, statusText: 'Unauthorized' });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
  });

  it('logout clears authenticated state', () => {
    // Set initial logged in state
    service.fetchCurrentUser().subscribe();
    httpMock.expectOne('/api/auth/me').flush({ username: 'hr.manager' });
    expect(service.isAuthenticated()).toBe(true);

    service.logout().subscribe();

    const req = httpMock.expectOne('/api/auth/logout');
    expect(req.request.method).toBe('POST');
    expect(req.request.withCredentials).toBe(true);
    req.flush(null, { status: 200, statusText: 'OK' });

    expect(service.isAuthenticated()).toBe(false);
    expect(service.currentUser()).toBeNull();
  });
});
