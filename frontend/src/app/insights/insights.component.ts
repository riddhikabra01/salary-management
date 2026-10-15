import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';

import {
  CountrySalaryStats,
  JobTitleSalaryStats,
  SalaryDistribution,
} from './insights.model';
import { InsightsService } from './insights.service';

// How many salary ranges the distribution chart starts with.
const DEFAULT_BANDS = 5;
// A tiny but non-zero count would otherwise draw an invisible bar; this keeps it visible.
const MIN_BAR_WIDTH_PERCENT = 2;

/**
 * The Insights page: a table of every country, plus details for the selected country
 * (min / average / max, a salary distribution, and the average for a chosen job title).
 */
@Component({
  selector: 'app-insights',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, MatFormFieldModule, MatProgressBarModule, MatSelectModule],
  templateUrl: './insights.component.html',
  styleUrl: './insights.component.scss',
})
export class InsightsComponent implements OnInit {
  private readonly insights = inject(InsightsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly bandOptions = [5, 8, 10];

  readonly countries = signal<CountrySalaryStats[]>([]);
  readonly jobTitles = signal<string[]>([]);
  readonly selectedCountry = signal<string | null>(null);
  readonly bands = signal(DEFAULT_BANDS);
  readonly distribution = signal<SalaryDistribution | null>(null);
  readonly selectedJobTitle = signal<string | null>(null);
  readonly jobTitleResult = signal<JobTitleSalaryStats | null>(null);
  readonly jobTitleMessage = signal<string | null>(null);
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly selectedStats = computed(
    () => this.countries().find((stats) => stats.country === this.selectedCountry()) ?? null,
  );

  // The biggest band sets the 100% bar width; every other bar is drawn relative to it.
  private readonly largestBand = computed(() => {
    const headcounts = this.distribution()?.bands.map((band) => band.headcount) ?? [];
    return Math.max(1, ...headcounts);
  });

  ngOnInit(): void {
    this.loading.set(true);

    this.insights
      .countryStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (stats) => {
          this.loading.set(false);
          this.countries.set(stats);
          if (stats.length > 0) {
            this.selectCountry(stats[0].country);
          }
        },
        error: () => {
          this.loading.set(false);
          this.errorMessage.set('Could not load the salary insights. Please try again.');
        },
      });

    this.insights
      .filterOptions()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (options) => this.jobTitles.set(options.jobTitles), error: () => undefined });
  }

  // Switching country clears the old chart right away, so stale numbers are never shown under a new name.
  selectCountry(country: string): void {
    this.selectedCountry.set(country);
    this.distribution.set(null);
    this.loadDistribution();
    if (this.selectedJobTitle()) {
      this.loadJobTitleAverage();
    }
  }

  changeBands(bands: number): void {
    this.bands.set(bands);
    this.loadDistribution();
  }

  selectJobTitle(jobTitle: string): void {
    this.selectedJobTitle.set(jobTitle);
    this.loadJobTitleAverage();
  }

  barWidth(headcount: number): number {
    if (headcount === 0) {
      return 0;
    }
    return Math.max(MIN_BAR_WIDTH_PERCENT, (headcount / this.largestBand()) * 100);
  }

  private loadDistribution(): void {
    const country = this.selectedCountry();
    if (!country) {
      return;
    }
    const bands = this.bands();

    this.insights
      .salaryDistribution(country, bands)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (distribution) => {
          // Ignore a slow response if the user has already picked something else.
          if (this.selectedCountry() === country && this.bands() === bands) {
            this.distribution.set(distribution);
          }
        },
        error: () => this.errorMessage.set('Could not load the salary distribution. Please try again.'),
      });
  }

  private loadJobTitleAverage(): void {
    const country = this.selectedCountry();
    const jobTitle = this.selectedJobTitle();
    if (!country || !jobTitle) {
      return;
    }
    this.jobTitleResult.set(null);
    this.jobTitleMessage.set(null);

    this.insights
      .jobTitleAverage(country, jobTitle)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          if (this.selectedCountry() === country && this.selectedJobTitle() === jobTitle) {
            this.jobTitleResult.set(result);
          }
        },
        error: (error: HttpErrorResponse) => {
          if (this.selectedCountry() !== country || this.selectedJobTitle() !== jobTitle) {
            return;
          }
          this.jobTitleMessage.set(
            // 404 here is not a failure, just "nobody has that job title in this country".
            error.status === 404
              ? `No ${jobTitle} employees found in ${country}.`
              : 'Could not load the average salary. Please try again.',
          );
        },
      });
  }
}
