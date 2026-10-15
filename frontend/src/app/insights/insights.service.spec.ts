import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CountrySalaryStats, FilterOptions } from './insights.model';
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

  it('loads the salary stats for every country', () => {
    const stats: CountrySalaryStats[] = [
      { country: 'India', currency: 'INR', headcount: 3, minSalary: 1, maxSalary: 3, averageSalary: 2 },
    ];
    let received: CountrySalaryStats[] | undefined;

    service.countryStats().subscribe((value) => (received = value));

    const request = http.expectOne('/api/insights/countries');
    expect(request.request.method).toBe('GET');
    request.flush(stats);
    expect(received).toEqual(stats);
  });

  it('asks for the salary distribution of a country with the number of bands', () => {
    service.salaryDistribution('India', 8).subscribe();

    const request = http.expectOne((req) => req.url === '/api/insights/countries/India/distribution');
    expect(request.request.params.get('bands')).toBe('8');
    request.flush({ country: 'India', currency: 'INR', bands: [] });
  });

  it('encodes country names that contain spaces', () => {
    service.salaryDistribution('United Kingdom', 5).subscribe();

    const request = http.expectOne((req) => req.url === '/api/insights/countries/United%20Kingdom/distribution');
    request.flush({ country: 'United Kingdom', currency: 'GBP', bands: [] });
  });

  it('asks for the average salary of a job title in a country', () => {
    service.jobTitleAverage('India', 'Software Engineer').subscribe();

    const request = http.expectOne((req) => req.url === '/api/insights/job-title-average');
    expect(request.request.params.get('country')).toBe('India');
    expect(request.request.params.get('jobTitle')).toBe('Software Engineer');
    request.flush({ country: 'India', jobTitle: 'Software Engineer', currency: 'INR', headcount: 1, averageSalary: 1 });
  });
});
