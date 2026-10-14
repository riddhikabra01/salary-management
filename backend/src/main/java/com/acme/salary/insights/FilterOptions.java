package com.acme.salary.insights;

import java.util.List;

public record FilterOptions(List<String> countries, List<String> departments, List<String> jobTitles) {
}
