import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { FilterOptions } from '../../insights/insights.model';
import { ConfirmDeleteDialogComponent } from '../confirm-delete-dialog/confirm-delete-dialog.component';
import { EmployeeFormDialogComponent } from '../employee-form-dialog/employee-form-dialog.component';
import { Employee, Page } from '../employee.model';
import { EmployeeListComponent } from './employee-list.component';

const SEARCH_DELAY_MS = 300;

const ASHA: Employee = {
  id: 1,
  fullName: 'Asha Patel',
  email: 'asha.patel@acme.com',
  jobTitle: 'Software Engineer',
  department: 'Engineering',
  country: 'India',
  salary: 1800000,
  currency: 'INR',
  joiningDate: '2022-04-01',
};

const LENA: Employee = {
  id: 2,
  fullName: 'Lena Fischer',
  email: 'lena.fischer@acme.com',
  jobTitle: 'Data Analyst',
  department: 'Analytics',
  country: 'Germany',
  salary: 70000,
  currency: 'EUR',
  joiningDate: '2019-09-15',
};

const OPTIONS: FilterOptions = {
  countries: ['Germany', 'India'],
  departments: ['Analytics', 'Engineering'],
  jobTitles: ['Data Analyst', 'Software Engineer'],
};

function pageOf(content: Employee[], totalElements = content.length): Page<Employee> {
  return { content, page: 0, size: 25, totalElements, totalPages: 1 };
}

const isEmployeeRequest = (request: HttpRequest<unknown>) => request.url === '/api/employees';
const isFilterOptionsRequest = (request: HttpRequest<unknown>) => request.url === '/api/insights/filter-options';

