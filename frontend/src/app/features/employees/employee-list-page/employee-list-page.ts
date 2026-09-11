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
  template: `
    <app-page-header
      title="Employee Directory"
      description="Manage employee salary profiles, department assignments, and compensation history."
    >
      <button type="button" class="btn btn-primary" (click)="openAddEmployeeModal()" title="Add Employee">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"></line><line x1="5" y1="12" x2="19" y2="12"></line></svg>
        Add Employee
      </button>
    </app-page-header>

    <!-- Modal Dialog for Add Employee -->
    @if (isAddModalOpen()) {
      <app-modal title="Add Employee" (closeModal)="closeAddEmployeeModal()">
        <app-employee-form
          mode="CREATE"
          (saveForm)="onSaveNewEmployee($event)"
          (cancelForm)="closeAddEmployeeModal()"
        />
      </app-modal>
    }

    <!-- Filter Toolbar Card -->
    <div class="card filter-card">
      <div class="filters-grid">
        <!-- Search Field -->
        <div class="filter-item search-field">
          <label for="empSearch" class="form-label">Search</label>
          <div class="input-with-icon">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon"><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <input
              id="empSearch"
              type="text"
              class="form-input"
              placeholder="Search by name, email or EMP ID..."
              [value]="searchQuery()"
              (input)="onSearchChange($event)"
            />
          </div>
        </div>

        <!-- Country Filter -->
        <div class="filter-item">
          <label for="countryFilter" class="form-label">Country</label>
          <select
            id="countryFilter"
            class="form-select"
            [value]="selectedCountry()"
            (change)="onCountryChange($event)"
          >
            <option value="">All Countries</option>
            @for (c of countries(); track c) {
              <option [value]="c">{{ c }}</option>
            }
          </select>
        </div>

        <!-- Department Filter -->
        <div class="filter-item">
          <label for="deptFilter" class="form-label">Department</label>
          <select
            id="deptFilter"
            class="form-select"
            [value]="selectedDepartment()"
            (change)="onDepartmentChange($event)"
          >
            <option value="">All Departments</option>
            @for (d of departments(); track d) {
              <option [value]="d">{{ d }}</option>
            }
          </select>
        </div>

        <!-- Clear Action -->
        @if (searchQuery() || selectedCountry() || selectedDepartment()) {
          <div class="filter-item clear-action">
            <button type="button" class="btn btn-subtle" (click)="clearFilters()">
              Clear Filters
            </button>
          </div>
        }
      </div>
    </div>

    <!-- Main Table or States -->
    @if (isLoading()) {
      <app-loading-state message="Loading employee directory..." />
    } @else if (totalItems() === 0) {
      <app-empty-state
        title="No employees match your filters"
        description="Try clearing your search terms or selecting a different country/department."
      >
        <button type="button" class="btn btn-secondary" (click)="clearFilters()">Reset All Filters</button>
      </app-empty-state>
    } @else {
      <div class="table-container">
        <table class="data-table" aria-label="Employees Table">
          <thead>
            <tr>
              <th>Employee #</th>
              <th>Name</th>
              <th>Email</th>
              <th>Country</th>
              <th>Department</th>
              <th>Current Salary</th>
              <th>Currency</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            @for (emp of displayedEmployees(); track emp.id) {
              <tr>
                <td>
                  <span class="emp-number">{{ emp.employeeNumber }}</span>
                </td>
                <td>
                  <strong class="emp-name">{{ emp.firstName }} {{ emp.lastName }}</strong>
                </td>
                <td class="text-secondary">{{ emp.email }}</td>
                <td>{{ emp.country }}</td>
                <td>
                  <span class="badge badge-secondary">{{ emp.department }}</span>
                </td>
                <td class="font-medium">
                  {{ formatSalary(emp.currentSalary, emp.currency) }}
                </td>
                <td>
                  @if (emp.currency) {
                    <span class="badge badge-info">{{ emp.currency }}</span>
                  } @else {
                    <span class="text-secondary">—</span>
                  }
                </td>
                <td class="text-right">
                  <a [routerLink]="['/employees', emp.id]" class="btn btn-secondary btn-sm" title="View details">
                    View
                  </a>
                </td>
              </tr>
            }
          </tbody>
        </table>

        <!-- Pagination Controls -->
        <div class="pagination-bar">
          <div class="pagination-info">
            {{ rangeText() }}
          </div>

          <div class="pagination-controls">
            <button
              type="button"
              class="btn btn-secondary btn-sm"
              [disabled]="currentPage() <= 1"
              (click)="goToPage(currentPage() - 1)"
            >
              Previous
            </button>

            <span class="page-indicator">
              Page <strong>{{ currentPage() }}</strong> of <strong>{{ totalPages() }}</strong>
            </span>

            <button
              type="button"
              class="btn btn-secondary btn-sm"
              [disabled]="currentPage() >= totalPages()"
              (click)="goToPage(currentPage() + 1)"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .filter-card {
      margin-bottom: var(--space-lg);
      padding: var(--space-md) var(--space-lg);
    }
    .filters-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: var(--space-md);
      align-items: flex-end;
    }
    @media (min-width: 640px) {
      .filters-grid {
        grid-template-columns: 2fr 1fr 1fr auto;
      }
    }
    .filter-item {
      display: flex;
      flex-direction: column;
    }
    .search-field {
      position: relative;
    }
    .input-with-icon {
      position: relative;
      display: flex;
      align-items: center;
    }
    .search-icon {
      position: absolute;
      left: 10px;
      color: var(--color-text-muted);
      pointer-events: none;
    }
    .input-with-icon .form-input {
      padding-left: 34px;
    }
    .clear-action {
      justify-content: flex-end;
      margin-bottom: 2px;
    }
    .text-right {
      text-align: right;
    }
    .text-secondary {
      color: var(--color-text-secondary);
    }
    .font-medium {
      font-weight: 600;
    }
    .emp-number {
      font-family: monospace;
      font-size: var(--font-size-xs);
      color: var(--color-text-secondary);
      background-color: var(--color-bg);
      padding: 2px 6px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--color-border);
    }
    .emp-name {
      color: var(--color-text-main);
    }
    .btn-sm {
      padding: 4px var(--space-sm);
      font-size: var(--font-size-xs);
    }
    .pagination-bar {
      display: flex;
      flex-direction: column;
      gap: var(--space-md);
      padding: var(--space-md);
      background-color: var(--color-bg);
      border-top: 1px solid var(--color-border);
    }
    @media (min-width: 640px) {
      .pagination-bar {
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
      }
    }
    .pagination-info {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
    }
    .pagination-controls {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
    }
    .page-indicator {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
    }
  `]
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
