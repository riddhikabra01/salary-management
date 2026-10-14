package com.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.salary.employee.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EmployeeGeneratorTest {

    private static final long SEED = 42L;
    private static final int SAMPLE_SIZE = 5000;

    private final List<Employee> employees = new EmployeeGenerator(SEED).generate(SAMPLE_SIZE);

    @Test
    void generatesTheRequestedNumberOfEmployees() {
        assertThat(new EmployeeGenerator(1L).generate(300)).hasSize(300);
    }

    @Test
    void givesEveryEmployeeAUniqueEmail() {
        long distinctEmails = employees.stream().map(Employee::getEmail).distinct().count();

        assertThat(distinctEmails).isEqualTo(employees.size());
    }

    @Test
    void producesTheSameDataForTheSameSeed() {
        List<Employee> again = new EmployeeGenerator(SEED).generate(SAMPLE_SIZE);

        assertThat(again).extracting(Employee::getEmail)
                .containsExactlyElementsOf(employees.stream().map(Employee::getEmail).toList());
        assertThat(again).extracting(Employee::getSalary)
                .containsExactlyElementsOf(employees.stream().map(Employee::getSalary).toList());
    }

    @Test
    void coversManyCountriesAndJobTitles() {
        long countries = employees.stream().map(Employee::getCountry).distinct().count();
        long jobTitles = employees.stream().map(Employee::getJobTitle).distinct().count();

        assertThat(countries).isGreaterThanOrEqualTo(8);
        assertThat(jobTitles).isGreaterThanOrEqualTo(15);
    }

    @Test
    void usesOneCurrencyPerCountry() {
        Map<String, Set<String>> currenciesByCountry = employees.stream().collect(
                Collectors.groupingBy(Employee::getCountry,
                        Collectors.mapping(Employee::getCurrency, Collectors.toSet())));

        assertThat(currenciesByCountry.values()).allSatisfy(currencies -> assertThat(currencies).hasSize(1));
    }

    @Test
    void keepsEachJobTitleInOneDepartment() {
        Map<String, Set<String>> departmentsByTitle = employees.stream().collect(
                Collectors.groupingBy(Employee::getJobTitle,
                        Collectors.mapping(Employee::getDepartment, Collectors.toSet())));

        assertThat(departmentsByTitle.values()).allSatisfy(departments -> assertThat(departments).hasSize(1));
    }

    @Test
    void paysSeniorRolesMoreThanJuniorRolesInTheSameCountry() {
        BigDecimal lowestManagerPay = salariesOf("Engineering Manager", "United States").stream()
                .min(Comparator.naturalOrder()).orElseThrow();
        BigDecimal highestSupportPay = salariesOf("Support Specialist", "United States").stream()
                .max(Comparator.naturalOrder()).orElseThrow();

        assertThat(lowestManagerPay).isGreaterThan(highestSupportPay);
    }

    @Test
    void keepsJoiningDatesInsideTheCompanyHistory() {
        LocalDate earliest = LocalDate.of(2012, 1, 1);
        LocalDate latest = LocalDate.of(2026, 9, 30);

        assertThat(employees).allSatisfy(employee ->
                assertThat(employee.getJoiningDate()).isBetween(earliest, latest));
    }

    @Test
    void givesEmployeesNamesThatFitTheirCountry() {
        assertThat(countriesWhereName(name -> name.startsWith("Ananya "))).containsExactly("India");
        assertThat(countriesWhereName(name -> name.endsWith(" Patel"))).containsExactly("India");
        assertThat(countriesWhereName(name -> name.startsWith("Hiroshi "))).containsExactly("Japan");
        assertThat(countriesWhereName(name -> name.endsWith(" Tanaka"))).containsExactly("Japan");
        assertThat(countriesWhereName(name -> name.startsWith("Lukas "))).containsExactly("Germany");
        assertThat(countriesWhereName(name -> name.endsWith(" Mueller"))).containsExactly("Germany");
        assertThat(countriesWhereName(name -> name.endsWith(" Silva"))).containsExactly("Brazil");
    }

    @Test
    void neverMixesJapaneseFirstNamesWithWesternSurnames() {
        Set<String> japaneseSurnames = Set.of("Sato", "Suzuki", "Takahashi", "Tanaka", "Watanabe", "Ito",
                "Yamamoto", "Nakamura", "Kobayashi", "Kato", "Yoshida", "Yamada", "Sasaki", "Matsumoto",
                "Inoue", "Kimura");

        assertThat(employees)
                .filteredOn(employee -> employee.getCountry().equals("Japan"))
                .allSatisfy(employee -> assertThat(japaneseSurnames)
                        .contains(employee.getFullName().substring(employee.getFullName().lastIndexOf(' ') + 1)));
    }

    @Test
    void writesEmailsWithoutSpaces() {
        assertThat(employees).allSatisfy(employee -> assertThat(employee.getEmail()).doesNotContain(" "));
    }

    private Set<String> countriesWhereName(Predicate<String> matches) {
        return employees.stream()
                .filter(employee -> matches.test(employee.getFullName()))
                .map(Employee::getCountry)
                .collect(Collectors.toSet());
    }

    private List<BigDecimal> salariesOf(String jobTitle, String country) {
        return employees.stream()
                .filter(employee -> employee.getJobTitle().equals(jobTitle))
                .filter(employee -> employee.getCountry().equals(country))
                .map(Employee::getSalary)
                .toList();
    }
}
