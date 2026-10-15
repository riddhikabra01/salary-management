import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Employee, EmployeeQuery, EmployeeRequest, Page } from './employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/employees';

  // Builds the query string. Blank filters are left out entirely so the URL stays clean
  // and the backend doesn't treat an empty string as a real filter.
  list(query: EmployeeQuery): Observable<Page<Employee>> {
    let params = new HttpParams()
      .set('page', query.page)
      .set('size', query.size)
      .set('sortBy', query.sortBy)
      .set('direction', query.direction);

    // Only send the secondary level when one is chosen.
    if (query.secondarySortBy) {
      params = params
        .set('secondarySortBy', query.secondarySortBy)
        .set('secondaryDirection', query.secondaryDirection ?? 'asc');
    }

    const filters: Array<[string, string]> = [
      ['search', query.search],
      ['country', query.country],
      ['department', query.department],
      ['jobTitle', query.jobTitle],
    ];
    for (const [name, value] of filters) {
      if (value.trim() !== '') {
        params = params.set(name, value.trim());
      }
    }

    return this.http.get<Page<Employee>>(this.baseUrl, { params });
  }

  create(request: EmployeeRequest): Observable<Employee> {
    return this.http.post<Employee>(this.baseUrl, request);
  }

  update(id: number, request: EmployeeRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
