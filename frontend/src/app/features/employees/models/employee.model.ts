export interface SalaryRecord {
  id: string;
  effectiveFrom: string;
  amount: number;
  currency: string;
  createdAt: string;
}

export interface Employee {
  id: string;
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
