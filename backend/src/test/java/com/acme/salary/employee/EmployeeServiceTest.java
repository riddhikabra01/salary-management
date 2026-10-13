package com.acme.salary.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository repository;

    @InjectMocks
    private EmployeeService service;

    @Test
    void createsAnEmployee() {
        when(repository.existsByEmailIgnoreCase("asha.patel@acme.com")).thenReturn(false);
        when(repository.save(any(Employee.class))).thenAnswer(call -> call.getArgument(0));

        Employee created = service.create(ashaDetails());

        assertThat(created.getFullName()).isEqualTo("Asha Patel");
        assertThat(created.getSalary()).isEqualByComparingTo("1800000");
        verify(repository).save(created);
    }

    @Test
    void refusesToCreateAnEmployeeWithATakenEmail() {
        when(repository.existsByEmailIgnoreCase("asha.patel@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(ashaDetails()))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("asha.patel@acme.com");

        verify(repository, never()).save(any(Employee.class));
    }

    @Test
    void returnsAnEmployeeById() {
        Employee asha = anEmployee();
        when(repository.findById(1L)).thenReturn(Optional.of(asha));

        assertThat(service.getById(1L)).isSameAs(asha);
    }

    @Test
    void failsWhenTheEmployeeDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updatesAnEmployee() {
        Employee asha = anEmployee();
        when(repository.findById(1L)).thenReturn(Optional.of(asha));
        when(repository.existsByEmailIgnoreCaseAndIdNot("asha.p@acme.com", 1L)).thenReturn(false);

        Employee updated = service.update(1L, new EmployeeDetails(
                "Asha Patel", "asha.p@acme.com", "Senior Engineer", "Engineering",
                "India", new BigDecimal("2400000"), "INR", LocalDate.of(2022, 4, 1)));

        assertThat(updated.getJobTitle()).isEqualTo("Senior Engineer");
        assertThat(updated.getEmail()).isEqualTo("asha.p@acme.com");
        assertThat(updated.getSalary()).isEqualByComparingTo("2400000");
    }

    @Test
    void refusesToUpdateToAnEmailUsedByAnotherEmployee() {
        when(repository.findById(1L)).thenReturn(Optional.of(anEmployee()));
        when(repository.existsByEmailIgnoreCaseAndIdNot("taken@acme.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new EmployeeDetails(
                "Asha Patel", "taken@acme.com", "Software Engineer", "Engineering",
                "India", new BigDecimal("1800000"), "INR", LocalDate.of(2022, 4, 1))))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void failsToUpdateAMissingEmployee() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, ashaDetails()))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void deletesAnEmployee() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void failsToDeleteAMissingEmployee() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(repository, never()).deleteById(any(Long.class));
    }

    private EmployeeDetails ashaDetails() {
        return new EmployeeDetails(
                "Asha Patel", "asha.patel@acme.com", "Software Engineer", "Engineering",
                "India", new BigDecimal("1800000"), "INR", LocalDate.of(2022, 4, 1));
    }

    private Employee anEmployee() {
        return new Employee(
                "Asha Patel", "asha.patel@acme.com", "Software Engineer", "Engineering",
                "India", new BigDecimal("1800000"), "INR", LocalDate.of(2022, 4, 1));
    }
}