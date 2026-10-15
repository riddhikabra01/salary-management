package com.acme.salary.insights;

import java.math.BigDecimal;

// Headcount and min / max / average salary for one country, in that country's own currency.
public record CountrySalaryStats(
        String country,
        String currency,
        long headcount,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        BigDecimal averageSalary) {
}
