package com.acme.salary.insights;

// Shape of one row coming back from the job-title query (Spring fills it from the "as ..." aliases).
public interface JobTitleStatsRow {

    String getJobTitle();

    String getCurrency();

    Number getHeadcount();

    Number getAverageSalary();
}
