package com.acme.salary.insights;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private final InsightsService service;

    public InsightsController(InsightsService service) {
        this.service = service;
    }

    @GetMapping("/countries")
    public List<CountrySalaryStats> countries() {
        return service.salaryStatsByCountry();
    }

    @GetMapping("/countries/{country}")
    public CountrySalaryStats country(@PathVariable String country) {
        return service.salaryStatsForCountry(country);
    }

    @GetMapping("/countries/{country}/distribution")
    public SalaryDistribution distribution(@PathVariable String country,
                                           @RequestParam(defaultValue = "5") int bands) {
        return service.salaryDistribution(country, bands);
    }

    @GetMapping("/job-title-average")
    public JobTitleSalaryStats jobTitleAverage(@RequestParam String country,
                                               @RequestParam String jobTitle) {
        return service.averageSalaryForJobTitle(country, jobTitle);
    }

    @GetMapping("/filter-options")
    public FilterOptions filterOptions() {
        return service.filterOptions();
    }
}
