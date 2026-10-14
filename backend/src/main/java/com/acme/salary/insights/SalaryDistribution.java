package com.acme.salary.insights;

import java.util.List;

public record SalaryDistribution(String country, String currency, List<SalaryBand> bands) {
}
