package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class EmployeeSearchTest {

    private static final PageRequest FIRST_PAGE_BY_NAME =
            PageRequest.of(0, 10, Sort.by("fullName"));

    @Autowired
    private EmployeeRepository repository;

    @BeforeEach
    void addEmployees() {
        repository.saveAll(List.of(
                employee("Asha Patel", "asha.patel@acme.com", "Software Engineer", "Engineering", "India", "1800000", "INR"),
                employee("Rohan Mehta", "rohan.mehta@acme.com", "Data Analyst", "Analytics", "India", "1200000", "INR"),
                employee("Lena Fischer", "lena.fischer@acme.com", "Software Engineer", "Engineering", "Germany", "70000", "EUR"),
                employee("Tom Baker", "tom.baker@acme.com", "Product Manager", "Product", "United Kingdom", "65000", "GBP")));
    }

    @Test
    void returnsEveryoneWhenNoFilterIsGiven() {
        Page<Employee> result = search(EmployeeFilter.none());

        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void filtersByCountry() {
        Page<Employee> result = search(new EmployeeFilter(null, "India", null, null));

        assertThat(names(result)).containsExactly("Asha Patel", "Rohan Mehta");
    }

    @Test
    void searchesNamesIgnoringCase() {
        Page<Employee> result = search(new EmployeeFilter("LENA", null, null, null));

        assertThat(names(result)).containsExactly("Lena Fischer");
    }

    @Test
    void searchesEmailsIgnoringCase() {
        Page<Employee> result = search(new EmployeeFilter("Rohan.Mehta@", null, null, null));

        assertThat(names(result)).containsExactly("Rohan Mehta");
    }

    @Test
    void combinesFilters() {
        Page<Employee> result = search(new EmployeeFilter(null, "India", null, "Software Engineer"));

        assertThat(names(result)).containsExactly("Asha Patel");
    }

    @Test
    void filtersByDepartment() {
        Page<Employee> result = search(new EmployeeFilter(null, null, "Engineering", null));

        assertThat(names(result)).containsExactly("Asha Patel", "Lena Fischer");
    }

    @Test
    void ignoresBlankFilters() {
        Page<Employee> result = search(new EmployeeFilter("  ", "", " ", null));

        assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void returnsNothingWhenNobodyMatches() {
        Page<Employee> result = search(new EmployeeFilter("zzz", null, null, null));

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void pagesThroughResults() {
        PageRequest secondPage = PageRequest.of(1, 2, Sort.by("fullName"));

        Page<Employee> result = repository.findAll(
                EmployeeSpecifications.matching(EmployeeFilter.none()), secondPage);

        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(names(result)).containsExactly("Rohan Mehta", "Tom Baker");
    }

    private Page<Employee> search(EmployeeFilter filter) {
        return repository.findAll(EmployeeSpecifications.matching(filter), FIRST_PAGE_BY_NAME);
    }

    private List<String> names(Page<Employee> page) {
        return page.getContent().stream().map(Employee::getFullName).toList();
    }

    private Employee employee(String name, String email, String jobTitle, String department,
                              String country, String salary, String currency) {
        return new Employee(name, email, jobTitle, department, country,
                new BigDecimal(salary), currency, LocalDate.of(2022, 4, 1));
    }
}
