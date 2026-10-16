import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
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

// Sort used on first load and after "Reset sort".
const DEFAULT_SORT = 'fullName';
// Wait for a short pause in typing before calling the server, so we don't fire a request per keystroke.
const SEARCH_DELAY_MS = 300;
const NOTIFICATION_MS = 4000;

// Friendly column names, used as the title of each column's "Then sort by" menu.
const COLUMN_LABELS: Record<string, string> = {
  fullName: 'Name',
  email: 'Email',
  jobTitle: 'Job title',
  department: 'Department',
  country: 'Country',
  salary: 'Salary',
  joiningDate: 'Joined',
};

// Plain-language wording for a direction, depending on what the column holds.
// "A–Z" makes no sense for a salary, so numbers and dates get their own labels.
function describeDirection(column: string, direction: SortDirection): string {
  const ascending = direction === 'asc';
  if (column === 'salary') {
    return ascending ? 'low to high' : 'high to low';
  }
  if (column === 'joiningDate') {
    return ascending ? 'oldest first' : 'newest first';
  }
  return ascending ? 'A–Z' : 'Z–A';
}

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
    MatMenuModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatSortModule,
    MatTableModule,
  ],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss',
})
/**
 * The employees screen: filters, two-level sorting, the table, paging, and add/edit/delete dialogs.
 * Every change that needs fresh data pushes into reload$, and one pipeline does the fetching.
 */
export class EmployeeListComponent implements OnInit {
  private readonly employeeService = inject(EmployeeService);
  private readonly insightsService = inject(InsightsService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  // Single "fetch again" trigger. switchMap on it cancels an outdated request when a newer one starts,
  // so a slow earlier response can't overwrite the latest one.
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
  // Whether the dropdown filters (country, department, job title) are expanded.
  readonly filtersOpen = signal(true);

  pageIndex = 0;
  pageSize = 25;
  sortBy = DEFAULT_SORT;
  direction: SortDirection = 'asc';
  // Empty string means "no secondary sort".
  secondarySortBy = '';
  secondaryDirection: SortDirection = 'asc';

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

    // Any filter change goes back to page 1, since the old page number may no longer exist.
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

  // Header click. Clicking back to "no sort" falls back to the default instead of an unsorted list.
  onSort(sort: Sort): void {
    if (sort.direction === '') {
      this.sortBy = DEFAULT_SORT;
      this.direction = 'asc';
    } else {
      this.sortBy = sort.active;
      this.direction = sort.direction;
    }
    // Sorting twice by the same column is pointless, so drop a secondary choice that now matches.
    if (this.secondarySortBy === this.sortBy) {
      this.secondarySortBy = '';
    }
    this.pageIndex = 0;
    this.reload$.next();
  }

  clearFilters(): void {
    this.filters.reset();
  }

  toggleFilters(): void {
    this.filtersOpen.update((open) => !open);
  }

  // Counts non-empty filters for the badge on the Filters button.
  activeFilterCount(): number {
    return Object.values(this.filters.getRawValue()).filter((value) => value.trim() !== '').length;
  }

  isDefaultSort(): boolean {
    return this.sortBy === DEFAULT_SORT && this.direction === 'asc' && this.secondarySortBy === '';
  }

  resetSort(): void {
    this.sortBy = DEFAULT_SORT;
    this.direction = 'asc';
    this.secondarySortBy = '';
    this.secondaryDirection = 'asc';
    this.pageIndex = 0;
    this.reload$.next();
  }

  directionLabel(column: string, direction: SortDirection): string {
    const label = describeDirection(column, direction);
    return label.charAt(0).toUpperCase() + label.slice(1);
  }

  // Called from a column's filter menu. Sets "then sort by this column" with the chosen direction.
  // The main sort column can't be its own tie-breaker, so that case is ignored.
  setSecondarySort(column: string, direction: SortDirection): void {
    if (column === this.sortBy) {
      return;
    }
    this.secondarySortBy = column;
    this.secondaryDirection = direction;
    this.pageIndex = 0;
    this.reload$.next();
  }

  // The menu's "Clear" option: back to the main sort, then name.
  clearSecondarySort(): void {
    this.secondarySortBy = '';
    this.secondaryDirection = 'asc';
    this.pageIndex = 0;
    this.reload$.next();
  }

  // True when this column is the secondary sort (optionally in a specific direction).
  // Used to tick the current choice in the menu and light up the filter icon.
  isSecondary(column: string, direction?: SortDirection): boolean {
    return this.secondarySortBy === column && (direction === undefined || this.secondaryDirection === direction);
  }

  columnLabel(column: string): string {
    return COLUMN_LABELS[column] ?? column;
  }

  // Tooltip for a sortable header label. The headers show no arrow, so this is where the current direction is spelled out.
  sortHint(column: string): string {
    if (column === this.sortBy) {
      return `Sorted ${this.directionLabel(column, this.direction)}. Click to reverse.`;
    }
    return 'Click to sort by this column';
  }

  // Tooltip for a column's filter icon.
  filterTitle(column: string): string {
    if (column === this.sortBy) {
      return 'This is the main sort column';
    }
    if (this.isSecondary(column)) {
      return `Then sorted ${this.directionLabel(column, this.secondaryDirection)}`;
    }
    return 'Then sort by this column';
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
        // Deleting the last row on a page would leave an empty page, so step back one.
        if (this.employees().length === 1 && this.pageIndex > 0) {
          this.pageIndex -= 1;
        }
        this.loadFilterOptions();
        this.reload$.next();
      },
      error: () => this.errorMessage.set('Could not delete the employee. Please try again.'),
    });
  }

  // Dropdown values for the filters. A failure is ignored on purpose: the screen still works without them.
  private loadFilterOptions(): void {
    this.insightsService
      .filterOptions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (options) => this.options.set(options), error: () => undefined });
  }

  private notify(message: string): void {
    this.snackBar.open(message, 'Dismiss', { duration: NOTIFICATION_MS });
  }

  // Snapshot of the current screen state, in the shape the API service expects.
  private currentQuery(): EmployeeQuery {
    return {
      ...this.filters.getRawValue(),
      page: this.pageIndex,
      size: this.pageSize,
      sortBy: this.sortBy,
      direction: this.direction,
      secondarySortBy: this.secondarySortBy || undefined,
      secondaryDirection: this.secondarySortBy ? this.secondaryDirection : undefined,
    };
  }
}
