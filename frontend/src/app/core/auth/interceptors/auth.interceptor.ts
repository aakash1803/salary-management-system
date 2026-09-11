import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  const clonedReq = req.clone({ withCredentials: true });

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        const url = req.url;
        const isAuthEndpoint = url.includes('/api/auth/login') || url.includes('/api/auth/me');

        if (!isAuthEndpoint) {
          authService.clearAuthState();
          router.navigate(['/login']);
        }
      }
      return throwError(() => error);
    })
  );
};
