package com.acme.salary.employee;

import org.springframework.data.jpa.domain.Specification;

final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    static Specification<Employee> matching(EmployeeFilter filter) {
        return Specification.where(nameOrEmailContains(filter.search()))
                .and(fieldEquals("country", filter.country()))
                .and(fieldEquals("department", filter.department()))
                .and(fieldEquals("jobTitle", filter.jobTitle()));
    }

    private static Specification<Employee> nameOrEmailContains(String term) {
        if (isBlank(term)) {
            return null;
        }
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("fullName")), pattern),
                builder.like(builder.lower(root.get("email")), pattern));
    }

    private static Specification<Employee> fieldEquals(String field, String value) {
        if (isBlank(value)) {
            return null;
        }
        String exact = value.trim();
        return (root, query, builder) -> builder.equal(root.get(field), exact);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
