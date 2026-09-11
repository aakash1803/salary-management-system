import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, EMPTY, catchError, debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { LoadingState } from '../../../shared/components/loading-state/loading-state';
import { Modal } from '../../../shared/components/modal/modal';
import { EmployeeForm } from '../components/employee-form/employee-form';
import { EmployeeRequest, EmployeeResponse, PageResponse } from '../models/employee.model';
import { EmployeeService } from '../services/employee.service';

@Component({
  selector: 'app-employee-list-page',
  standalone: true,
  imports: [RouterLink, PageHeader, EmptyState, LoadingState, Modal, EmployeeForm],
  templateUrl: './employee-list-page.html',
  styleUrl: './employee-list-page.scss'
})
export class EmployeeListPage implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly searchSubject = new Subject<string>();

  // Filter signals
  readonly searchQuery = signal('');
  readonly selectedCountry = signal('');
  readonly selectedDepartment = signal('');

  // Static filter dropdown options
  readonly countries = signal([
    'Canada',
    'France',
    'Germany',
    'India',
    'Japan',
    'United Kingdom',
    'United States'
  ]);

  readonly departments = signal([
    'Engineering',
    'Executive',
    'Finance',
    'Human Resources',
    'Marketing',
    'Operations',
    'Product',
    'Sales',
    'Technology'
  ]);

  // Pagination & List signals
  readonly currentPage = signal(1);
  readonly pageSize = signal(5);
  readonly totalItems = signal(0);
  readonly totalPages = signal(1);
  readonly displayedEmployees = signal<EmployeeResponse[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // Modal signals
  readonly isAddModalOpen = signal(false);
  readonly isSaving = signal(false);
  readonly saveError = signal<string | null>(null);

  readonly rangeText = computed(() => {
    const total = this.totalItems();
    if (total === 0) return 'Showing 0 of 0 employees';
    const start = (this.currentPage() - 1) * this.pageSize() + 1;
    const end = Math.min(this.currentPage() * this.pageSize(), total);
    return `Showing ${start}–${end} of ${total} employees`;
  });

  constructor() {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => {
        this.isLoading.set(true);
        this.errorMessage.set(null);
      }),
      switchMap((query) => {
        const apiPage = Math.max(0, this.currentPage() - 1);
        return this.employeeService.getEmployees({
          search: query.trim() || undefined,
          country: this.selectedCountry() || undefined,
          department: this.selectedDepartment() || undefined,
          page: apiPage,
          size: this.pageSize()
        }).pipe(
          catchError((err) => {
            this.handleEmployeeLoadError(err);
            return EMPTY;
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((response) => {
      this.handleEmployeeResponse(response);
    });
  }

  ngOnInit() {
    this.loadEmployees();
  }

  loadEmployees() {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const apiPage = Math.max(0, this.currentPage() - 1);

    this.employeeService.getEmployees({
      search: this.searchQuery().trim() || undefined,
      country: this.selectedCountry() || undefined,
      department: this.selectedDepartment() || undefined,
      page: apiPage,
      size: this.pageSize()
    }).subscribe({
      next: (response) => this.handleEmployeeResponse(response),
      error: (err) => this.handleEmployeeLoadError(err)
    });
  }

  private handleEmployeeResponse(response: PageResponse<EmployeeResponse>) {
    this.displayedEmployees.set(response.content || []);
    this.totalItems.set(response.totalElements ?? 0);
    this.totalPages.set(response.totalPages || 1);
    this.isLoading.set(false);
  }

  private handleEmployeeLoadError(err: any) {
    this.displayedEmployees.set([]);
    this.totalItems.set(0);
    this.totalPages.set(1);
    this.isLoading.set(false);
    this.errorMessage.set(err?.error?.message || 'Failed to load employee directory.');
  }

  onSearchChange(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
    this.currentPage.set(1);
    this.searchSubject.next(value);
  }

  onCountryChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedCountry.set(value);
    this.currentPage.set(1);
    this.loadEmployees();
  }

  onDepartmentChange(event: Event) {
    const value = (event.target as HTMLSelectElement).value;
    this.selectedDepartment.set(value);
    this.currentPage.set(1);
    this.loadEmployees();
  }

  clearFilters() {
    this.searchQuery.set('');
    this.selectedCountry.set('');
    this.selectedDepartment.set('');
    this.currentPage.set(1);
    this.loadEmployees();
  }

  goToPage(page: number) {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
      this.loadEmployees();
    }
  }

  openAddEmployeeModal() {
    this.saveError.set(null);
    this.isSaving.set(false);
    this.isAddModalOpen.set(true);
  }

  closeAddEmployeeModal() {
    this.isAddModalOpen.set(false);
    this.saveError.set(null);
    this.isSaving.set(false);
  }

  onSaveNewEmployee(request: EmployeeRequest) {
    this.isSaving.set(true);
    this.saveError.set(null);

    this.employeeService.createEmployee(request).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeAddEmployeeModal();
        this.currentPage.set(1);
        this.loadEmployees();
      },
      error: (err) => {
        this.isSaving.set(false);
        const msg = err?.error?.message || 'Failed to create employee. Please check your inputs.';
        this.saveError.set(msg);
      }
    });
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
