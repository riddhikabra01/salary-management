package com.acme.salary.seed;

import com.acme.salary.employee.Employee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Produces realistic, repeatable employee data: the same seed always gives the same employees.
 * Salaries are a job's base pay scaled by the country's pay level, in the country's own currency.
 */
public class EmployeeGenerator {

    private static final LocalDate FIRST_JOINING_DATE = LocalDate.of(2012, 1, 1);
    private static final LocalDate LAST_JOINING_DATE = LocalDate.of(2026, 9, 30);
    private static final double LOWEST_PAY_SPREAD = 0.80;
    private static final double PAY_SPREAD_RANGE = 0.45;

    private record Country(String name, String currency, double payFactor, int weight) {
    }

    private record Job(String title, String department, int baseSalary) {
    }

    private static final List<Country> COUNTRIES = List.of(
            new Country("India", "INR", 25.0, 30),
            new Country("United States", "USD", 1.0, 20),
            new Country("United Kingdom", "GBP", 0.67, 8),
            new Country("Germany", "EUR", 0.78, 8),
            new Country("France", "EUR", 0.72, 5),
            new Country("Canada", "CAD", 1.2, 6),
            new Country("Australia", "AUD", 1.4, 5),
            new Country("Singapore", "SGD", 1.2, 5),
            new Country("Brazil", "BRL", 1.5, 5),
            new Country("Japan", "JPY", 105.0, 8));

    private static final int TOTAL_COUNTRY_WEIGHT =
            COUNTRIES.stream().mapToInt(Country::weight).sum();

    private static final List<Job> JOBS = List.of(
            new Job("Software Engineer", "Engineering", 95_000),
            new Job("Senior Software Engineer", "Engineering", 130_000),
            new Job("Engineering Manager", "Engineering", 160_000),
            new Job("QA Engineer", "Engineering", 75_000),
            new Job("DevOps Engineer", "Engineering", 105_000),
            new Job("Data Analyst", "Analytics", 70_000),
            new Job("Data Scientist", "Analytics", 115_000),
            new Job("Product Manager", "Product", 120_000),
            new Job("Product Designer", "Product", 95_000),
            new Job("Sales Executive", "Sales", 60_000),
            new Job("Account Manager", "Sales", 80_000),
            new Job("Marketing Specialist", "Marketing", 65_000),
            new Job("HR Executive", "Human Resources", 55_000),
            new Job("Recruiter", "Human Resources", 58_000),
            new Job("Accountant", "Finance", 62_000),
            new Job("Financial Analyst", "Finance", 80_000),
            new Job("Operations Manager", "Operations", 90_000),
            new Job("Support Specialist", "Customer Support", 45_000));

    private static final List<String> FIRST_NAMES = List.of(
            "Asha", "Rohan", "Vikram", "Priya", "Ananya", "Arjun", "Neha", "Kabir", "Meera", "Rahul",
            "Sophie", "Liam", "Emma", "Noah", "Olivia", "Lucas", "Mia", "Ethan", "Hannah", "Lena",
            "Tom", "Chloe", "Hiroshi", "Yuki", "Kenji", "Sakura", "Mateus", "Camila", "Daniel", "Grace");

    private static final List<String> LAST_NAMES = List.of(
            "Patel", "Mehta", "Rao", "Sharma", "Iyer", "Kapoor", "Nair", "Singh", "Smith", "Johnson",
            "Brown", "Taylor", "Wilson", "Fischer", "Mueller", "Dubois", "Martin", "Tremblay", "Nguyen", "Tan",
            "Lim", "Tanaka", "Sato", "Suzuki", "Silva", "Santos", "Oliveira", "Baker", "Walker", "Clarke");

    private final Random random;

    public EmployeeGenerator(long seed) {
        this.random = new Random(seed);
    }

    public List<Employee> generate(int count) {
        List<Employee> employees = new ArrayList<>(count);
        for (int sequence = 1; sequence <= count; sequence++) {
            employees.add(nextEmployee(sequence));
        }
        return employees;
    }

    private Employee nextEmployee(int sequence) {
        String firstName = pick(FIRST_NAMES);
        String lastName = pick(LAST_NAMES);
        Country country = pickCountry();
        Job job = pick(JOBS);

        return new Employee(
                firstName + " " + lastName,
                emailFor(firstName, lastName, sequence),
                job.title(),
                job.department(),
                country.name(),
                salaryFor(job, country),
                country.currency(),
                randomJoiningDate());
    }

    private String emailFor(String firstName, String lastName, int sequence) {
        return (firstName + "." + lastName + "." + sequence + "@acme.com").toLowerCase();
    }

    private BigDecimal salaryFor(Job job, Country country) {
        double spread = LOWEST_PAY_SPREAD + random.nextDouble() * PAY_SPREAD_RANGE;
        double rawSalary = job.baseSalary() * country.payFactor() * spread;
        long roundedToHundreds = Math.round(rawSalary / 100.0) * 100;
        return BigDecimal.valueOf(roundedToHundreds);
    }

    private LocalDate randomJoiningDate() {
        long daysAvailable = ChronoUnit.DAYS.between(FIRST_JOINING_DATE, LAST_JOINING_DATE) + 1;
        return FIRST_JOINING_DATE.plusDays(random.nextInt((int) daysAvailable));
    }

    private Country pickCountry() {
        int roll = random.nextInt(TOTAL_COUNTRY_WEIGHT);
        int cumulative = 0;
        for (Country country : COUNTRIES) {
            cumulative += country.weight();
            if (roll < cumulative) {
                return country;
            }
        }
        throw new IllegalStateException("Country weights are inconsistent");
    }

    private <T> T pick(List<T> options) {
        return options.get(random.nextInt(options.size()));
    }
}
