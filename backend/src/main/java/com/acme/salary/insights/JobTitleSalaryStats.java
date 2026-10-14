package com.acme.salary.insights;

import java.math.BigDecimal;

public record JobTitleSalaryStats(
        String country,
        String jobTitle,
        String currency,
        long headcount,
        BigDecimal averageSalary) {
}
