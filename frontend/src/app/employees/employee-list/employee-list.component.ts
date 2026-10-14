import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Subject, catchError, debounceTime, of, switchMap, tap } from 'rxjs';

import { FilterOptions } from '../../insights/insights.model';
import { InsightsService } from '../../insights/insights.service';
import { ConfirmDeleteDialogComponent } from '../confirm-delete-dialog/confirm-delete-dialog.component';
import { EmployeeFormData, EmployeeFormDialogComponent } from '../employee-form-dialog/employee-form-dialog.component';
import { Employee, EmployeeQuery, SortDirection } from '../employee.model';
import { EmployeeService } from '../employee.service';

const DEFAULT_SORT = 'fullName';
const SEARCH_DELAY_MS = 300;
const NOTIFICATION_MS = 4000;

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
  ],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss',
})
export class EmployeeListComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly insightsService = inject(InsightsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<void>();

  readonly columns = ['fullName', 'email', 'jobTitle', 'department', 'country', 'salary', 'joiningDate', 'actions'];
  readonly pageSizeOptions = [10, 25, 50, 100];

  readonly filters = new FormGroup({
    search: new FormControl('', { nonNullable: true }),
    country: new FormControl('', { nonNullable: true }),
    department: new FormControl('', { nonNullable: true }),
    jobTitle: new FormControl('', { nonNullable: true }),
  });

  readonly employees = signal<Employee[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly options = signal<FilterOptions>({ countries: [], departments: [], jobTitles: [] });

  pageIndex = 0;
  pageSize = 25;
  sortBy = DEFAULT_SORT;
  direction: SortDirection = 'asc';

  ngOnInit(): void {
    this.loadFilterOptions();

    this.reload$
      .pipe(
        tap(() => {
          this.loading.set(true);
          this.errorMessage.set(null);
        }),
        switchMap(() =>
          this.employeeService.list(this.currentQuery()).pipe(
            catchError(() => {
              this.errorMessage.set('Could not load employees. Please try again.');
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        this.loading.set(false);
        if (page) {
          this.employees.set(page.content);
          this.totalElements.set(page.totalElements);
        }
      });

    this.filters.valueChanges
      .pipe(debounceTime(SEARCH_DELAY_MS), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.pageIndex = 0;
        this.reload$.next();
      });

    this.reload$.next();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload$.next();
  }

  onSort(sort: Sort): void {
    if (sort.direction === '') {
      this.sortBy = DEFAULT_SORT;
      this.direction = 'asc';
    } else {
      this.sortBy = sort.active;
      this.direction = sort.direction;
    }
    this.pageIndex = 0;
    this.reload$.next();
  }

  clearFilters(): void {
    this.filters.reset();
  }

  addEmployee(): void {
    this.openForm(null);
  }

  editEmployee(employee: Employee): void {
    this.openForm(employee);
  }

  deleteEmployee(employee: Employee): void {
    this.dialog
      .open<ConfirmDeleteDialogComponent, Employee, boolean>(ConfirmDeleteDialogComponent, {
        data: employee,
        width: '440px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.removeEmployee(employee);
        }
      });
  }

  private openForm(employee: Employee | null): void {
    const data: EmployeeFormData = { employee, options: this.options() };

    this.dialog
      .open<EmployeeFormDialogComponent, EmployeeFormData, Employee>(EmployeeFormDialogComponent, {
        data,
        width: '560px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.notify(employee ? 'Employee updated' : 'Employee added');
          this.loadFilterOptions();
          this.reload$.next();
        }
      });
  }

  private removeEmployee(employee: Employee): void {
    this.employeeService.delete(employee.id).subscribe({
      next: () => {
        this.notify('Employee deleted');
        if (this.employees().length === 1 && this.pageIndex > 0) {
          this.pageIndex -= 1;
        }
        this.loadFilterOptions();
        this.reload$.next();
      },
      error: () => this.errorMessage.set('Could not delete the employee. Please try again.'),
    });
  }

  private loadFilterOptions(): void {
    this.insightsService
      .filterOptions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (options) => this.options.set(options), error: () => undefined });
  }

  private notify(message: string): void {
    this.snackBar.open(message, 'Dismiss', { duration: NOTIFICATION_MS });
  }

  private currentQuery(): EmployeeQuery {
    return {
      ...this.filters.getRawValue(),
      page: this.pageIndex,
      size: this.pageSize,
      sortBy: this.sortBy,
      direction: this.direction,
    };
  }
}
