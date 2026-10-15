package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

// What the API sends back. A separate class from the entity so the database model can change
// without breaking the API, and so nothing internal leaks out by accident.
public record EmployeeResponse(
        Long id,
        String fullName,
        String email,
        String jobTitle,
        String department,
        String country,
        BigDecimal salary,
        String currency,
        LocalDate joiningDate) {

    static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(), employee.getFullName(), employee.getEmail(),
                employee.getJobTitle(), employee.getDepartment(), employee.getCountry(),
                employee.getSalary(), employee.getCurrency(), employee.getJoiningDate());
    }
}
