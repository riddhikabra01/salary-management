package com.acme.salary.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for employees. Controllers call this; it talks to the repository.
 * Every method is transactional, and reads are marked read-only so the database can skip extra work.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Employee create(EmployeeDetails details) {
        Employee employee = new Employee(
                details.fullName(), details.email(), details.jobTitle(), details.department(),
                details.country(), details.salary(), details.currency(), details.joiningDate());

        // Emails are unique ignoring case ("A@x.com" equals "a@x.com"). Check first so the caller
        // gets a clear 409 rather than a raw database constraint error.
        if (repository.existsByEmailIgnoreCase(employee.getEmail())) {
            throw new DuplicateEmailException(employee.getEmail());
        }
        return repository.save(employee);
    }

    @Transactional(readOnly = true)
    public Employee getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Employee> search(EmployeeFilter filter, Pageable pageable) {
        // The specification builds the WHERE clause from whichever filters were supplied;
        // the pageable carries paging and sorting.
        return repository.findAll(EmployeeSpecifications.matching(filter), pageable);
    }

    @Transactional
    public Employee update(Long id, EmployeeDetails details) {
        Employee employee = getById(id);

        // Check for a clash before changing the entity. If the entity were changed first, the database
        // would reject the duplicate while this query flushes it, and the caller would get a 500
        // instead of a clear "email already in use" answer.
        String newEmail = details.email() == null ? "" : details.email().trim();
        if (repository.existsByEmailIgnoreCaseAndIdNot(newEmail, id)) {
            throw new DuplicateEmailException(newEmail);
        }

        // No explicit save() needed: the entity is managed inside this transaction,
        // so changes are written when it commits.
        employee.updateDetails(
                details.fullName(), details.email(), details.jobTitle(), details.department(),
                details.country(), details.salary(), details.currency(), details.joiningDate());
        return employee;
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
