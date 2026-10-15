package com.acme.salary.insights;

import java.util.List;

// The distinct values that fill the filter dropdowns, so they always reflect what is really in the data.
public record FilterOptions(List<String> countries, List<String> departments, List<String> jobTitles) {
}
