import { Routes } from '@angular/router';

// The two pages. Each is loaded lazily, so the browser downloads a page's code only when you open it,
// and the first load stays small. The empty path sends people to the employee list.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  {
    path: 'employees',
    loadComponent: () =>
      import('./employees/employee-list/employee-list.component').then(
        (module) => module.EmployeeListComponent,
      ),
  },
  {
    path: 'insights',
    loadComponent: () =>
      import('./insights/insights.component').then((module) => module.InsightsComponent),
  },
];
