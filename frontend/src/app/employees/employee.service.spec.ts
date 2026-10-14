import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EmployeeQuery } from './employee.model';
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

    const request = http.expectOne((req) => req.url === '/api/employees');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('50');
    expect(request.request.params.get('sortBy')).toBe('salary');
    expect(request.request.params.get('direction')).toBe('desc');
    request.flush({ content: [], page: 1, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('sends the filters that have a value', () => {
    service
      .list({ ...noFilters, search: ' asha ', country: 'India', department: 'Engineering', jobTitle: 'QA Engineer' })
      .subscribe();

    const request = http.expectOne((req) => req.url === '/api/employees');
    expect(request.request.params.get('search')).toBe('asha');
    expect(request.request.params.get('country')).toBe('India');
    expect(request.request.params.get('department')).toBe('Engineering');
    expect(request.request.params.get('jobTitle')).toBe('QA Engineer');
    request.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('leaves out filters that are blank', () => {
    service.list({ ...noFilters, search: '   ' }).subscribe();

    const request = http.expectOne((req) => req.url === '/api/employees');
    expect(request.request.params.has('search')).toBeFalse();
    expect(request.request.params.has('country')).toBeFalse();
    expect(request.request.params.has('department')).toBeFalse();
    expect(request.request.params.has('jobTitle')).toBeFalse();
    request.flush({ content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 });
  });
});
