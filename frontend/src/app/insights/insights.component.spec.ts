import { HttpRequest, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { InsightsComponent } from './insights.component';
import { CountrySalaryStats, FilterOptions, SalaryDistribution } from './insights.model';

const GERMANY: CountrySalaryStats = {
  country: 'Germany',
  currency: 'EUR',
  headcount: 824,
  minSalary: 28700,
  maxSalary: 153600,
  averageSalary: 70298.79,
};

const INDIA: CountrySalaryStats = {
  country: 'India',
  currency: 'INR',
  headcount: 3006,
  minSalary: 906600,
  maxSalary: 4997000,
  averageSalary: 2211939.49,
};

const OPTIONS: FilterOptions = {
  countries: ['Germany', 'India'],
  departments: ['Analytics', 'Engineering'],
  jobTitles: ['Data Analyst', 'QA Engineer', 'Software Engineer'],
};

const GERMANY_DISTRIBUTION: SalaryDistribution = {
  country: 'Germany',
  currency: 'EUR',
  bands: [
    { from: 28700, to: 91150, headcount: 600 },
    { from: 91150, to: 153600, headcount: 224 },
  ],
};

const INDIA_DISTRIBUTION: SalaryDistribution = {
  country: 'India',
  currency: 'INR',
  bands: [{ from: 906600, to: 4997000, headcount: 3006 }],
};

const distributionOf = (country: string) => (request: HttpRequest<unknown>) =>
  request.url === `/api/insights/countries/${encodeURIComponent(country)}/distribution`;

const isJobTitleRequest = (request: HttpRequest<unknown>) => request.url === '/api/insights/job-title-average';

describe('InsightsComponent', () => {
  let fixture: ComponentFixture<InsightsComponent>;
  let component: InsightsComponent;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InsightsComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideNoopAnimations()],
    }).compileComponents();

    fixture = TestBed.createComponent(InsightsComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function start(): TestRequest {
    fixture.detectChanges();
    http.expectOne('/api/insights/filter-options').flush(OPTIONS);
    http.expectOne('/api/insights/countries').flush([GERMANY, INDIA]);
    const distribution = http.expectOne(distributionOf('Germany'));
    distribution.flush(GERMANY_DISTRIBUTION);
    fixture.detectChanges();
    return distribution;
  }

  function text(): string {
    return fixture.nativeElement.textContent;
  }

  it('shows a row for every country', () => {
    start();

    const rows = fixture.nativeElement.querySelectorAll('.overview tbody tr');
    expect(rows.length).toBe(2);
    expect(text()).toContain('Germany');
    expect(text()).toContain('India');
  });

  it('shows the minimum, average and maximum for the first country', () => {
    start();

    expect(fixture.nativeElement.querySelectorAll('.stat').length).toBe(4);
    expect(text()).toContain('€28,700');
    expect(text()).toContain('€70,299');
    expect(text()).toContain('€153,600');
  });

  it('asks for five salary bands by default', () => {
    const request = start();

    expect(request.request.params.get('bands')).toBe('5');
  });

  it('draws a bar for every band with its headcount', () => {
    start();

    const rows = fixture.nativeElement.querySelectorAll('.bar-row');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('600');
    expect(rows[1].textContent).toContain('224');
  });

  it('scales the bars to the largest band', () => {
    start();

    expect(component.barWidth(600)).toBe(100);
    expect(component.barWidth(300)).toBe(50);
    expect(component.barWidth(0)).toBe(0);
    expect(component.barWidth(1)).toBeGreaterThanOrEqual(2);
  });

  it('shows another country when it is selected', () => {
    start();

    component.selectCountry('India');
    http.expectOne(distributionOf('India')).flush(INDIA_DISTRIBUTION);
    fixture.detectChanges();

    expect(text()).toContain('₹906,600');
    expect(text()).toContain('₹2,211,939');
  });

  it('asks for the chosen number of bands', () => {
    start();

    component.changeBands(8);

    const request = http.expectOne(distributionOf('Germany'));
    expect(request.request.params.get('bands')).toBe('8');
    request.flush(GERMANY_DISTRIBUTION);
  });

  it('shows the average salary for a job title', () => {
    start();

    component.selectJobTitle('Software Engineer');
    const request = http.expectOne(isJobTitleRequest);
    expect(request.request.params.get('country')).toBe('Germany');
    expect(request.request.params.get('jobTitle')).toBe('Software Engineer');
    request.flush({
      country: 'Germany',
      jobTitle: 'Software Engineer',
      currency: 'EUR',
      headcount: 120,
      averageSalary: 98000,
    });
    fixture.detectChanges();

    expect(text()).toContain('Software Engineer');
    expect(text()).toContain('earn on average');
    expect(text()).toContain('€98,000');
    expect(text()).toContain('120 employees');
  });

  it('says so when a job title has no employees in the country', () => {
    start();

    component.selectJobTitle('Data Analyst');
    http.expectOne(isJobTitleRequest).flush(
      { message: 'No Data Analyst employees found in Germany' },
      { status: 404, statusText: 'Not Found' },
    );
    fixture.detectChanges();

    expect(text()).toContain('No Data Analyst employees found in Germany.');
  });

  it('looks up the chosen job title again when the country changes', () => {
    start();
    component.selectJobTitle('QA Engineer');
    http.expectOne(isJobTitleRequest).flush({
      country: 'Germany',
      jobTitle: 'QA Engineer',
      currency: 'EUR',
      headcount: 10,
      averageSalary: 60000,
    });

    component.selectCountry('India');

    http.expectOne(distributionOf('India')).flush(INDIA_DISTRIBUTION);
    const lookup = http.expectOne(isJobTitleRequest);
    expect(lookup.request.params.get('country')).toBe('India');
    expect(lookup.request.params.get('jobTitle')).toBe('QA Engineer');
    lookup.flush({
      country: 'India',
      jobTitle: 'QA Engineer',
      currency: 'INR',
      headcount: 50,
      averageSalary: 1800000,
    });
  });

  it('shows an error when the insights cannot be loaded', () => {
    fixture.detectChanges();
    http.expectOne('/api/insights/filter-options').flush(OPTIONS);

    http.expectOne('/api/insights/countries').flush('Oops', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert: HTMLElement = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert.textContent).toContain('Could not load the salary insights');
  });
});
