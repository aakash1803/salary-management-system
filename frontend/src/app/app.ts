import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly router = inject(Router);
  readonly currentUrl = signal(this.router.url);
  readonly isMobileSidebarOpen = signal(false);

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.currentUrl.set(event.urlAfterRedirects || event.url);
        this.isMobileSidebarOpen.set(false);
      });
  }

  readonly isLoginPage = computed(() => {
    return this.currentUrl().startsWith('/login');
  });

  readonly pageTitle = computed(() => {
    const url = this.currentUrl();
    if (url.startsWith('/dashboard')) return 'Dashboard';
    if (url.startsWith('/employees')) {
      const parts = url.split('/').filter(Boolean);
      if (parts.length > 1 && parts[1] !== '') {
        return 'Employee Profile';
      }
      return 'Employees';
    }
    return 'Salary Management';
  });

  toggleMobileSidebar() {
    this.isMobileSidebarOpen.update((v) => !v);
  }
}
