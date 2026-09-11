import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, forkJoin, map, of, switchMap } from 'rxjs';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { LoadingState } from '../../../shared/components/loading-state/loading-state';
import { Modal } from '../../../shared/components/modal/modal';
import { EmployeeForm } from '../components/employee-form/employee-form';
import { SalaryForm } from '../components/salary-form/salary-form';
import { EmployeeRequest, EmployeeResponse } from '../models/employee.model';
import { SalaryRequest, SalaryResponse } from '../models/salary.model';
import { EmployeeService } from '../services/employee.service';
import { SalaryService } from '../services/salary.service';

@Component({
  selector: 'app-employee-detail-page',
  standalone: true,
  imports: [RouterLink, PageHeader, EmptyState, LoadingState, Modal, EmployeeForm, SalaryForm],
  templateUrl: './employee-detail-page.html',
  styleUrl: './employee-detail-page.scss'
})
export class EmployeeDetailPage {
  private readonly employeeService = inject(EmployeeService);
  private readonly salaryService = inject(SalaryService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();

  readonly employee = signal<EmployeeResponse | null>(null);
  readonly currentSalary = signal<SalaryResponse | null>(null);
  readonly currentSalaryError = signal<string | null>(null);
  readonly salaryHistory = signal<SalaryResponse[]>([]);
  readonly salaryHistoryError = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isNotFound = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // Modal signals
  readonly isEditModalOpen = signal(false);
  readonly isSalaryModalOpen = signal(false);
  readonly isSaving = signal(false);
  readonly saveError = signal<string | null>(null);
  readonly isSavingSalary = signal(false);
  readonly salaryError = signal<string | null>(null);

  constructor() {
    toObservable(this.id).pipe(
      switchMap(rawId => {
        const numId = Number(rawId);
        if (!rawId || isNaN(numId)) {
          this.employee.set(null);
          this.currentSalary.set(null);
          this.currentSalaryError.set(null);
          this.salaryHistory.set([]);
          this.salaryHistoryError.set(null);
          this.isLoading.set(false);
          this.isNotFound.set(true);
          this.errorMessage.set(null);
          return EMPTY;
        }

        this.isLoading.set(true);
        this.isNotFound.set(false);
        this.errorMessage.set(null);

        return this.employeeService.getEmployeeById(numId).pipe(
          switchMap(emp => {
            return forkJoin({
              emp: of(emp),
              currentSalaryRes: this.salaryService.getCurrentSalary(numId).pipe(
                map(data => ({ data, error: null as string | null })),
                catchError((err: HttpErrorResponse) => {
                  if (err?.status === 404) {
                    return of({ data: null, error: null as string | null });
                  }
                  return of({
                    data: null,
                    error: err?.error?.message || 'Failed to load current salary.'
                  });
                })
              ),
              salaryHistoryRes: this.salaryService.getSalaryHistory(numId).pipe(
                map(data => ({ data, error: null as string | null })),
                catchError((err: HttpErrorResponse) => {
                  return of({
                    data: [] as SalaryResponse[],
                    error: err?.error?.message || 'Failed to load salary history.'
                  });
                })
              )
            });
          }),
          catchError((err: HttpErrorResponse) => {
            this.employee.set(null);
            this.currentSalary.set(null);
            this.currentSalaryError.set(null);
            this.salaryHistory.set([]);
            this.salaryHistoryError.set(null);
            this.isLoading.set(false);
            if (err?.status === 404) {
              this.isNotFound.set(true);
              this.errorMessage.set(null);
            } else {
              this.isNotFound.set(false);
              this.errorMessage.set(err?.error?.message || 'Failed to load employee details.');
            }
            return EMPTY;
          })
        );
      }),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(result => {
      this.employee.set(result.emp);
      this.currentSalary.set(result.currentSalaryRes.data);
      this.currentSalaryError.set(result.currentSalaryRes.error);
      this.salaryHistory.set(result.salaryHistoryRes.data);
      this.salaryHistoryError.set(result.salaryHistoryRes.error);
      this.isLoading.set(false);
      this.isNotFound.set(false);
      this.errorMessage.set(null);
    });
  }

  openEditModal() {
    this.saveError.set(null);
    this.isSaving.set(false);
    this.isEditModalOpen.set(true);
  }

  closeEditModal() {
    this.isEditModalOpen.set(false);
    this.saveError.set(null);
    this.isSaving.set(false);
  }

  onSaveEditEmployee(request: EmployeeRequest) {
    const currentEmp = this.employee();
    if (!currentEmp) return;

    this.isSaving.set(true);
    this.saveError.set(null);

    this.employeeService.updateEmployee(currentEmp.id, request).subscribe({
      next: (updated) => {
        this.isSaving.set(false);
        this.employee.set(updated);
        this.closeEditModal();
      },
      error: (err: HttpErrorResponse) => {
        this.isSaving.set(false);
        const msg = err?.error?.message || 'Failed to update employee. Please check your inputs.';
        this.saveError.set(msg);
      }
    });
  }

  openSalaryModal() {
    this.salaryError.set(null);
    this.isSavingSalary.set(false);
    this.isSalaryModalOpen.set(true);
  }

  closeSalaryModal() {
    this.isSalaryModalOpen.set(false);
    this.salaryError.set(null);
    this.isSavingSalary.set(false);
  }

  onSaveSalaryRecord(request: SalaryRequest) {
    const currentEmp = this.employee();
    if (!currentEmp) return;

    this.isSavingSalary.set(true);
    this.salaryError.set(null);

    this.salaryService.addSalary(currentEmp.id, request).subscribe({
      next: () => {
        this.isSavingSalary.set(false);
        this.closeSalaryModal();
        this.loadSalaryData(currentEmp.id);
      },
      error: (err: HttpErrorResponse) => {
        this.isSavingSalary.set(false);
        const msg = err?.error?.message || 'Failed to add salary record. Please check your inputs.';
        this.salaryError.set(msg);
      }
    });
  }

  private loadSalaryData(numId: number) {
    forkJoin({
      currentSalaryRes: this.salaryService.getCurrentSalary(numId).pipe(
        map(data => ({ data, error: null as string | null })),
        catchError((err: HttpErrorResponse) => {
          if (err?.status === 404) {
            return of({ data: null, error: null as string | null });
          }
          return of({
            data: null,
            error: err?.error?.message || 'Failed to load current salary.'
          });
        })
      ),
      salaryHistoryRes: this.salaryService.getSalaryHistory(numId).pipe(
        map(data => ({ data, error: null as string | null })),
        catchError((err: HttpErrorResponse) => {
          return of({
            data: [] as SalaryResponse[],
            error: err?.error?.message || 'Failed to load salary history.'
          });
        })
      )
    }).subscribe({
      next: res => {
        this.currentSalary.set(res.currentSalaryRes.data);
        this.currentSalaryError.set(res.currentSalaryRes.error);
        this.salaryHistory.set(res.salaryHistoryRes.data);
        this.salaryHistoryError.set(res.salaryHistoryRes.error);
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

  formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    const d = new Date(dateStr);
    return isNaN(d.getTime()) ? dateStr : d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
  }

  isFutureDate(dateStr: string): boolean {
    if (!dateStr) return false;
    const today = new Date().toISOString().split('T')[0];
    return dateStr > today;
  }
}
