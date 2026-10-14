package com.acme.salary.insights;

import com.acme.salary.employee.Employee;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InsightsRepository extends Repository<Employee, Long> {

    @Query("""
            select e.country as country, min(e.currency) as currency, count(e) as headcount,
                   min(e.salary) as minSalary, max(e.salary) as maxSalary,
                   avg(e.salary) as averageSalary
            from Employee e
            group by e.country
            order by e.country
            """)
    List<SalaryStatsRow> statsByCountry();

    @Query("""
            select e.country as country, min(e.currency) as currency, count(e) as headcount,
                   min(e.salary) as minSalary, max(e.salary) as maxSalary,
                   avg(e.salary) as averageSalary
            from Employee e
            where e.country = :country
            group by e.country
            """)
    Optional<SalaryStatsRow> statsForCountry(@Param("country") String country);

    @Query("""
            select e.jobTitle as jobTitle, min(e.currency) as currency, count(e) as headcount,
                   avg(e.salary) as averageSalary
            from Employee e
            where e.country = :country and e.jobTitle = :jobTitle
            group by e.jobTitle
            """)
    Optional<JobTitleStatsRow> jobTitleStatsInCountry(@Param("country") String country,
                                                      @Param("jobTitle") String jobTitle);

    long countByCountryAndSalaryGreaterThanEqualAndSalaryLessThan(
            String country, BigDecimal from, BigDecimal to);

    long countByCountryAndSalaryBetween(String country, BigDecimal from, BigDecimal to);

    @Query("select distinct e.country from Employee e order by e.country")
    List<String> distinctCountries();

    @Query("select distinct e.department from Employee e order by e.department")
    List<String> distinctDepartments();

    @Query("select distinct e.jobTitle from Employee e order by e.jobTitle")
    List<String> distinctJobTitles();
}
