package com.acme.salary.seed;

import com.acme.salary.employee.Employee;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class EmployeeSeeder {

    private static final int BATCH_SIZE = 1000;
    private static final long RANDOM_SEED = 2026L;
    private static final String INSERT_EMPLOYEE = """
            insert into employees
                (full_name, email, job_title, department, country, salary, currency, joining_date)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transaction;

    public EmployeeSeeder(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Loads generated employees into an empty table using batched inserts.
     *
     * @return how many employees were inserted, or 0 when the table already had data
     */
    public int seedIfEmpty(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Seed count must be at least 1");
        }
        if (existingEmployees() > 0) {
            return 0;
        }

        List<Employee> employees = new EmployeeGenerator(RANDOM_SEED).generate(count);
        transaction.executeWithoutResult(status ->
                jdbcTemplate.batchUpdate(INSERT_EMPLOYEE, employees, BATCH_SIZE, EmployeeSeeder::bind));
        return employees.size();
    }

    private int existingEmployees() {
        Integer existing = jdbcTemplate.queryForObject("select count(*) from employees", Integer.class);
        return existing == null ? 0 : existing;
    }

    private static void bind(PreparedStatement statement, Employee employee) throws SQLException {
        statement.setString(1, employee.getFullName());
        statement.setString(2, employee.getEmail());
        statement.setString(3, employee.getJobTitle());
        statement.setString(4, employee.getDepartment());
        statement.setString(5, employee.getCountry());
        statement.setBigDecimal(6, employee.getSalary());
        statement.setString(7, employee.getCurrency());
        statement.setDate(8, Date.valueOf(employee.getJoiningDate()));
    }
}
