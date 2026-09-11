import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SalaryRequest, SalaryResponse } from '../models/salary.model';

@Injectable({
  providedIn: 'root'
})
export class SalaryService {
  private readonly http = inject(HttpClient);

  getCurrentSalary(employeeId: number): Observable<SalaryResponse> {
    return this.http.get<SalaryResponse>(`/api/employees/${employeeId}/salary`, {
      withCredentials: true
    });
  }

  getSalaryHistory(employeeId: number): Observable<SalaryResponse[]> {
    return this.http.get<SalaryResponse[]>(`/api/employees/${employeeId}/salary/history`, {
      withCredentials: true
    });
  }

  addSalary(employeeId: number, request: SalaryRequest): Observable<SalaryResponse> {
    return this.http.post<SalaryResponse>(`/api/employees/${employeeId}/salary`, request, {
      withCredentials: true
    });
  }
}
