import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { PageEvent, MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Subject, catchError, debounceTime, of, switchMap, tap } from 'rxjs';

import { FilterOptions } from '../../insights/insights.model';
import { InsightsService } from '../../insights/insights.service';
import { Employee, EmployeeQuery, SortDirection } from '../employee.model';
import { EmployeeService } from '../employee.service';

const DEFAULT_SORT = 'fullName';
const SEARCH_DELAY_MS = 300;

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    CurrencyPipe,
    DatePipe,
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
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload$ = new Subject<void>();

  readonly columns = ['fullName', 'email', 'jobTitle', 'department', 'country', 'salary', 'joiningDate'];
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
    this.insightsService
      .filterOptions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (options) => this.options.set(options), error: () => undefined });

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
