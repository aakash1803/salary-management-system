import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { PageHeader } from '../../../shared/components/page-header/page-header';
import { DashboardService } from '../services/dashboard.service';
import {
  DashboardResponse,
  SalaryMetrics,
  SalaryDistributionByCurrency
} from '../models/dashboard.model';

const COUNTRY_FLAGS: Record<string, string> = {
  'Canada': '🇨🇦',
  'France': '🇫🇷',
  'Germany': '🇩🇪',
  'India': '🇮🇳',
  'Japan': '🇯🇵',
  'United Kingdom': '🇬🇧',
  'United States': '🇺🇸',
};

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [PageHeader],
  templateUrl: './dashboard-page.html',
  styleUrl: './dashboard-page.scss'
})
export class DashboardPage implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  readonly loading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly dashboard = signal<DashboardResponse | null>(null);
  readonly selectedCurrency = signal<string>('USD');

  readonly availableCurrencies = computed(() => {
    const data = this.dashboard();
    if (!data) return [];
    const currencies = new Set<string>();
    data.salaryMetricsByCurrency?.forEach(m => currencies.add(m.currency));
    data.salaryDistribution?.forEach(d => currencies.add(d.currency));
    return Array.from(currencies);
  });

  readonly currentMetrics = computed<SalaryMetrics | null>(() => {
    const data = this.dashboard();
    if (!data || !data.salaryMetricsByCurrency?.length) return null;
    const curr = this.selectedCurrency();
    return data.salaryMetricsByCurrency.find(m => m.currency === curr) ?? null;
  });

  readonly currentDistribution = computed<SalaryDistributionByCurrency | null>(() => {
    const data = this.dashboard();
    if (!data || !data.salaryDistribution?.length) return null;
    const curr = this.selectedCurrency();
    return data.salaryDistribution.find(d => d.currency === curr) ?? null;
  });

  readonly currentDistributionTotal = computed<number>(() => {
    const dist = this.currentDistribution();
    if (!dist) return 0;
    return dist.bands.reduce((sum, b) => sum + b.employeeCount, 0);
  });

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);

    this.dashboardService.getDashboard().subscribe({
      next: (data) => {
        this.dashboard.set(data);
        this.loading.set(false);
        if (data.salaryMetricsByCurrency?.length > 0) {
          const firstCurr = data.salaryMetricsByCurrency[0].currency;
          if (!data.salaryMetricsByCurrency.some(m => m.currency === this.selectedCurrency())) {
            this.selectedCurrency.set(firstCurr);
          }
        }
      },
      error: (_err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set('Failed to load dashboard data. Please try again.');
      }
    });
  }

  onCurrencyChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    if (select && select.value) {
      this.selectedCurrency.set(select.value);
    }
  }

  getCountryFlag(country: string): string {
    return COUNTRY_FLAGS[country] || '🌐';
  }

  calculateCountryPercentage(count: number): number {
    const total = this.dashboard()?.totalEmployees || 0;
    if (total === 0) return 0;
    return Math.round((count / total) * 100);
  }

  calculateBandPercentage(count: number): number {
    const total = this.currentDistributionTotal();
    if (total === 0) return 0;
    return Math.round((count / total) * 100);
  }

  formatCurrency(amount: number | null | undefined, currency?: string): string {
    if (amount == null) return 'N/A';
    const curr = currency || this.selectedCurrency() || 'USD';
    try {
      return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: curr,
        maximumFractionDigits: 0
      }).format(amount);
    } catch {
      return `${curr} ${amount.toLocaleString()}`;
    }
  }
}
