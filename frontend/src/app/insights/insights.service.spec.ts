import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { FilterOptions } from './insights.model';
import { InsightsService } from './insights.service';

describe('InsightsService', () => {
  let service: InsightsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InsightsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the values for the filter dropdowns', () => {
    const options: FilterOptions = {
      countries: ['Germany', 'India'],
      departments: ['Engineering'],
      jobTitles: ['QA Engineer'],
    };
    let received: FilterOptions | undefined;

    service.filterOptions().subscribe((value) => (received = value));

    const request = http.expectOne('/api/insights/filter-options');
    expect(request.request.method).toBe('GET');
    request.flush(options);
    expect(received).toEqual(options);
  });
});
