import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { EmployeeService } from './employee.service';
import { EmployeeResponse, PageResponse } from '../models/employee.model';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        EmployeeService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
    service = TestBed.inject(EmployeeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getEmployees() sends expected query parameters and parses PageResponse', () => {
    const mockResponse: PageResponse<EmployeeResponse> = {
      content: [
        {
          id: 1,
          employeeNumber: 'EMP-1001',
          firstName: 'Sarah',
          lastName: 'Jenkins',
          email: 'sarah.jenkins@company.com',
          country: 'United States',
          department: 'Engineering'
        }
      ],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
      first: true,
      last: true
    };

    service.getEmployees({
      search: 'Sarah',
      country: 'United States',
      department: 'Engineering',
      page: 0,
      size: 5,
      sortBy: 'employeeNumber',
      sortDirection: 'asc'
    }).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].firstName).toBe('Sarah');
      expect(res.totalElements).toBe(1);
    });

    const req = httpMock.expectOne((r) => r.url === '/api/employees');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('search')).toBe('Sarah');
    expect(req.request.params.get('country')).toBe('United States');
    expect(req.request.params.get('department')).toBe('Engineering');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('5');
    expect(req.request.params.get('sortBy')).toBe('employeeNumber');
    expect(req.request.params.get('sortDirection')).toBe('asc');
    expect(req.request.withCredentials).toBe(true);

    req.flush(mockResponse);
  });

  it('getEmployeeById() calls the correct URL', () => {
    const mockEmployee: EmployeeResponse = {
      id: 1,
      employeeNumber: 'EMP-1001',
      firstName: 'Sarah',
      lastName: 'Jenkins',
      email: 'sarah.jenkins@company.com',
      country: 'United States',
      department: 'Engineering'
    };

    service.getEmployeeById(1).subscribe((emp) => {
      expect(emp.id).toBe(1);
      expect(emp.firstName).toBe('Sarah');
    });

    const req = httpMock.expectOne('/api/employees/1');
    expect(req.request.method).toBe('GET');
    expect(req.request.withCredentials).toBe(true);

    req.flush(mockEmployee);
  });
});
