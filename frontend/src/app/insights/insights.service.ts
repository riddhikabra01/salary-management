import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { FilterOptions } from './insights.model';

@Injectable({ providedIn: 'root' })
export class InsightsService {
  private readonly http = inject(HttpClient);

  filterOptions(): Observable<FilterOptions> {
    return this.http.get<FilterOptions>('/api/insights/filter-options');
  }
}
