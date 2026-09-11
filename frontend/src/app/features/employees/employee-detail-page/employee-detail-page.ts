import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { EMPTY, catchError, switchMap } from 'rxjs';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { EmptyState } from '../../../shared/components/empty-state/empty-state';
import { LoadingState } from '../../../shared/components/loading-state/loading-state';
import { Modal } from '../../../shared/components/modal/modal';
import { EmployeeForm } from '../components/employee-form/employee-form';
import { SalaryForm } from '../components/salary-form/salary-form';
import { EmployeeRequest, EmployeeResponse } from '../models/employee.model';
import { EmployeeService } from '../services/employee.service';

@Component({
  selector: 'app-employee-detail-page',
  standalone: true,
  imports: [RouterLink, PageHeader, EmptyState, LoadingState, Modal, EmployeeForm, SalaryForm],
  templateUrl: './employee-detail-page.html',
  styleUrl: './employee-detail-page.scss'
})
export class EmployeeDetailPage {
  private readonly employeeService = inject(EmployeeService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input<string>();

  readonly employee = signal<EmployeeResponse | null>(null);
  readonly isLoading = signal(true);
  readonly isNotFound = signal(false);
  readonly errorMessage = signal<string | null>(null);

  // Modal signals
  readonly isEditModalOpen = signal(false);
  readonly isSalaryModalOpen = signal(false);
  readonly isSaving = signal(false);
  readonly saveError = signal<string | null>(null);

  constructor() {
    toObservable(this.id).pipe(
      switchMap(rawId => {
        const numId = Number(rawId);
        if (!rawId || isNaN(numId)) {
          this.employee.set(null);
          this.isLoading.set(false);
          this.isNotFound.set(true);
          this.errorMessage.set(null);
          return EMPTY;
        }

        this.isLoading.set(true);
        this.isNotFound.set(false);
        this.errorMessage.set(null);

        return this.employeeService.getEmployeeById(numId).pipe(
          catchError(err => {
            this.employee.set(null);
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
    ).subscribe(emp => {
      this.employee.set(emp);
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
      error: (err) => {
        this.isSaving.set(false);
        const msg = err?.error?.message || 'Failed to update employee. Please check your inputs.';
        this.saveError.set(msg);
      }
    });
  }

  openSalaryModal() {
    this.isSalaryModalOpen.set(true);
  }

  closeSalaryModal() {
    this.isSalaryModalOpen.set(false);
  }

  onSaveSalaryRecord(data: any) {
    this.closeSalaryModal();
  }
}
