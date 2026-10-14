package com.acme.salary.insights;

public class NoSalaryDataException extends RuntimeException {

    public NoSalaryDataException(String message) {
        super(message);
    }
}
