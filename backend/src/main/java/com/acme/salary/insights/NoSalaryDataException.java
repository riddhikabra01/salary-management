package com.acme.salary.insights;

// Thrown when a country or job title has no employees to report on. Mapped to HTTP 404.
public class NoSalaryDataException extends RuntimeException {

    public NoSalaryDataException(String message) {
        super(message);
    }
}
