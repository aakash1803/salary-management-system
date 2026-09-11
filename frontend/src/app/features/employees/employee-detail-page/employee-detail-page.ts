import { Component, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { Modal } from '../../../shared/components/modal/modal';
import { EmployeeForm } from '../components/employee-form/employee-form';
import { SalaryForm } from '../components/salary-form/salary-form';
import { Employee } from '../models/employee.model';
import { MockEmployeeService } from '../services/mock-employee.service';

@Component({
  selector: 'app-employee-detail-page',
  standalone: true,
  imports: [RouterLink, PageHeader, Modal, EmployeeForm, SalaryForm],
  template: `
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <a routerLink="/employees" class="back-link">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="19" y1="12" x2="5" y2="12"></line><polyline points="12 19 5 12 12 5"></polyline></svg>
        Back to Employees
      </a>
    </div>

    <app-page-header
      [title]="employee().firstName + ' ' + employee().lastName"
      [description]="'Employee Number: ' + employee().employeeNumber + ' • ' + employee().department"
    >
      <div class="action-buttons">
        <button type="button" class="btn btn-secondary" (click)="openEditModal()" title="Edit Employee">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
          Edit Employee
        </button>
        <button type="button" class="btn btn-primary" (click)="openSalaryModal()" title="Add Salary Record">
          <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
          Add Salary Record
        </button>
      </div>
    </app-page-header>

    <!-- Modal for Edit Employee -->
    @if (isEditModalOpen()) {
      <app-modal title="Edit Employee" (closeModal)="closeEditModal()">
        <app-employee-form
          mode="EDIT"
          [employee]="employee()"
          (saveForm)="onSaveEditEmployee($event)"
          (cancelForm)="closeEditModal()"
        />
      </app-modal>
    }

    <!-- Modal for Add Salary Record -->
    @if (isSalaryModalOpen()) {
      <app-modal title="Add Salary Record" (closeModal)="closeSalaryModal()">
        <app-salary-form
          [defaultCurrency]="employee().currency"
          (saveForm)="onSaveSalaryRecord($event)"
          (cancelForm)="closeSalaryModal()"
        />
      </app-modal>
    }

    <!-- Overview Cards Grid -->
    <div class="overview-grid">
      <!-- Employee Info Card -->
      <div class="card info-card">
        <div class="card-header">
          <h2 class="card-title">Employee Details</h2>
          <span class="badge badge-secondary">{{ employee().country }}</span>
        </div>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">Employee Number</span>
            <span class="info-value emp-code">{{ employee().employeeNumber }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Full Name</span>
            <span class="info-value">{{ employee().firstName }} {{ employee().lastName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Email Address</span>
            <span class="info-value text-break">{{ employee().email }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Country</span>
            <span class="info-value">{{ employee().country }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">Department</span>
            <span class="info-value">
              <span class="badge badge-secondary">{{ employee().department }}</span>
            </span>
          </div>
        </div>
      </div>

      <!-- Current Salary Highlight Card -->
      <div class="card salary-highlight-card">
        <div class="card-header">
          <h2 class="card-title">Current Compensation</h2>
          <span class="badge badge-success">Active Rate</span>
        </div>
        <div class="salary-display">
          <div class="salary-amount">
            {{ formatSalary(employee().currentSalary, employee().currency) }}
          </div>
          <div class="salary-meta">
            <span class="badge badge-info">{{ employee().currency }}</span>
            <span class="meta-label">Effective from {{ formatDate(employee().effectiveFrom) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Salary History Section -->
    <div class="card history-section">
      <div class="card-header">
        <div>
          <h2 class="card-title">Salary Change History</h2>
          <p class="section-subtitle">Chronological audit log of salary adjustments over time</p>
        </div>
      </div>

      <div class="table-container">
        <table class="data-table" aria-label="Salary History Table">
          <thead>
            <tr>
              <th>Status</th>
              <th>Effective From</th>
              <th>Amount</th>
              <th>Currency</th>
              <th>Recorded Date</th>
            </tr>
          </thead>
          <tbody>
            @for (record of employee().salaryHistory; track record.id; let idx = $index) {
              <tr [class.current-record-row]="record.amount === employee().currentSalary && record.effectiveFrom === employee().effectiveFrom">
                <td>
                  @if (record.amount === employee().currentSalary && record.effectiveFrom === employee().effectiveFrom) {
                    <span class="badge badge-success">Current</span>
                  } @else {
                    <span class="badge badge-secondary">Historical</span>
                  }
                </td>
                <td>
                  <strong>{{ formatDate(record.effectiveFrom) }}</strong>
                </td>
                <td class="font-bold">
                  {{ formatSalary(record.amount, record.currency) }}
                </td>
                <td>
                  <span class="badge badge-info">{{ record.currency }}</span>
                </td>
                <td class="text-muted">
                  {{ formatDate(record.createdAt) }}
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .breadcrumb {
      margin-bottom: var(--space-md);
    }
    .back-link {
      display: inline-flex;
      align-items: center;
      gap: var(--space-xs);
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      font-weight: 500;

      &:hover {
        color: var(--color-primary);
        text-decoration: none;
      }
    }
    .action-buttons {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
    }
    .overview-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: var(--space-lg);
      margin-bottom: var(--space-xl);
    }
    @media (min-width: 1024px) {
      .overview-grid {
        grid-template-columns: 3fr 2fr;
      }
    }
    .info-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: var(--space-md);
    }
    @media (min-width: 640px) {
      .info-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    .info-item {
      display: flex;
      flex-direction: column;
      gap: var(--space-2xs);
    }
    .info-label {
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .info-value {
      font-size: var(--font-size-sm);
      font-weight: 600;
      color: var(--color-text-main);
    }
    .emp-code {
      font-family: monospace;
      font-size: var(--font-size-xs);
      background-color: var(--color-bg);
      padding: 2px 6px;
      border-radius: var(--radius-sm);
      width: fit-content;
      border: 1px solid var(--color-border);
    }
    .text-break {
      word-break: break-all;
    }
    .salary-highlight-card {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      background-color: #f8fafc;
    }
    .salary-display {
      display: flex;
      flex-direction: column;
      gap: var(--space-sm);
      margin-top: var(--space-md);
    }
    .salary-amount {
      font-size: 2.25rem;
      font-weight: 800;
      color: var(--color-primary);
      letter-spacing: -0.02em;
      line-height: 1.1;
    }
    .salary-meta {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
    }
    .meta-label {
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
    }
    .section-subtitle {
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
      margin-top: 2px;
    }
    .current-record-row {
      background-color: #f0fdf4;
    }
    .current-record-row:hover {
      background-color: #dcfce7 !important;
    }
    .font-bold {
      font-weight: 700;
    }
    .text-muted {
      color: var(--color-text-muted);
    }
  `]
})
export class EmployeeDetailPage {
  private readonly mockEmployeeService = inject(MockEmployeeService);

  readonly id = input<string>();

  readonly isEditModalOpen = signal(false);
  readonly isSalaryModalOpen = signal(false);

  readonly employee = computed<Employee>(() => {
    const targetId = this.id();
    if (!targetId) return this.mockEmployeeService.employees()[0];
    const found = this.mockEmployeeService.getEmployeeById(targetId);
    return found || this.mockEmployeeService.employees()[0];
  });

  openEditModal() {
    this.isEditModalOpen.set(true);
  }

  closeEditModal() {
    this.isEditModalOpen.set(false);
  }

  onSaveEditEmployee(data: Partial<Employee>) {
    this.mockEmployeeService.updateEmployee(this.employee().id, data);
    this.closeEditModal();
  }

  openSalaryModal() {
    this.isSalaryModalOpen.set(true);
  }

  closeSalaryModal() {
    this.isSalaryModalOpen.set(false);
  }

  onSaveSalaryRecord(data: { amount: number; currency: string; effectiveFrom: string }) {
    this.mockEmployeeService.addSalaryRecord(this.employee().id, data);
    this.closeSalaryModal();
  }

  formatSalary(amount: number, currency: string): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      maximumFractionDigits: 0
    }).format(amount);
  }

  formatDate(dateString: string): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
