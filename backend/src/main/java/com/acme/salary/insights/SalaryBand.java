package com.acme.salary.insights;

import java.math.BigDecimal;

// One bar of the salary distribution: employees earning from..to.
public record SalaryBand(BigDecimal from, BigDecimal to, long headcount) {
}
