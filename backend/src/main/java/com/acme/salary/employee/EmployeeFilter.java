package com.acme.salary.employee;

public record EmployeeFilter(String search, String country, String department, String jobTitle) {

    public static EmployeeFilter none() {
        return new EmployeeFilter(null, null, null, null);
    }
}
