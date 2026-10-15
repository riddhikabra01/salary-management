package com.acme.salary.insights;

import java.math.BigDecimal;

// Average salary and headcount for one job title within one country.
public record JobTitleSalaryStats(
        String country,
        String jobTitle,
        String currency,
        long headcount,
        BigDecimal averageSalary) {
}
