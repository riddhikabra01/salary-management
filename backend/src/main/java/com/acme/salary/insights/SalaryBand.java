package com.acme.salary.insights;

import java.math.BigDecimal;

public record SalaryBand(BigDecimal from, BigDecimal to, long headcount) {
}
