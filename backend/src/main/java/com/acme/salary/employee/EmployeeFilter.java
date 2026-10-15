package com.acme.salary.employee;

// The optional search conditions for the list. A null or blank value means "don't filter on this".
public record EmployeeFilter(String search, String country, String department, String jobTitle) {

    public static EmployeeFilter none() {
        return new EmployeeFilter(null, null, null, null);
    }
}
