package com.acme.salary.insights;

// Shape of one row from the per-country statistics query (Spring fills it from the "as ..." aliases).
public interface SalaryStatsRow {

    String getCountry();

    String getCurrency();

    Number getHeadcount();

    Number getMinSalary();

    Number getMaxSalary();

    Number getAverageSalary();
}
