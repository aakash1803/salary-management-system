import { TestBed } from '@angular/core/testing';
import { DashboardPage } from './dashboard-page';

describe('DashboardPage Component', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage]
    }).compileComponents();
  });

  it('should render core HR metric cards and distribution tables', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelectorAll('.metric-card').length).toBe(4);
    expect(compiled.textContent).toContain('Total Employees');
    expect(compiled.textContent).toContain('Average Salary');
    expect(compiled.textContent).toContain('Employee Distribution by Country');
    expect(compiled.textContent).toContain('Salary Band Breakdown');
  });

  it('should format currency amounts cleanly', () => {
    const fixture = TestBed.createComponent(DashboardPage);
    const component = fixture.componentInstance;
    expect(component.formatCurrency(125000, 'USD')).toContain('125,000');
  });
});
