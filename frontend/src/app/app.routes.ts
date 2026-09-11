import { Routes } from '@angular/router';
import { LoginPage } from './features/auth/login-page/login-page';
import { DashboardPage } from './features/dashboard/dashboard-page/dashboard-page';
import { EmployeeListPage } from './features/employees/employee-list-page/employee-list-page';
import { EmployeeDetailPage } from './features/employees/employee-detail-page/employee-detail-page';
import { authGuard } from './core/auth/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login', component: LoginPage },
  { path: 'dashboard', component: DashboardPage, canActivate: [authGuard] },
  { path: 'employees', component: EmployeeListPage, canActivate: [authGuard] },
  { path: 'employees/:id', component: EmployeeDetailPage, canActivate: [authGuard] },
];
