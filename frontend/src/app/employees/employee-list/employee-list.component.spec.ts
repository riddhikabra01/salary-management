import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { FilterOptions } from '../../insights/insights.model';
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

describe('EmployeeListComponent', () => {
  let fixture: ComponentFixture<EmployeeListComponent>;
  let component: EmployeeListComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmployeeListComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(EmployeeListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function start(employees: Employee[] = [ASHA, LENA], totalElements = employees.length): TestRequest {
    fixture.detectChanges();
    const employeeRequest = http.expectOne(isEmployeeRequest);
    http.expectOne('/api/insights/filter-options').flush(OPTIONS);
    employeeRequest.flush(pageOf(employees, totalElements));
    fixture.detectChanges();
    return employeeRequest;
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
    http.expectOne('/api/insights/filter-options').flush(OPTIONS);

    employeeRequest.flush('Something went wrong', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert: HTMLElement = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert.textContent).toContain('Could not load employees');
  });
});
