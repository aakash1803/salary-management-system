import { Component } from '@angular/core';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { MOCK_EMPLOYEES } from '../../employees/data/mock-employees';
import { PayrollSummary, SalaryBand } from '../../employees/models/employee.model';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [PageHeader],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss'
})
export class DashboardPage {
  readonly employees = MOCK_EMPLOYEES;

  readonly totalEmployeesCount = 148;
  readonly averageSalaryFormatted = '$84,250';
  readonly minSalaryFormatted = '$30,000';
  readonly maxSalaryFormatted = '$195,000';

  readonly countryDistribution = [
    { country: 'United States', count: 52, percentage: 35, flag: '🇺🇸' },
    { country: 'Germany', count: 30, percentage: 20, flag: '🇩🇪' },
    { country: 'United Kingdom', count: 26, percentage: 18, flag: '🇬🇧' },
    { country: 'India', count: 22, percentage: 15, flag: '🇮🇳' },
    { country: 'Japan', count: 18, percentage: 12, flag: '🇯🇵' },
  ];

  readonly salaryBands: SalaryBand[] = [
    { label: 'Under 30,000', min: 0, max: 29999.99, count: 12, percentage: 8 },
    { label: '30,000 - 59,999.99', min: 30000, max: 59999.99, count: 34, percentage: 23 },
    { label: '60,000 - 99,999.99', min: 60000, max: 99999.99, count: 58, percentage: 39 },
    { label: '100,000 - 149,999.99', min: 100000, max: 149999.99, count: 32, percentage: 22 },
    { label: '150,000 and above', min: 150000, max: Infinity, count: 12, percentage: 8 },
  ];

  readonly payrollSummaries: PayrollSummary[] = [
    { country: 'United States', currency: 'USD', employeeCount: 52, totalPayroll: 6188000, averageSalary: 119000 },
    { country: 'Germany', currency: 'EUR', employeeCount: 30, totalPayroll: 2430000, averageSalary: 81000 },
    { country: 'United Kingdom', currency: 'GBP', employeeCount: 26, totalPayroll: 1976000, averageSalary: 76000 },
    { country: 'Canada', currency: 'CAD', employeeCount: 18, totalPayroll: 1530000, averageSalary: 85000 },
    { country: 'India', currency: 'INR', employeeCount: 22, totalPayroll: 55000000, averageSalary: 2500000 },
    { country: 'Japan', currency: 'JPY', employeeCount: 18, totalPayroll: 153000000, averageSalary: 8500000 },
  ];

  formatCurrency(amount: number, currency: string): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      maximumFractionDigits: 0
    }).format(amount);
  }
}
