package com.acme.salary.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_employee_country", columnList = "country"),
        @Index(name = "idx_employee_department", columnList = "department"),
        @Index(name = "idx_employee_job_title", columnList = "jobTitle")
})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String jobTitle;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal salary;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private LocalDate joiningDate;

    protected Employee() {
        // required by JPA
    }

    public Employee(String fullName, String email, String jobTitle, String department,
                    String country, BigDecimal salary, String currency, LocalDate joiningDate) {
        this.fullName = requireText(fullName, "Full name is required");
        this.email = requireEmail(email);
        this.jobTitle = requireText(jobTitle, "Job title is required");
        this.department = requireText(department, "Department is required");
        this.country = requireText(country, "Country is required");
        this.salary = requirePositive(salary);
        this.currency = requireText(currency, "Currency is required");
        this.joiningDate = joiningDate;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String requireEmail(String value) {
        String email = requireText(value, "A valid email is required");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("A valid email is required");
        }
        return email;
    }

    private static BigDecimal requirePositive(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException("Salary must be greater than zero");
        }
        return value;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public String getCountry() { return country; }
    public BigDecimal getSalary() { return salary; }
    public String getCurrency() { return currency; }
    public LocalDate getJoiningDate() { return joiningDate; }
}
