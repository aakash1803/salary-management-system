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
  templateUrl: './employee-detail-page.html',
  styleUrl: './employee-detail-page.scss'
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

  readonly activeSalaryRecordId = computed<string | undefined>(() => {
    const history = this.employee().salaryHistory;
    if (!history || history.length === 0) return undefined;

    const todayStr = new Date().toISOString().split('T')[0];
    const sorted = [...history].sort((a, b) => {
      if (b.effectiveFrom === a.effectiveFrom) {
        return b.id.localeCompare(a.id);
      }
      return b.effectiveFrom.localeCompare(a.effectiveFrom);
    });

    const activeRecord = sorted.find(r => r.effectiveFrom <= todayStr);
    return activeRecord?.id;
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

  formatSalary(amount: number | undefined, currency: string | undefined): string {
    if (amount === undefined || amount === null || !currency) {
      return '—';
    }
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      maximumFractionDigits: 0
    }).format(amount);
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}
