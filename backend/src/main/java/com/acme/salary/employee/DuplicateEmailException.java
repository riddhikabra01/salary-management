package com.acme.salary.employee;

// Thrown when an email is already taken by another employee. Mapped to HTTP 409.
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An employee with email " + email + " already exists");
    }
}