import { Routes } from '@angular/router';
import { LoginPage } from './features/auth/login-page/login-page';
import { DashboardPage } from './features/dashboard/dashboard-page/dashboard-page';
import { EmployeeListPage } from './features/employees/employee-list-page/employee-list-page';
import { EmployeeDetailPage } from './features/employees/employee-detail-page/employee-detail-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'login', component: LoginPage },
  { path: 'dashboard', component: DashboardPage },
  { path: 'employees', component: EmployeeListPage },
  { path: 'employees/:id', component: EmployeeDetailPage },
];
