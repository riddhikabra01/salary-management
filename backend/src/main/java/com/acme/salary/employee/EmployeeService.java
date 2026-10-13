package com.acme.salary.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return repository.findAll(EmployeeSpecifications.matching(filter), pageable);
    }

    @Transactional
    public Employee update(Long id, EmployeeDetails details) {
        Employee employee = getById(id);

        employee.updateDetails(
                details.fullName(), details.email(), details.jobTitle(), details.department(),
                details.country(), details.salary(), details.currency(), details.joiningDate());

        if (repository.existsByEmailIgnoreCaseAndIdNot(employee.getEmail(), id)) {
            throw new DuplicateEmailException(employee.getEmail());
        }
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
