import { TestBed } from '@angular/core/testing';
import { MockEmployeeService } from './mock-employee.service';

describe('MockEmployeeService Domain Rules', () => {
  let service: MockEmployeeService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MockEmployeeService);
  });

  it('should create new employee with empty salaryHistory and undefined currentSalary', () => {
    const newEmp = service.addEmployee({
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice.smith@company.com',
      country: 'United States',
      department: 'Engineering'
    });

    expect(newEmp.salaryHistory).toEqual([]);
    expect(newEmp.currentSalary).toBeUndefined();
    expect(newEmp.currency).toBeUndefined();
    expect(newEmp.effectiveFrom).toBeUndefined();
  });

  it('should update current salary when adding a current/effective-today salary record', () => {
    const newEmp = service.addEmployee({
      firstName: 'Bob',
      lastName: 'Jones',
      email: 'bob.jones@company.com'
    });

    const today = new Date().toISOString().split('T')[0];
    service.addSalaryRecord(newEmp.id, {
      amount: 90000,
      currency: 'USD',
      effectiveFrom: today
    });

    const updated = service.getEmployeeById(newEmp.id);
    expect(updated?.currentSalary).toBe(90000);
    expect(updated?.currency).toBe('USD');
    expect(updated?.effectiveFrom).toBe(today);
    expect(updated?.salaryHistory.length).toBe(1);
  });

  it('should not set current salary when adding a future-dated salary record to employee with no current salary', () => {
    const newEmp = service.addEmployee({
      firstName: 'Charlie',
      lastName: 'Brown'
    });

    const futureDate = '2099-01-01';
    service.addSalaryRecord(newEmp.id, {
      amount: 120000,
      currency: 'USD',
      effectiveFrom: futureDate
    });

    const updated = service.getEmployeeById(newEmp.id);
    expect(updated?.currentSalary).toBeUndefined();
    expect(updated?.salaryHistory.length).toBe(1);
    expect(updated?.salaryHistory[0].amount).toBe(120000);
  });

  it('should preserve existing current salary when adding a future-dated salary record', () => {
    const emp = service.getEmployees()[0]; // Sarah Jenkins (has current salary)
    const originalSalary = emp.currentSalary;
    const futureDate = '2099-01-01';

    service.addSalaryRecord(emp.id, {
      amount: 200000,
      currency: 'USD',
      effectiveFrom: futureDate
    });

    const updated = service.getEmployeeById(emp.id);
    expect(updated?.currentSalary).toBe(originalSalary);
    expect(updated?.salaryHistory[0].amount).toBe(200000);
  });

  it('should preserve salary history when editing employee metadata', () => {
    const emp = service.getEmployees()[0];
    const historyCount = emp.salaryHistory.length;

    const updated = service.updateEmployee(emp.id, {
      firstName: 'Sarah-Edited',
      department: 'Marketing'
    });

    expect(updated?.firstName).toBe('Sarah-Edited');
    expect(updated?.department).toBe('Marketing');
    expect(updated?.salaryHistory.length).toBe(historyCount);
  });

  it('should order same-effectiveDate salary records deterministically by ID descending', () => {
    const emp = service.getEmployees()[0];
    const dateStr = '2024-01-01';

    service.addSalaryRecord(emp.id, { amount: 100000, currency: 'USD', effectiveFrom: dateStr });
    service.addSalaryRecord(emp.id, { amount: 110000, currency: 'USD', effectiveFrom: dateStr });

    const updated = service.getEmployeeById(emp.id);
    const sameDateRecords = updated?.salaryHistory.filter(r => r.effectiveFrom === dateStr);

    expect(sameDateRecords && sameDateRecords.length >= 2).toBe(true);
    if (sameDateRecords && sameDateRecords.length >= 2) {
      expect(sameDateRecords[0].id.localeCompare(sameDateRecords[1].id)).toBeGreaterThanOrEqual(0);
    }
  });
});
