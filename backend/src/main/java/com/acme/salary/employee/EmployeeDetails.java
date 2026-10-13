package com.acme.salary.employee;

import java.math.BigDecimal;
import java.time.LocalDate;

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