package com.acme.salary.insights;

public interface SalaryStatsRow {

    String getCountry();

    String getCurrency();

    Number getHeadcount();

    Number getMinSalary();

    Number getMaxSalary();

    Number getAverageSalary();
}
