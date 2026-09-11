export interface SalaryMetrics {
  currency: string;
  minimum: number;
  maximum: number;
  average: number;
}

export interface EmployeeCountByCountry {
  country: string;
  employeeCount: number;
}

export interface SalaryBand {
  range: string;
  employeeCount: number;
}

export interface SalaryDistributionByCurrency {
  currency: string;
  bands: SalaryBand[];
}

export interface PayrollByCountry {
  country: string;
  currency: string;
  totalPayroll: number;
}

export interface PayrollByCurrency {
  currency: string;
  totalPayroll: number;
}

export interface DashboardResponse {
  totalEmployees: number;
  salaryMetricsByCurrency: SalaryMetrics[];
  employeesByCountry: EmployeeCountByCountry[];
  salaryDistribution: SalaryDistributionByCurrency[];
  payrollByCountry: PayrollByCountry[];
  payrollByCurrency: PayrollByCurrency[];
}
