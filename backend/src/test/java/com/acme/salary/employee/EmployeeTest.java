package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmployeeTest {

    private static final LocalDate JOINED = LocalDate.of(2022, 4, 1);

    @Test
    void createsEmployeeWithValidDetails() {
        Employee employee = new Employee(
                "Asha Patel", "asha.patel@acme.com", "Software Engineer",
                "Engineering", "India", new BigDecimal("1800000"), "INR", JOINED);

        assertThat(employee.getFullName()).isEqualTo("Asha Patel");
        assertThat(employee.getSalary()).isEqualByComparingTo("1800000");
        assertThat(employee.getCurrency()).isEqualTo("INR");
    }

    @Test
    void rejectsZeroSalary() {
        assertThatThrownBy(() -> anEmployeeWithSalary(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Salary must be greater than zero");
    }

    @Test
    void rejectsNegativeSalary() {
        assertThatThrownBy(() -> anEmployeeWithSalary(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Employee(
                " ", "a@acme.com", "Analyst", "Finance", "Germany",
                new BigDecimal("50000"), "EUR", JOINED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Full name is required");
    }

    @Test
    void rejectsMalformedEmail() {
        assertThatThrownBy(() -> new Employee(
                "Lena Fischer", "not-an-email", "Analyst", "Finance", "Germany",
                new BigDecimal("50000"), "EUR", JOINED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid email");
    }

    private Employee anEmployeeWithSalary(BigDecimal salary) {
        return new Employee("Asha Patel", "asha.patel@acme.com", "Software Engineer",
                "Engineering", "India", salary, "INR", JOINED);
    }
}
