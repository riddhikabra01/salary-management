package com.acme.salary.insights;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns raw database aggregates into the figures the Insights page shows.
 * Everything is read-only.
 */
@Service
@Transactional(readOnly = true)
public class InsightsService {

    // Upper limit on distribution bands, to keep the chart readable and the number of queries bounded.
    static final int MAX_BANDS = 20;

    private final InsightsRepository repository;

    public InsightsService(InsightsRepository repository) {
        this.repository = repository;
    }

    public List<CountrySalaryStats> salaryStatsByCountry() {
        return repository.statsByCountry().stream()
                .map(InsightsService::toCountryStats)
                .toList();
    }

    public CountrySalaryStats salaryStatsForCountry(String country) {
        return repository.statsForCountry(country)
                .map(InsightsService::toCountryStats)
                .orElseThrow(() -> new NoSalaryDataException("No employees found in " + country));
    }

    public JobTitleSalaryStats averageSalaryForJobTitle(String country, String jobTitle) {
        return repository.jobTitleStatsInCountry(country, jobTitle)
                .map(row -> new JobTitleSalaryStats(
                        country, row.getJobTitle(), row.getCurrency(),
                        row.getHeadcount().longValue(), money(row.getAverageSalary())))
                .orElseThrow(() -> new NoSalaryDataException(
                        "No " + jobTitle + " employees found in " + country));
    }

    public SalaryDistribution salaryDistribution(String country, int bands) {
        if (bands < 1 || bands > MAX_BANDS) {
            throw new IllegalArgumentException("Bands must be between 1 and " + MAX_BANDS);
        }
        CountrySalaryStats stats = salaryStatsForCountry(country);
        return new SalaryDistribution(country, stats.currency(), splitIntoBands(stats, bands));
    }

    public FilterOptions filterOptions() {
        return new FilterOptions(
                repository.distinctCountries(),
                repository.distinctDepartments(),
                repository.distinctJobTitles());
    }

    // Splits min..max into equal-width ranges and counts the employees in each.
    // If everyone earns the same, there is nothing to split, so we return a single band.
    private List<SalaryBand> splitIntoBands(CountrySalaryStats stats, int bands) {
        BigDecimal min = stats.minSalary();
        BigDecimal max = stats.maxSalary();

        if (min.compareTo(max) == 0) {
            return List.of(new SalaryBand(min, max, stats.headcount()));
        }

        BigDecimal range = max.subtract(min);
        List<SalaryBand> result = new ArrayList<>();
        BigDecimal lower = min;

        for (int band = 1; band <= bands; band++) {
            // The last band uses the exact max as its edge, avoiding rounding drift from the divisions.
            boolean isLast = band == bands;
            BigDecimal upper = isLast ? max : min.add(boundaryOffset(range, band, bands));
            long headcount = isLast
                    ? repository.countByCountryAndSalaryBetween(stats.country(), lower, upper)
                    : repository.countByCountryAndSalaryGreaterThanEqualAndSalaryLessThan(
                            stats.country(), lower, upper);
            result.add(new SalaryBand(lower, upper, headcount));
            lower = upper;
        }
        return result;
    }

    private static BigDecimal boundaryOffset(BigDecimal range, int band, int bands) {
        return range.multiply(BigDecimal.valueOf(band))
                .divide(BigDecimal.valueOf(bands), 2, RoundingMode.HALF_UP);
    }

    private static CountrySalaryStats toCountryStats(SalaryStatsRow row) {
        return new CountrySalaryStats(
                row.getCountry(), row.getCurrency(), row.getHeadcount().longValue(),
                money(row.getMinSalary()), money(row.getMaxSalary()), money(row.getAverageSalary()));
    }

    // Averages can come back as doubles; round everything to 2 decimals the same way.
    private static BigDecimal money(Number value) {
        BigDecimal decimal = value instanceof BigDecimal exact
                ? exact
                : BigDecimal.valueOf(value.doubleValue());
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }
}
