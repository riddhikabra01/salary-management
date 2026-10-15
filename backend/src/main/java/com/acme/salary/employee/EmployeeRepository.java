package com.acme.salary.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Database access for employees. The two exists-methods power the duplicate-email check;
 * the second ignores the employee being edited, so saving someone with their own email is fine.
 * JpaSpecificationExecutor is what lets the list screen combine any mix of filters.
 */
public interface EmployeeRepository
        extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}
