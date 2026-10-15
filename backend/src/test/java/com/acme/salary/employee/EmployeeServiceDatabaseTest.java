package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Runs the service against a real database, because the duplicate-email rule on edit
 * depends on how the database and the persistence layer behave together.
 */
@DataJpaTest
@Import(EmployeeService.class)
class EmployeeServiceDatabaseTest {

    @Autowired
    private EmployeeService service;

    @Test
    void refusesToChangeAnEmployeeToAnEmailThatAnotherEmployeeUses() {
        service.create(details("asha@acme.com", "1800000"));
        Employee rohan = service.create(details("rohan@acme.com", "1200000"));

        assertThatThrownBy(() -> service.update(rohan.getId(), details("ASHA@acme.com", "1200000")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void letsAnEmployeeKeepTheirOwnEmailWhileChangingOtherDetails() {
        Employee asha = service.create(details("asha@acme.com", "1800000"));

        Employee updated = service.update(asha.getId(), details("asha@acme.com", "2000000"));

        assertThat(updated.getSalary()).isEqualByComparingTo("2000000");
    }

    private EmployeeDetails details(String email, String salary) {
        return new EmployeeDetails("Asha Patel", email, "Software Engineer", "Engineering",
                "India", new BigDecimal(salary), "INR", LocalDate.of(2022, 4, 1));
    }
}
