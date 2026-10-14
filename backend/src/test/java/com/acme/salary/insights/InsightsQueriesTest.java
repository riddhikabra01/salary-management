package com.acme.salary.insights;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class InsightsQueriesTest {

    @Autowired
    private InsightsRepository insights;

    @Autowired
    private EmployeeRepository employees;

    @BeforeEach
    void addEmployees() {
        employees.saveAll(List.of(
                employee("Asha Patel", "asha@acme.com", "Software Engineer", "Engineering", "India", "1800000", "INR"),
                employee("Vikram Rao", "vikram@acme.com", "Software Engineer", "Engineering", "India", "1200000", "INR"),
                employee("Rohan Mehta", "rohan@acme.com", "Data Analyst", "Analytics", "India", "600000", "INR"),
                employee("Lena Fischer", "lena@acme.com", "Software Engineer", "Engineering", "Germany", "70000", "EUR")));
    }

    @Test
    void calculatesSalaryStatsForACountry() {
        SalaryStatsRow stats = insights.statsForCountry("India").orElseThrow();

        assertThat(stats.getCountry()).isEqualTo("India");
        assertThat(stats.getCurrency()).isEqualTo("INR");
        assertThat(stats.getHeadcount().longValue()).isEqualTo(3);
        assertThat(decimal(stats.getMinSalary())).isEqualByComparingTo("600000");
        assertThat(decimal(stats.getMaxSalary())).isEqualByComparingTo("1800000");
        assertThat(decimal(stats.getAverageSalary())).isEqualByComparingTo("1200000");
    }

    @Test
    void groupsSalaryStatsByCountry() {
        List<SalaryStatsRow> stats = insights.statsByCountry();

        assertThat(stats).extracting(SalaryStatsRow::getCountry).containsExactly("Germany", "India");
        assertThat(stats.get(0).getHeadcount().longValue()).isEqualTo(1);
    }

    @Test
    void returnsNothingForACountryWithoutEmployees() {
        assertThat(insights.statsForCountry("France")).isEmpty();
    }

    @Test
    void calculatesTheAverageForAJobTitleInACountry() {
        JobTitleStatsRow india = insights.jobTitleStatsInCountry("India", "Software Engineer").orElseThrow();
        JobTitleStatsRow germany = insights.jobTitleStatsInCountry("Germany", "Software Engineer").orElseThrow();

        assertThat(india.getHeadcount().longValue()).isEqualTo(2);
        assertThat(decimal(india.getAverageSalary())).isEqualByComparingTo("1500000");
        assertThat(germany.getHeadcount().longValue()).isEqualTo(1);
        assertThat(decimal(germany.getAverageSalary())).isEqualByComparingTo("70000");
    }

    @Test
    void returnsNothingWhenTheJobTitleDoesNotExistInThatCountry() {
        assertThat(insights.jobTitleStatsInCountry("Germany", "Data Analyst")).isEmpty();
    }

    @Test
    void countsEmployeesInASalaryRange() {
        BigDecimal from = new BigDecimal("600000");
        BigDecimal to = new BigDecimal("1200000");

        long belowUpperLimit = insights.countByCountryAndSalaryGreaterThanEqualAndSalaryLessThan("India", from, to);
        long includingUpperLimit = insights.countByCountryAndSalaryBetween("India", from, to);

        assertThat(belowUpperLimit).isEqualTo(1);
        assertThat(includingUpperLimit).isEqualTo(2);
    }

    @Test
    void listsDistinctFilterValuesInOrder() {
        assertThat(insights.distinctCountries()).containsExactly("Germany", "India");
        assertThat(insights.distinctDepartments()).containsExactly("Analytics", "Engineering");
        assertThat(insights.distinctJobTitles()).containsExactly("Data Analyst", "Software Engineer");
    }

    private static BigDecimal decimal(Number value) {
        return new BigDecimal(value.toString());
    }

    private Employee employee(String name, String email, String jobTitle, String department,
                              String country, String salary, String currency) {
        return new Employee(name, email, jobTitle, department, country,
                new BigDecimal(salary), currency, LocalDate.of(2022, 4, 1));
    }
}
