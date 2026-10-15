package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

// The plain data needed to create or edit an employee. It keeps the service independent from the
// web layer's request class, so the same rules work no matter where the data came from.
public record EmployeeDetails(
        String fullName,
        String email,
        String jobTitle,
        String department,
        String country,
        BigDecimal salary,
        String currency,
        LocalDate joiningDate) {
}