export interface EmployeeResponse {
  id: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  country: string;
  department: string;
  currentSalary?: number | null;
  currency?: string | null;
}

export interface EmployeeRequest {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  country: string;
  department: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface EmployeeQueryParams {
  search?: string;
  country?: string;
  department?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: 'asc' | 'desc';
}

export interface SalaryRecord {
  id: string;
  effectiveFrom: string;
  amount: number;
  currency: string;
  createdAt: string;
}

export interface Employee {
  id: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  country: string;
  department: string;
  currentSalary?: number;
  currency?: string;
  effectiveFrom?: string;
  salaryHistory: SalaryRecord[];
}

export interface PayrollSummary {
  country: string;
  currency: string;
  employeeCount: number;
  totalPayroll: number;
  averageSalary: number;
}

export interface SalaryBand {
  label: string;
  min: number;
  max: number;
  count: number;
  percentage: number;
}
