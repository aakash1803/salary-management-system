import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { LoginRequest, LoginResponse, UserProfile } from '../models/auth.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly currentUserSignal = signal<UserProfile | null>(null);
  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.currentUserSignal());

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', credentials, { withCredentials: true }).pipe(
      tap((response) => {
        this.currentUserSignal.set({ username: response.username });
      })
    );
  }

  fetchCurrentUser(): Observable<UserProfile | null> {
    return this.http.get<UserProfile>('/api/auth/me', { withCredentials: true }).pipe(
      tap((profile) => {
        this.currentUserSignal.set({ username: profile.username });
      }),
      catchError(() => {
        this.currentUserSignal.set(null);
        return of(null);
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', {}, { withCredentials: true }).pipe(
      tap(() => {
        this.currentUserSignal.set(null);
      }),
      catchError(() => {
        this.currentUserSignal.set(null);
        return of(void 0);
      })
    );
  }

  clearAuthState(): void {
    this.currentUserSignal.set(null);
  }
}