describe('EmployeeListComponent', () => {
  let fixture: ComponentFixture<EmployeeListComponent>;
  let component: EmployeeListComponent;
  let http: HttpTestingController;
  let dialog: MatDialog;
  let snackBar: MatSnackBar;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    dialog = TestBed.inject(MatDialog);
    snackBar = TestBed.inject(MatSnackBar);
    spyOn(snackBar, 'open');
  });

  afterEach(() => http.verify());

  function start(employees: Employee[] = [ASHA, LENA], totalElements = employees.length): TestRequest {
    fixture.detectChanges();
    const employeeRequest = http.expectOne(isEmployeeRequest);
    http.expectOne(isFilterOptionsRequest).flush(OPTIONS);
    employeeRequest.flush(pageOf(employees, totalElements));
    fixture.detectChanges();
    return employeeRequest;
  }

  function dialogWillClose(result: unknown): jasmine.Spy {
    return spyOn(dialog, 'open').and.returnValue({
      afterClosed: () => of(result),
    } as unknown as MatDialogRef<unknown>);
  }

  function click(selector: string): void {
    const button: HTMLButtonElement = fixture.nativeElement.querySelector(selector);
    button.click();
  }

  it('shows the employees returned by the server', () => {
    start();

    const rows = fixture.nativeElement.querySelectorAll('tr[mat-row]');
    expect(rows.length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Asha Patel');
    expect(fixture.nativeElement.textContent).toContain('Lena Fischer');
  });

  it('shows how many employees match', () => {
    start([ASHA], 10000);

    expect(fixture.nativeElement.textContent).toContain('10,000 employees');
  });

  it('uses the singular when exactly one employee matches', () => {
    start([ASHA], 1);

    expect(fixture.nativeElement.textContent).toMatch(/\b1 employee(?!s)/);
  });

  it('asks for the first page sorted by name by default', () => {
    const request = start();

    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('25');
    expect(request.request.params.get('sortBy')).toBe('fullName');
    expect(request.request.params.get('direction')).toBe('asc');
  });

  it('loads the values for the filter dropdowns', () => {
    start();

    expect(component.options()).toEqual(OPTIONS);
  });

  it('searches after the user stops typing', fakeAsync(() => {
    start();

    component.filters.controls.search.setValue('asha');
    tick(SEARCH_DELAY_MS);

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('search')).toBe('asha');
    expect(request.request.params.get('page')).toBe('0');
    request.flush(pageOf([ASHA]));
  }));

  it('filters by country', fakeAsync(() => {
    start();

    component.filters.controls.country.setValue('India');
    tick(SEARCH_DELAY_MS);

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('country')).toBe('India');
    request.flush(pageOf([ASHA]));
  }));

  it('goes back to the first page when a filter changes', fakeAsync(() => {
    start();
    component.onPage({ pageIndex: 3, pageSize: 25, length: 200 });
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA], 200));

    component.filters.controls.department.setValue('Engineering');
    tick(SEARCH_DELAY_MS);

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('page')).toBe('0');
    request.flush(pageOf([ASHA]));
  }));

  it('clears every filter', fakeAsync(() => {
    start();
    component.filters.controls.search.setValue('asha');
    tick(SEARCH_DELAY_MS);
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA]));

    component.clearFilters();
    tick(SEARCH_DELAY_MS);

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.has('search')).toBeFalse();
    request.flush(pageOf([ASHA, LENA]));
  }));

  it('requests the page the user moves to', () => {
    start();

    component.onPage({ pageIndex: 2, pageSize: 50, length: 500 });

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('50');
    request.flush(pageOf([ASHA], 500));
  });

  it('requests the sort order the user picks', () => {
    start();

    component.onSort({ active: 'salary', direction: 'desc' });

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('sortBy')).toBe('salary');
    expect(request.request.params.get('direction')).toBe('desc');
    expect(request.request.params.get('page')).toBe('0');
    request.flush(pageOf([ASHA, LENA]));
  });

  it('returns to the default sort order when sorting is cleared', () => {
    start();

    component.onSort({ active: 'salary', direction: '' });

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('sortBy')).toBe('fullName');
    expect(request.request.params.get('direction')).toBe('asc');
    request.flush(pageOf([ASHA, LENA]));
  });

  it('tells the user when nobody matches', () => {
    start([]);

    expect(fixture.nativeElement.textContent).toContain('No employees found');
  });

  it('shows an error when the employees cannot be loaded', () => {
    fixture.detectChanges();
    const employeeRequest = http.expectOne(isEmployeeRequest);
    http.expectOne(isFilterOptionsRequest).flush(OPTIONS);

    employeeRequest.flush('Something went wrong', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert: HTMLElement = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert.textContent).toContain('Could not load employees');
  });

  it('counts only the filters that are in use', fakeAsync(() => {
    start();
    expect(component.activeFilterCount()).toBe(0);

    component.filters.controls.country.setValue('India');
    component.filters.controls.search.setValue('   ');
    tick(SEARCH_DELAY_MS);
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA]));

    expect(component.activeFilterCount()).toBe(1);
  }));

  it('shows a filter icon with the number of active filters', fakeAsync(() => {
    start();
    component.filters.controls.country.setValue('India');
    tick(SEARCH_DELAY_MS);
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA]));
    fixture.detectChanges();

    const toggle: HTMLElement = fixture.nativeElement.querySelector('.filters-toggle');
    expect(toggle.querySelector('svg')).not.toBeNull();
    expect(toggle.querySelector('.badge')?.textContent?.trim()).toBe('1');
  }));

  it('hides and shows the dropdown filters', () => {
    start();
    expect(fixture.nativeElement.querySelectorAll('.filters-fields mat-select').length).toBe(3);

    click('.filters-toggle');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.filters-fields mat-select').length).toBe(0);

    click('.filters-toggle');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.filters-fields mat-select').length).toBe(3);
  });

  it('describes the default sort and offers no reset', () => {
    start();

    const summary: HTMLElement = fixture.nativeElement.querySelector('.sort-summary');
    expect(summary.textContent).toContain('Name (A–Z)');
    expect(fixture.nativeElement.querySelector('button.reset-sort')).toBeNull();
  });

  it('explains that equal values are ordered by name', () => {
    start();

    component.onSort({ active: 'salary', direction: 'desc' });
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA, LENA]));
    fixture.detectChanges();

    const summary: HTMLElement = fixture.nativeElement.querySelector('.sort-summary');
    expect(summary.textContent).toContain('Salary (high to low), then Name (A–Z)');
  });

  it('resets the sort to the default', () => {
    start();
    component.onSort({ active: 'department', direction: 'desc' });
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA, LENA]));
    fixture.detectChanges();

    click('button.reset-sort');

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('sortBy')).toBe('fullName');
    expect(request.request.params.get('direction')).toBe('asc');
    expect(request.request.params.get('page')).toBe('0');
    request.flush(pageOf([ASHA, LENA]));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('button.reset-sort')).toBeNull();
  });

  it('sends no secondary sort by default', () => {
    const request = start();

    expect(request.request.params.has('secondarySortBy')).toBeFalse();
    expect(request.request.params.has('secondaryDirection')).toBeFalse();
  });

  it('offers every column except the main sort column as a secondary sort', () => {
    start();

    const values = component.secondaryOptions().map((option) => option.value);
    expect(values).not.toContain('fullName');
    expect(values).toContain('department');
  });

  it('sends the chosen secondary sort', () => {
    start();

    component.onSecondarySort('department');

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('secondarySortBy')).toBe('department');
    expect(request.request.params.get('secondaryDirection')).toBe('asc');
    expect(request.request.params.get('page')).toBe('0');
    request.flush(pageOf([ASHA, LENA]));
  });

  it('sends the secondary direction the user picks', () => {
    start();
    component.onSecondarySort('salary');
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA, LENA]));

    component.onSecondaryDirection('desc');

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('secondarySortBy')).toBe('salary');
    expect(request.request.params.get('secondaryDirection')).toBe('desc');
    request.flush(pageOf([ASHA, LENA]));
  });

  it('drops the secondary sort when that column becomes the main sort', () => {
    start();
    component.onSecondarySort('department');
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA, LENA]));

    component.onSort({ active: 'department', direction: 'asc' });

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.get('sortBy')).toBe('department');
    expect(request.request.params.has('secondarySortBy')).toBeFalse();
    request.flush(pageOf([ASHA, LENA]));
  });

  it('describes the secondary sort in the summary', () => {
    start();
    component.onSort({ active: 'country', direction: 'asc' });
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA, LENA]));
    component.onSecondarySort('department');
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA, LENA]));
    component.onSecondaryDirection('desc');
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA, LENA]));
    fixture.detectChanges();

    const summary: HTMLElement = fixture.nativeElement.querySelector('.sort-summary');
    expect(summary.textContent).toContain('Country (A–Z), then Department (Z–A), then Name (A–Z)');
  });

  it('uses wording that suits each column', () => {
    expect(component.directionLabel('department', 'asc')).toBe('A–Z');
    expect(component.directionLabel('department', 'desc')).toBe('Z–A');
    expect(component.directionLabel('salary', 'asc')).toBe('Low to high');
    expect(component.directionLabel('salary', 'desc')).toBe('High to low');
    expect(component.directionLabel('joiningDate', 'asc')).toBe('Oldest first');
    expect(component.directionLabel('joiningDate', 'desc')).toBe('Newest first');
  });

  it('clears the secondary sort when the sort is reset', () => {
    start();
    component.onSecondarySort('department');
    http.expectOne(isEmployeeRequest).flush(pageOf([ASHA, LENA]));

    component.resetSort();

    const request = http.expectOne(isEmployeeRequest);
    expect(request.request.params.has('secondarySortBy')).toBeFalse();
    request.flush(pageOf([ASHA, LENA]));
  });

  describe('adding, editing and deleting', () => {
    function reloadAfterChange(employees: Employee[] = [ASHA, LENA]): TestRequest {
      http.expectOne(isFilterOptionsRequest).flush(OPTIONS);
      const request = http.expectOne(isEmployeeRequest);
      request.flush(pageOf(employees));
      return request;
    }

    it('opens an empty form and refreshes the list after saving', () => {
      start();
      const open = dialogWillClose(ASHA);

      click('button.add');

      expect(open).toHaveBeenCalledWith(
        EmployeeFormDialogComponent,
        jasmine.objectContaining({ data: jasmine.objectContaining({ employee: null }) }),
      );
      reloadAfterChange();
      expect(snackBar.open).toHaveBeenCalledWith('Employee added', 'Dismiss', jasmine.anything());
    });

    it('does not refresh the list when the form is cancelled', () => {
      start();
      dialogWillClose(undefined);

      click('button.add');

      http.expectNone(isEmployeeRequest);
      expect(snackBar.open).not.toHaveBeenCalled();
    });

    it('opens the form with the chosen employee for editing', () => {
      start();
      const open = dialogWillClose(ASHA);

      click('button[aria-label="Edit Asha Patel"]');

      expect(open).toHaveBeenCalledWith(
        EmployeeFormDialogComponent,
        jasmine.objectContaining({ data: jasmine.objectContaining({ employee: ASHA }) }),
      );
      reloadAfterChange();
      expect(snackBar.open).toHaveBeenCalledWith('Employee updated', 'Dismiss', jasmine.anything());
    });

    it('deletes an employee after confirmation', () => {
      start();
      const open = dialogWillClose(true);

      click('button[aria-label="Delete Asha Patel"]');

      expect(open).toHaveBeenCalledWith(
        ConfirmDeleteDialogComponent,
        jasmine.objectContaining({ data: ASHA }),
      );
      const deletion = http.expectOne('/api/employees/1');
      expect(deletion.request.method).toBe('DELETE');
      deletion.flush(null, { status: 204, statusText: 'No Content' });
      reloadAfterChange([LENA]);
      expect(snackBar.open).toHaveBeenCalledWith('Employee deleted', 'Dismiss', jasmine.anything());
    });

    it('keeps the employee when the deletion is not confirmed', () => {
      start();
      const open = dialogWillClose(false);

      click('button[aria-label="Delete Asha Patel"]');

      http.expectNone('/api/employees/1');
      expect(open).toHaveBeenCalledTimes(1);
    });

    it('goes back a page when the last employee on a page is deleted', () => {
      start([ASHA], 26);
      component.onPage({ pageIndex: 1, pageSize: 25, length: 26 });
      http.expectOne(isEmployeeRequest).flush(pageOf([ASHA], 26));
      fixture.detectChanges();
      dialogWillClose(true);

      click('button[aria-label="Delete Asha Patel"]');
      http.expectOne('/api/employees/1').flush(null, { status: 204, statusText: 'No Content' });

      http.expectOne(isFilterOptionsRequest).flush(OPTIONS);
      const reload = http.expectOne(isEmployeeRequest);
      expect(reload.request.params.get('page')).toBe('0');
      reload.flush(pageOf([LENA], 25));
    });

    it('shows an error when the deletion fails', () => {
      start();
      dialogWillClose(true);

      click('button[aria-label="Delete Asha Patel"]');
      http.expectOne('/api/employees/1').flush('Oops', { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();

      const alert: HTMLElement = fixture.nativeElement.querySelector('[role="alert"]');
      expect(alert.textContent).toContain('Could not delete the employee');
    });
  });
});
