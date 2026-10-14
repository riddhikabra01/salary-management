package com.acme.salary.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.salary.employee.Employee;
import com.acme.salary.employee.EmployeeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(EmployeeSeeder.class)
class EmployeeSeederTest {

    @Autowired
    private EmployeeSeeder seeder;

    @Autowired
    private EmployeeRepository repository;

    @Test
    void seedsAnEmptyDatabase() {
        int inserted = seeder.seedIfEmpty(250);

        assertThat(inserted).isEqualTo(250);
        assertThat(repository.count()).isEqualTo(250);
    }

    @Test
    void insertsLargeVolumesInSeveralBatches() {
        int inserted = seeder.seedIfEmpty(2500);

        assertThat(inserted).isEqualTo(2500);
        assertThat(repository.count()).isEqualTo(2500);
    }

    @Test
    void leavesExistingDataAlone() {
        repository.save(new Employee("Asha Patel", "asha.patel@acme.com", "Software Engineer",
                "Engineering", "India", new BigDecimal("1800000"), "INR", LocalDate.of(2022, 4, 1)));

        int inserted = seeder.seedIfEmpty(250);

        assertThat(inserted).isZero();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void storesValuesThatTheApplicationCanReadBack() {
        seeder.seedIfEmpty(50);

        assertThat(repository.findAll()).hasSize(50).allSatisfy(employee -> {
            assertThat(employee.getFullName()).isNotBlank();
            assertThat(employee.getEmail()).contains("@acme.com");
            assertThat(employee.getJobTitle()).isNotBlank();
            assertThat(employee.getDepartment()).isNotBlank();
            assertThat(employee.getCountry()).isNotBlank();
            assertThat(employee.getCurrency()).hasSize(3);
            assertThat(employee.getSalary()).isPositive();
            assertThat(employee.getJoiningDate()).isNotNull();
        });
    }

    @Test
    void refusesToSeedFewerThanOneEmployee() {
        assertThatThrownBy(() -> seeder.seedIfEmpty(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
