package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

    @Mock
    private InsightsRepository repository;

    @InjectMocks
    private InsightsService service;

    @Test
    void returnsSalaryStatsForACountry() {
        when(repository.statsForCountry("India")).thenReturn(Optional.of(
                new CountryRow("India", "INR", 3L, "600000.00", "1800000.00", 1200000.456)));

        CountrySalaryStats stats = service.salaryStatsForCountry("India");

        assertThat(stats.country()).isEqualTo("India");
        assertThat(stats.currency()).isEqualTo("INR");
        assertThat(stats.headcount()).isEqualTo(3);
        assertThat(stats.minSalary()).isEqualByComparingTo("600000");
        assertThat(stats.maxSalary()).isEqualByComparingTo("1800000");
        assertThat(stats.averageSalary()).isEqualByComparingTo("1200000.46");
    }

    @Test
    void listsSalaryStatsForEveryCountry() {
        when(repository.statsByCountry()).thenReturn(List.of(
                new CountryRow("Germany", "EUR", 1L, "70000.00", "70000.00", 70000.0),
                new CountryRow("India", "INR", 3L, "600000.00", "1800000.00", 1200000.0)));

        List<CountrySalaryStats> stats = service.salaryStatsByCountry();

        assertThat(stats).extracting(CountrySalaryStats::country).containsExactly("Germany", "India");
    }

    @Test
    void failsWhenACountryHasNoEmployees() {
        when(repository.statsForCountry("Atlantis")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salaryStatsForCountry("Atlantis"))
                .isInstanceOf(NoSalaryDataException.class)
                .hasMessageContaining("Atlantis");
    }

    @Test
    void returnsTheAverageSalaryForAJobTitleInACountry() {
        when(repository.jobTitleStatsInCountry("India", "Software Engineer")).thenReturn(Optional.of(
                new JobRow("Software Engineer", "INR", 2L, 1500000.0)));

        JobTitleSalaryStats stats = service.averageSalaryForJobTitle("India", "Software Engineer");

        assertThat(stats.country()).isEqualTo("India");
        assertThat(stats.jobTitle()).isEqualTo("Software Engineer");
        assertThat(stats.currency()).isEqualTo("INR");
        assertThat(stats.headcount()).isEqualTo(2);
        assertThat(stats.averageSalary()).isEqualByComparingTo("1500000");
    }

    @Test
    void failsWhenTheJobTitleDoesNotExistInTheCountry() {
        when(repository.jobTitleStatsInCountry("Germany", "Data Analyst")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.averageSalaryForJobTitle("Germany", "Data Analyst"))
                .isInstanceOf(NoSalaryDataException.class)
                .hasMessageContaining("Data Analyst")
                .hasMessageContaining("Germany");
    }

    @Test
    void splitsTheSalaryRangeIntoEqualBands() {
        when(repository.statsForCountry("India")).thenReturn(Optional.of(
                new CountryRow("India", "INR", 10L, "100.00", "500.00", 300.0)));
        when(repository.countByCountryAndSalaryGreaterThanEqualAndSalaryLessThan(
                "India", new BigDecimal("100.00"), new BigDecimal("200.00"))).thenReturn(1L);
        when(repository.countByCountryAndSalaryGreaterThanEqualAndSalaryLessThan(
                "India", new BigDecimal("200.00"), new BigDecimal("300.00"))).thenReturn(2L);
        when(repository.countByCountryAndSalaryGreaterThanEqualAndSalaryLessThan(
                "India", new BigDecimal("300.00"), new BigDecimal("400.00"))).thenReturn(3L);
        when(repository.countByCountryAndSalaryBetween(
                "India", new BigDecimal("400.00"), new BigDecimal("500.00"))).thenReturn(4L);

        SalaryDistribution distribution = service.salaryDistribution("India", 4);

        assertThat(distribution.country()).isEqualTo("India");
        assertThat(distribution.currency()).isEqualTo("INR");
        assertThat(distribution.bands()).extracting(SalaryBand::headcount).containsExactly(1L, 2L, 3L, 4L);
        assertThat(distribution.bands().get(0).from()).isEqualByComparingTo("100");
        assertThat(distribution.bands().get(3).to()).isEqualByComparingTo("500");
    }

    @Test
    void putsEveryoneInOneBandWhenAllSalariesAreEqual() {
        when(repository.statsForCountry("India")).thenReturn(Optional.of(
                new CountryRow("India", "INR", 4L, "50000.00", "50000.00", 50000.0)));

        SalaryDistribution distribution = service.salaryDistribution("India", 5);

        assertThat(distribution.bands()).hasSize(1);
        assertThat(distribution.bands().get(0).headcount()).isEqualTo(4);
        verify(repository, never()).countByCountryAndSalaryBetween(any(), any(), any());
    }

    @Test
    void failsToDistributeSalariesForACountryWithoutEmployees() {
        when(repository.statsForCountry("Atlantis")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.salaryDistribution("Atlantis", 5))
                .isInstanceOf(NoSalaryDataException.class);
    }

    @Test
    void rejectsTooFewBands() {
        assertThatThrownBy(() -> service.salaryDistribution("India", 0))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsTooManyBands() {
        assertThatThrownBy(() -> service.salaryDistribution("India", InsightsService.MAX_BANDS + 1))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(repository);
    }

    @Test
    void returnsTheValuesForTheFilterDropdowns() {
        when(repository.distinctCountries()).thenReturn(List.of("Germany", "India"));
        when(repository.distinctDepartments()).thenReturn(List.of("Analytics", "Engineering"));
        when(repository.distinctJobTitles()).thenReturn(List.of("Data Analyst", "Software Engineer"));

        FilterOptions options = service.filterOptions();

        assertThat(options.countries()).containsExactly("Germany", "India");
        assertThat(options.departments()).containsExactly("Analytics", "Engineering");
        assertThat(options.jobTitles()).containsExactly("Data Analyst", "Software Engineer");
    }

    private record CountryRow(String country, String currency, Long headcount,
                              String minSalary, String maxSalary, Double averageSalary)
            implements SalaryStatsRow {

        @Override
        public String getCountry() {
            return country;
        }

        @Override
        public String getCurrency() {
            return currency;
        }

        @Override
        public Number getHeadcount() {
            return headcount;
        }

        @Override
        public Number getMinSalary() {
            return new BigDecimal(minSalary);
        }

        @Override
        public Number getMaxSalary() {
            return new BigDecimal(maxSalary);
        }

        @Override
        public Number getAverageSalary() {
            return averageSalary;
        }
    }

    private record JobRow(String jobTitle, String currency, Long headcount, Double averageSalary)
            implements JobTitleStatsRow {

        @Override
        public String getJobTitle() {
            return jobTitle;
        }

        @Override
        public String getCurrency() {
            return currency;
        }

        @Override
        public Number getHeadcount() {
            return headcount;
        }

        @Override
        public Number getAverageSalary() {
            return averageSalary;
        }
    }
}
