package com.acme.salary.employee;

// Thrown when an id does not exist. Mapped to HTTP 404.
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("Employee not found with id " + id);
    }
}