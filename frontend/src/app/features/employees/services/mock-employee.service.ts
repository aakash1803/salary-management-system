import { Injectable, signal } from '@angular/core';
import { MOCK_EMPLOYEES } from '../data/mock-employees';
import { Employee, SalaryRecord } from '../models/employee.model';

let salaryRecordSeq = 1000;

@Injectable({
  providedIn: 'root'
})
export class MockEmployeeService {
  private readonly employeesSignal = signal<Employee[]>(MOCK_EMPLOYEES);

  readonly employees = this.employeesSignal.asReadonly();

  getEmployees(): Employee[] {
    return this.employeesSignal();
  }

  getEmployeeById(id: number | string): Employee | undefined {
    return this.employeesSignal().find(e => String(e.id) === String(id) || e.employeeNumber === id);
  }

  addEmployee(data: Partial<Employee>): Employee {
    const newEmployee: Employee = {
      id: Date.now(),
      employeeNumber: data.employeeNumber || `EMP-${1000 + this.employeesSignal().length + 1}`,
      firstName: data.firstName || '',
      lastName: data.lastName || '',
      email: data.email || '',
      country: data.country || 'United States',
      department: data.department || 'Engineering',
      currentSalary: undefined,
      currency: undefined,
      effectiveFrom: undefined,
      salaryHistory: []
    };

    this.employeesSignal.update(list => [newEmployee, ...list]);
    return newEmployee;
  }

  updateEmployee(id: number | string, data: Partial<Employee>): Employee | undefined {
    let updatedEmp: Employee | undefined;

    this.employeesSignal.update(list =>
      list.map(emp => {
        if (String(emp.id) === String(id) || emp.employeeNumber === id) {
          updatedEmp = {
            ...emp,
            ...data,
            id: emp.id,
            employeeNumber: data.employeeNumber || emp.employeeNumber,
            salaryHistory: emp.salaryHistory // Preserve existing salary history
          };
          return updatedEmp;
        }
        return emp;
      })
    );

    return updatedEmp;
  }

  addSalaryRecord(
    employeeId: number | string,
    recordData: { amount: number; currency: string; effectiveFrom: string }
  ): Employee | undefined {
    let updatedEmp: Employee | undefined;
    const todayStr = new Date().toISOString().split('T')[0];

    this.employeesSignal.update(list =>
      list.map(emp => {
        if (String(emp.id) === String(employeeId) || emp.employeeNumber === employeeId) {
          const newRecord: SalaryRecord = {
            id: 'sh-' + Date.now() + '-' + (++salaryRecordSeq),
            effectiveFrom: recordData.effectiveFrom,
            amount: Number(recordData.amount),
            currency: recordData.currency,
            createdAt: new Date().toISOString()
          };

          // Combine and sort salary history:
          // Primary: effectiveFrom descending
          // Secondary: record ID descending (deterministic tie-breaking)
          const updatedHistory = [newRecord, ...emp.salaryHistory].sort((a, b) => {
            if (b.effectiveFrom === a.effectiveFrom) {
              return b.id.localeCompare(a.id);
            }
            return b.effectiveFrom.localeCompare(a.effectiveFrom);
          });

          // Determine current active salary: record with latest effectiveFrom <= today
          const activeRecord = updatedHistory.find(r => r.effectiveFrom <= todayStr);

          updatedEmp = {
            ...emp,
            currentSalary: activeRecord ? activeRecord.amount : emp.currentSalary,
            currency: activeRecord ? activeRecord.currency : emp.currency,
            effectiveFrom: activeRecord ? activeRecord.effectiveFrom : emp.effectiveFrom,
            salaryHistory: updatedHistory
          };

          return updatedEmp;
        }
        return emp;
      })
    );

    return updatedEmp;
  }
}
