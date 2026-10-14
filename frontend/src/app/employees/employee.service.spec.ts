import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Employee, EmployeeQuery, EmployeeRequest } from './employee.model';
import { EmployeeService } from './employee.service';

describe('EmployeeService', () => {
  let service: EmployeeService;
  let http: HttpTestingController;

  const noFilters: EmployeeQuery = {
    search: '',
    country: '',
    department: '',
    jobTitle: '',
    page: 0,
    size: 25,
    sortBy: 'fullName',
    direction: 'asc',
  };

  const request: EmployeeRequest = {
    fullName: 'Asha Patel',
    email: 'asha.patel@acme.com',
    jobTitle: 'Software Engineer',
    department: 'Engineering',
    country: 'India',
    salary: 1800000,
    currency: 'INR',
    joiningDate: '2022-04-01',
  };

  const saved: Employee = { id: 7, ...request };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EmployeeService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the page, size and sort order', () => {
    service.list({ ...noFilters, page: 1, size: 50, sortBy: 'salary', direction: 'desc' }).subscribe();

    const call = http.expectOne((req) => req.url === '/api/employees');
    expect(call.request.method).toBe('GET');
    expect(call.request.params.get('page')).toBe('1');
    expect(call.request.params.get('size')).toBe('50');
    expect(call.request.params.get('sortBy')).toBe('salary');
    expect(call.request.params.get('direction')).toBe('desc');
    call.flush({ content: [], page: 1, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('sends the filters that have a value', () => {
    service
      .list({ ...noFilters, search: ' asha ', country: 'India', department: 'Engineering', jobTitle: 'QA Engineer' })
      .subscribe();

    const call = http.expectOne((req) => req.url === '/api/employees');
    expect(call.request.params.get('search')).toBe('asha');
    expect(call.request.params.get('country')).toBe('India');
    expect(call.request.params.get('department')).toBe('Engineering');
    expect(call.request.params.get('jobTitle')).toBe('QA Engineer');
    call.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('leaves out filters that are blank', () => {
    service.list({ ...noFilters, search: '   ' }).subscribe();

    const call = http.expectOne((req) => req.url === '/api/employees');
    expect(call.request.params.has('search')).toBeFalse();
    expect(call.request.params.has('country')).toBeFalse();
    expect(call.request.params.has('department')).toBeFalse();
    expect(call.request.params.has('jobTitle')).toBeFalse();
    call.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('creates an employee', () => {
    let created: Employee | undefined;
    service.create(request).subscribe((employee) => (created = employee));

    const call = http.expectOne('/api/employees');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush(saved);
    expect(created).toEqual(saved);
  });

  it('updates an employee', () => {
    let updated: Employee | undefined;
    service.update(7, request).subscribe((employee) => (updated = employee));

    const call = http.expectOne('/api/employees/7');
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(request);
    call.flush(saved);
    expect(updated).toEqual(saved);
  });

  it('deletes an employee', () => {
    let finished = false;
    service.delete(7).subscribe(() => (finished = true));

    const call = http.expectOne('/api/employees/7');
    expect(call.request.method).toBe('DELETE');
    call.flush(null, { status: 204, statusText: 'No Content' });
    expect(finished).toBeTrue();
  });
});
