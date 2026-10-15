package com.acme.salary.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The JSON body for creating or editing an employee, with first-line validation.
 * These annotations catch the common mistakes and return friendly per-field messages;
 * the Employee entity then enforces the same rules again as a last line of defence.
 */
public record EmployeeRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Job title is required") String jobTitle,
        @NotBlank(message = "Department is required") String department,
        @NotBlank(message = "Country is required") String country,
        @NotNull(message = "Salary is required") @Positive(message = "Salary must be greater than zero") BigDecimal salary,
        @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be a 3-letter code") String currency,
        @NotNull(message = "Joining date is required") LocalDate joiningDate) {

    EmployeeDetails toDetails() {
        return new EmployeeDetails(fullName, email, jobTitle, department, country,
                salary, currency, joiningDate);
    }
}
