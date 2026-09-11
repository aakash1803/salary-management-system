import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EmployeeQueryParams,
  EmployeeRequest,
  EmployeeResponse,
  PageResponse
} from '../models/employee.model';

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {
  private readonly http = inject(HttpClient);

  getEmployees(params?: EmployeeQueryParams): Observable<PageResponse<EmployeeResponse>> {
    let httpParams = new HttpParams();

    if (params) {
      if (params.search) {
        httpParams = httpParams.set('search', params.search);
      }
      if (params.country) {
        httpParams = httpParams.set('country', params.country);
      }
      if (params.department) {
        httpParams = httpParams.set('department', params.department);
      }
      if (params.page !== undefined && params.page !== null) {
        httpParams = httpParams.set('page', params.page.toString());
      }
      if (params.size !== undefined && params.size !== null) {
        httpParams = httpParams.set('size', params.size.toString());
      }
      if (params.sortBy) {
        httpParams = httpParams.set('sortBy', params.sortBy);
      }
      if (params.sortDirection) {
        httpParams = httpParams.set('sortDirection', params.sortDirection);
      }
    }

    return this.http.get<PageResponse<EmployeeResponse>>('/api/employees', {
      params: httpParams,
      withCredentials: true
    });
  }

  getEmployeeById(id: number): Observable<EmployeeResponse> {
    return this.http.get<EmployeeResponse>(`/api/employees/${id}`, {
      withCredentials: true
    });
  }

  createEmployee(request: EmployeeRequest): Observable<EmployeeResponse> {
    return this.http.post<EmployeeResponse>('/api/employees', request, {
      withCredentials: true
    });
  }

  updateEmployee(id: number, request: EmployeeRequest): Observable<EmployeeResponse> {
    return this.http.put<EmployeeResponse>(`/api/employees/${id}`, request, {
      withCredentials: true
    });
  }
}
