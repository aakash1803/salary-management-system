import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { LoadingState } from '../../../shared/components/loading-state/loading-state';
import { Modal } from '../../../shared/components/modal/modal';
import { EmployeeForm } from '../components/employee-form/employee-form';
import { Employee } from '../models/employee.model';
import { MockEmployeeService } from '../services/mock-employee.service';

@Component({
  selector: 'app-employee-list-page',
  standalone: true,
  imports: [RouterLink, PageHeader, EmptyState, LoadingState, Modal, EmployeeForm],
  templateUrl: './employee-list-page.html',
  styleUrl: './employee-list-page.scss'
})
export class EmployeeListPage {
  private readonly mockEmployeeService = inject(MockEmployeeService);

  readonly allEmployees = this.mockEmployeeService.employees;

  // Filter signals
  readonly searchQuery = signal('');
  readonly selectedCountry = signal('');
  readonly selectedDepartment = signal('');

  // Pagination signals
  readonly currentPage = signal(1);
  readonly pageSize = signal(5);
  readonly isLoading = signal(false);

  // Modal signal
  readonly isAddModalOpen = signal(false);

  // Dynamic filter dropdown options derived from state
  readonly countries = computed(() =>
    Array.from(new Set(this.allEmployees().map(e => e.country))).sort()
  );

  readonly departments = computed(() =>
    Array.from(new Set(this.allEmployees().map(e => e.department))).sort()
  );

  // Derived filtered employees list
  readonly filteredEmployees = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const country = this.selectedCountry();
    const dept = this.selectedDepartment();

    return this.allEmployees().filter(emp => {
      const matchesQuery = !query ||
        emp.firstName.toLowerCase().includes(query) ||
        emp.lastName.toLowerCase().includes(query) ||
        emp.email.toLowerCase().includes(query) ||
        emp.employeeNumber.toLowerCase().includes(query);

      const matchesCountry = !country || emp.country === country;
      const matchesDept = !dept || emp.department === dept;

      return matchesQuery && matchesCountry && matchesDept;
    });
  });

  // Derived Pagination computations
  readonly totalItems = computed(() => this.filteredEmployees().length);

  readonly totalPages = computed(() => {
    return Math.ceil(this.totalItems() / this.pageSize()) || 1;
  });

  readonly displayedEmployees = computed(() => {
    const page = this.currentPage();
    const size = this.pageSize();
    const start = (page - 1) * size;
    return this.filteredEmployees().slice(start, start + size);
  });

  readonly rangeText = computed(() => {
    const total = this.totalItems();
    if (total === 0) return 'Showing 0 of 0 employees';
    const start = (this.currentPage() - 1) * this.pageSize() + 1;
    const end = Math.min(this.currentPage() * this.pageSize(), total);
    return `Showing ${start}–${end} of ${total} employees`;
  });

  onSearchChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    this.currentPage.set(1);
  }

  onCountryChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedCountry.set(value);
    this.currentPage.set(1);
  }

  onDepartmentChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedDepartment.set(value);
    this.currentPage.set(1);
  }

  clearFilters() {
    this.searchQuery.set('');
    this.selectedCountry.set('');
    this.selectedDepartment.set('');
    this.currentPage.set(1);
  }

  goToPage(page: number) {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  openAddEmployeeModal() {
    this.isAddModalOpen.set(true);
  }

  closeAddEmployeeModal() {
    this.isAddModalOpen.set(false);
  }

  onSaveNewEmployee(data: Partial<Employee>) {
    this.mockEmployeeService.addEmployee(data);
    this.closeAddEmployeeModal();
    this.currentPage.set(1);
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
}
