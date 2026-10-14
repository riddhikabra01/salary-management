import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  {
    path: 'employees',
    loadComponent: () =>
      import('./employees/employee-list/employee-list.component').then(
        (module) => module.EmployeeListComponent,
      ),
  },
];
