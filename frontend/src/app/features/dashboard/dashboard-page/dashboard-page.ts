import { Component } from '@angular/core';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { MOCK_EMPLOYEES } from '../../employees/data/mock-employees';
import { PayrollSummary, SalaryBand } from '../../employees/models/employee.model';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [PageHeader],
  template: `
    <app-page-header
      title="HR Payroll Dashboard"
      description="Executive overview of workforce distribution, salary statistics, and global payroll summary."
    />

    <!-- Top Metric Cards -->
    <div class="metrics-grid">
      <div class="card metric-card">
        <div class="metric-header">
          <span class="metric-label">Total Employees</span>
          <div class="metric-icon icon-blue">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>
          </div>
        </div>
        <div class="metric-value">{{ totalEmployeesCount }}</div>
        <div class="metric-subtext">Active global staff</div>
      </div>

      <div class="card metric-card">
        <div class="metric-header">
          <span class="metric-label">Average Salary</span>
          <div class="metric-icon icon-green">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"></line><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path></svg>
          </div>
        </div>
        <div class="metric-value">{{ averageSalaryFormatted }}</div>
        <div class="metric-subtext">USD equivalent base mean</div>
      </div>

      <div class="card metric-card">
        <div class="metric-header">
          <span class="metric-label">Minimum Salary</span>
          <div class="metric-icon icon-amber">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 18 13.5 8.5 8.5 13.5 1 6"></polyline><polyline points="17 18 23 18 23 12"></polyline></svg>
          </div>
        </div>
        <div class="metric-value">{{ minSalaryFormatted }}</div>
        <div class="metric-subtext">Entry-level baseline</div>
      </div>

      <div class="card metric-card">
        <div class="metric-header">
          <span class="metric-label">Maximum Salary</span>
          <div class="metric-icon icon-purple">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"></polyline><polyline points="17 6 23 6 23 12"></polyline></svg>
          </div>
        </div>
        <div class="metric-value">{{ maxSalaryFormatted }}</div>
        <div class="metric-subtext">Executive tier maximum</div>
      </div>
    </div>

    <!-- Breakdown Sections -->
    <div class="sections-grid">
      <!-- Employee Distribution by Country -->
      <div class="card section-card">
        <div class="card-header">
          <h2 class="card-title">Employee Distribution by Country</h2>
          <span class="badge badge-secondary">5 Regions</span>
        </div>
        <div class="distribution-list">
          @for (item of countryDistribution; track item.country) {
            <div class="distribution-item">
              <div class="dist-info">
                <span class="dist-name">{{ item.flag }} {{ item.country }}</span>
                <span class="dist-count">{{ item.count }} employees ({{ item.percentage }}%)</span>
              </div>
              <div class="progress-bar-bg">
                <div class="progress-bar-fill" [style.width.%]="item.percentage"></div>
              </div>
            </div>
          }
        </div>
      </div>

      <!-- Salary Bands Distribution -->
      <div class="card section-card">
        <div class="card-header">
          <h2 class="card-title">Salary Band Breakdown (USD Eq.)</h2>
          <span class="badge badge-secondary">5 Bands</span>
        </div>
        <div class="distribution-list">
          @for (band of salaryBands; track band.label) {
            <div class="distribution-item">
              <div class="dist-info">
                <span class="dist-name">{{ band.label }}</span>
                <span class="dist-count">{{ band.count }} employees ({{ band.percentage }}%)</span>
              </div>
              <div class="progress-bar-bg">
                <div class="progress-bar-fill fill-indigo" [style.width.%]="band.percentage"></div>
              </div>
            </div>
          }
        </div>
      </div>
    </div>

    <!-- Global Payroll Summary Table -->
    <div class="card table-section">
      <div class="card-header">
        <h2 class="card-title">Global Payroll Summary by Country & Currency</h2>
      </div>
      <div class="table-container">
        <table class="data-table" aria-label="Global Payroll Summary">
          <thead>
            <tr>
              <th>Country</th>
              <th>Currency</th>
              <th>Headcount</th>
              <th>Total Payroll</th>
              <th>Average Salary</th>
            </tr>
          </thead>
          <tbody>
            @for (row of payrollSummaries; track row.country) {
              <tr>
                <td><strong>{{ row.country }}</strong></td>
                <td><span class="badge badge-info">{{ row.currency }}</span></td>
                <td>{{ row.employeeCount }}</td>
                <td>{{ formatCurrency(row.totalPayroll, row.currency) }}</td>
                <td>{{ formatCurrency(row.averageSalary, row.currency) }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(1, 1fr);
      gap: var(--space-md);
      margin-bottom: var(--space-xl);
    }
    @media (min-width: 640px) {
      .metrics-grid { grid-template-columns: repeat(2, 1fr); }
    }
    @media (min-width: 1024px) {
      .metrics-grid { grid-template-columns: repeat(4, 1fr); }
    }
    .metric-card {
      padding: var(--space-lg);
      display: flex;
      flex-direction: column;
    }
    .metric-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: var(--space-xs);
    }
    .metric-label {
      font-size: var(--font-size-xs);
      font-weight: 600;
      color: var(--color-text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .metric-icon {
      width: 36px;
      height: 36px;
      border-radius: var(--radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .icon-blue {
      background-color: var(--color-primary-light);
      color: var(--color-primary);
    }
    .icon-green {
      background-color: var(--color-success-bg);
      color: var(--color-success);
    }
    .icon-amber {
      background-color: var(--color-warning-bg);
      color: var(--color-warning);
    }
    .icon-purple {
      background-color: #f3e8ff;
      color: #7e22ce;
    }
    .metric-value {
      font-size: var(--font-size-2xl);
      font-weight: 700;
      color: var(--color-text-main);
      line-height: 1.2;
      margin-bottom: var(--space-2xs);
    }
    .metric-subtext {
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
    }
    .sections-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: var(--space-lg);
      margin-bottom: var(--space-xl);
    }
    @media (min-width: 1024px) {
      .sections-grid { grid-template-columns: repeat(2, 1fr); }
    }
    .section-card {
      display: flex;
      flex-direction: column;
    }
    .distribution-list {
      display: flex;
      flex-direction: column;
      gap: var(--space-md);
    }
    .distribution-item {
      display: flex;
      flex-direction: column;
      gap: var(--space-2xs);
    }
    .dist-info {
      display: flex;
      justify-content: space-between;
      font-size: var(--font-size-sm);
    }
    .dist-name {
      font-weight: 500;
      color: var(--color-text-main);
    }
    .dist-count {
      color: var(--color-text-secondary);
      font-size: var(--font-size-xs);
    }
    .progress-bar-bg {
      width: 100%;
      height: 8px;
      background-color: var(--color-bg);
      border-radius: var(--radius-full);
      overflow: hidden;
    }
    .progress-bar-fill {
      height: 100%;
      background-color: var(--color-primary);
      border-radius: var(--radius-full);
      transition: width 0.3s ease;
    }
    .fill-indigo {
      background-color: #4338ca;
    }
    .table-section {
      margin-bottom: var(--space-xl);
    }
  `]
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
