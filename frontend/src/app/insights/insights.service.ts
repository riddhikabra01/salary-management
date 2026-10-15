import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CountrySalaryStats,
  FilterOptions,
  JobTitleSalaryStats,
  SalaryDistribution,
} from './insights.model';

@Injectable({ providedIn: 'root' })
export class InsightsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/insights';

  filterOptions(): Observable<FilterOptions> {
    return this.http.get<FilterOptions>(`${this.baseUrl}/filter-options`);
  }

  countryStats(): Observable<CountrySalaryStats[]> {
    return this.http.get<CountrySalaryStats[]>(`${this.baseUrl}/countries`);
  }

  salaryDistribution(country: string, bands: number): Observable<SalaryDistribution> {
    const params = new HttpParams().set('bands', bands);
    return this.http.get<SalaryDistribution>(
      `${this.baseUrl}/countries/${encodeURIComponent(country)}/distribution`,
      { params },
    );
  }

  jobTitleAverage(country: string, jobTitle: string): Observable<JobTitleSalaryStats> {
    const params = new HttpParams().set('country', country).set('jobTitle', jobTitle);
    return this.http.get<JobTitleSalaryStats>(`${this.baseUrl}/job-title-average`, { params });
  }
}
