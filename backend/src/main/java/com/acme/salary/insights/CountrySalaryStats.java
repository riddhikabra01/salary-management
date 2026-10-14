package com.acme.salary.insights;

import java.math.BigDecimal;

public record CountrySalaryStats(
        String country,
        String currency,
        long headcount,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        BigDecimal averageSalary) {
}
