package com.acme.salary.insights;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsightsController.class)
class InsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InsightsService service;

    @Test
    void listsSalaryStatsForEveryCountry() throws Exception {
        when(service.salaryStatsByCountry()).thenReturn(List.of(indiaStats()));

        mockMvc.perform(get("/api/insights/countries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].country").value("India"))
                .andExpect(jsonPath("$[0].headcount").value(3));
    }

    @Test
    void returnsSalaryStatsForACountry() throws Exception {
        when(service.salaryStatsForCountry("India")).thenReturn(indiaStats());

        mockMvc.perform(get("/api/insights/countries/India"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.minSalary").value(600000.0))
                .andExpect(jsonPath("$.maxSalary").value(1800000.0))
                .andExpect(jsonPath("$.averageSalary").value(1200000.0));
    }

    @Test
    void returnsNotFoundWhenACountryHasNoEmployees() throws Exception {
        when(service.salaryStatsForCountry("Atlantis"))
                .thenThrow(new NoSalaryDataException("No employees found in Atlantis"));

        mockMvc.perform(get("/api/insights/countries/Atlantis"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No employees found in Atlantis"));
    }

    @Test
    void returnsTheSalaryDistributionWithFiveBandsByDefault() throws Exception {
        when(service.salaryDistribution("India", 5)).thenReturn(new SalaryDistribution("India", "INR",
                List.of(new SalaryBand(new BigDecimal("600000.00"), new BigDecimal("1200000.00"), 2),
                        new SalaryBand(new BigDecimal("1200000.00"), new BigDecimal("1800000.00"), 1))));

        mockMvc.perform(get("/api/insights/countries/India/distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.bands[0].headcount").value(2))
                .andExpect(jsonPath("$.bands[1].to").value(1800000.0));

        verify(service).salaryDistribution("India", 5);
    }

    @Test
    void usesTheRequestedNumberOfBands() throws Exception {
        when(service.salaryDistribution("India", 8))
                .thenReturn(new SalaryDistribution("India", "INR", List.of()));

        mockMvc.perform(get("/api/insights/countries/India/distribution").param("bands", "8"))
                .andExpect(status().isOk());

        verify(service).salaryDistribution("India", 8);
    }

    @Test
    void rejectsAnInvalidNumberOfBands() throws Exception {
        when(service.salaryDistribution("India", 0))
                .thenThrow(new IllegalArgumentException("Bands must be between 1 and 20"));

        mockMvc.perform(get("/api/insights/countries/India/distribution").param("bands", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsTheAverageSalaryForAJobTitleInACountry() throws Exception {
        when(service.averageSalaryForJobTitle("India", "Software Engineer")).thenReturn(
                new JobTitleSalaryStats("India", "Software Engineer", "INR", 2, new BigDecimal("1500000.00")));

        mockMvc.perform(get("/api/insights/job-title-average")
                        .param("country", "India")
                        .param("jobTitle", "Software Engineer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("Software Engineer"))
                .andExpect(jsonPath("$.headcount").value(2))
                .andExpect(jsonPath("$.averageSalary").value(1500000.0));
    }

    @Test
    void requiresBothACountryAndAJobTitle() throws Exception {
        mockMvc.perform(get("/api/insights/job-title-average").param("country", "India"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsTheValuesForTheFilterDropdowns() throws Exception {
        when(service.filterOptions()).thenReturn(new FilterOptions(
                List.of("Germany", "India"), List.of("Engineering"), List.of("Software Engineer")));

        mockMvc.perform(get("/api/insights/filter-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countries[1]").value("India"))
                .andExpect(jsonPath("$.departments[0]").value("Engineering"))
                .andExpect(jsonPath("$.jobTitles[0]").value("Software Engineer"));
    }

    private CountrySalaryStats indiaStats() {
        return new CountrySalaryStats("India", "INR", 3,
                new BigDecimal("600000.00"), new BigDecimal("1800000.00"), new BigDecimal("1200000.00"));
    }
}
