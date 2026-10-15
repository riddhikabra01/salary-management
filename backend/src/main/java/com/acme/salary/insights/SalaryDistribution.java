package com.acme.salary.insights;

import java.util.List;

// A country's salary range cut into bands, ready to draw as a chart.
public record SalaryDistribution(String country, String currency, List<SalaryBand> bands) {
}
