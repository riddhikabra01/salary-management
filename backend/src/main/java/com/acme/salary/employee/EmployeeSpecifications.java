package com.acme.salary.employee;

import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the list query's WHERE clause from the filter. Each condition is added only when the user
 * actually supplied a value, so any combination of filters works.
 */
final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    static Specification<Employee> matching(EmployeeFilter filter) {
        // Returning null from a helper means "no condition", and Specification simply skips it.
        return Specification.where(nameOrEmailContains(filter.search()))
                .and(fieldEquals("country", filter.country()))
                .and(fieldEquals("department", filter.department()))
                .and(fieldEquals("jobTitle", filter.jobTitle()));
    }

    private static Specification<Employee> nameOrEmailContains(String term) {
        if (isBlank(term)) {
            return null;
        }
        // Case-insensitive "contains" match on name OR email.
        String pattern = "%" + term.trim().toLowerCase() + "%";
        return (root, query, builder) -> builder.or(
                builder.like(builder.lower(root.get("fullName")), pattern),
                builder.like(builder.lower(root.get("email")), pattern));
    }

    private static Specification<Employee> fieldEquals(String field, String value) {
        if (isBlank(value)) {
            return null;
        }
        // Dropdown filters use exact matches (they come from known values), which can use the indexes.
        String exact = value.trim();
        return (root, query, builder) -> builder.equal(root.get(field), exact);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
