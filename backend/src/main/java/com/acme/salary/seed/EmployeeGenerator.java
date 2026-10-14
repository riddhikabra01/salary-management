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
 * First names and surnames come from the same cultural group, so "Hiroshi Tanaka" or "Priya Iyer"
 * can appear, but never "Hiroshi Smith".
 */
public class EmployeeGenerator {

    private static final LocalDate FIRST_JOINING_DATE = LocalDate.of(2012, 1, 1);
    private static final LocalDate LAST_JOINING_DATE = LocalDate.of(2026, 9, 30);
    private static final double LOWEST_PAY_SPREAD = 0.80;
    private static final double PAY_SPREAD_RANGE = 0.45;

    private record NameGroup(List<String> firstNames, List<String> lastNames) {
    }

    private record Country(String name, String currency, double payFactor, int weight,
                           List<NameGroup> nameGroups) {
    }

    private record Job(String title, String department, int baseSalary) {
    }

    private static final NameGroup INDIAN = new NameGroup(
            List.of("Asha", "Rohan", "Vikram", "Priya", "Ananya", "Arjun", "Neha", "Kabir", "Meera", "Rahul",
                    "Sneha", "Aditya", "Kavya", "Ishaan", "Pooja", "Siddharth", "Divya", "Karan", "Nisha", "Varun"),
            List.of("Patel", "Mehta", "Rao", "Sharma", "Iyer", "Kapoor", "Nair", "Singh",
                    "Gupta", "Reddy", "Joshi", "Desai", "Banerjee", "Chopra", "Verma", "Menon"));

    private static final NameGroup AMERICAN = new NameGroup(
            List.of("James", "Emily", "Michael", "Jessica", "David", "Sarah", "Daniel", "Ashley", "Matthew",
                    "Olivia", "Ryan", "Megan", "Andrew", "Lauren", "Joshua", "Hannah", "Brandon", "Natalie"),
            List.of("Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Wilson",
                    "Anderson", "Taylor", "Moore", "Thomas", "Jackson", "White", "Harris", "Clark"));

    private static final NameGroup BRITISH = new NameGroup(
            List.of("Oliver", "Amelia", "George", "Isla", "Harry", "Poppy", "Jack", "Charlotte", "Thomas",
                    "Freya", "William", "Grace", "Henry", "Ella", "Edward", "Lily"),
            List.of("Davies", "Evans", "Roberts", "Walker", "Wright", "Robinson", "Thompson", "Hughes",
                    "Edwards", "Green", "Hall", "Wood", "Clarke", "Baker", "Turner", "Ward"));

    private static final NameGroup GERMAN = new NameGroup(
            List.of("Lukas", "Hannah", "Leon", "Lena", "Felix", "Mia", "Jonas", "Sophie", "Maximilian",
                    "Laura", "Paul", "Anna", "Finn", "Lea", "Tobias", "Julia"),
            List.of("Mueller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner", "Becker",
                    "Hoffmann", "Schulz", "Koch", "Richter", "Klein", "Wolf", "Neumann", "Braun"));

    private static final NameGroup FRENCH = new NameGroup(
            List.of("Louis", "Camille", "Hugo", "Lea", "Arthur", "Chloe", "Jules", "Manon", "Gabriel",
                    "Ines", "Antoine", "Emma", "Theo", "Juliette", "Nicolas", "Clara"),
            List.of("Martin", "Bernard", "Dubois", "Robert", "Richard", "Petit", "Durand", "Leroy",
                    "Moreau", "Simon", "Laurent", "Lefebvre", "Michel", "Garcia", "Roux", "Fournier"));

    private static final NameGroup CANADIAN_ENGLISH = new NameGroup(
            List.of("Liam", "Emma", "Noah", "Olivia", "Ethan", "Chloe", "Lucas", "Zoe"),
            List.of("Smith", "Brown", "Wilson", "MacDonald", "Campbell", "Stewart", "Anderson", "Fraser"));

    private static final NameGroup CANADIAN_FRENCH = new NameGroup(
            List.of("Mathieu", "Sophie", "Samuel", "Charlotte", "Olivier", "Camille", "Alexandre", "Julie"),
            List.of("Tremblay", "Gagnon", "Roy", "Bouchard", "Cote", "Gauthier", "Morin", "Lavoie"));

    private static final NameGroup AUSTRALIAN = new NameGroup(
            List.of("Jack", "Charlotte", "Noah", "Matilda", "Oliver", "Chloe", "William", "Mia", "Lachlan",
                    "Isla", "Mitchell", "Ruby", "Cooper", "Harper", "Riley", "Georgia"),
            List.of("Jones", "Williams", "Brown", "Wilson", "Taylor", "Anderson", "Thompson", "Walker",
                    "White", "Harris", "Martin", "Kelly", "Ryan", "Murphy", "Campbell", "Hall"));

    private static final NameGroup SINGAPOREAN_CHINESE = new NameGroup(
            List.of("Wei Jie", "Mei Ling", "Jun Hao", "Xin Yi", "Kai Wen", "Hui Min", "Zhi Hao", "Li Ting"),
            List.of("Tan", "Lim", "Lee", "Ng", "Wong", "Goh", "Chua", "Teo"));

    private static final NameGroup SINGAPOREAN_INDIAN = new NameGroup(
            List.of("Ravi", "Deepa", "Suresh", "Lakshmi", "Anand", "Kavitha", "Prakash", "Meena"),
            List.of("Kumar", "Pillai", "Krishnan", "Subramaniam", "Raj", "Naidu", "Chandran", "Muthu"));

    private static final NameGroup SINGAPOREAN_MALAY = new NameGroup(
            List.of("Muhammad", "Nur Aisyah", "Ahmad", "Siti", "Hafiz", "Farah", "Irfan", "Aisyah"),
            List.of("Rahman", "Abdullah", "Ismail", "Hassan", "Ibrahim", "Yusof", "Osman", "Salleh"));

    private static final NameGroup BRAZILIAN = new NameGroup(
            List.of("Lucas", "Maria", "Gabriel", "Ana", "Pedro", "Juliana", "Mateus", "Camila", "Rafael",
                    "Beatriz", "Thiago", "Larissa", "Bruno", "Fernanda", "Felipe", "Isabela"),
            List.of("Silva", "Santos", "Oliveira", "Souza", "Lima", "Pereira", "Costa", "Ferreira",
                    "Rodrigues", "Almeida", "Nascimento", "Carvalho", "Gomes", "Martins", "Araujo", "Ribeiro"));

    private static final NameGroup JAPANESE = new NameGroup(
            List.of("Hiroshi", "Yuki", "Kenji", "Sakura", "Takumi", "Haruka", "Daiki", "Aoi", "Ren",
                    "Yui", "Sota", "Hana", "Kaito", "Mio", "Ryota", "Rina"),
            List.of("Sato", "Suzuki", "Takahashi", "Tanaka", "Watanabe", "Ito", "Yamamoto", "Nakamura",
                    "Kobayashi", "Kato", "Yoshida", "Yamada", "Sasaki", "Matsumoto", "Inoue", "Kimura"));

    private static final List<Country> COUNTRIES = List.of(
            new Country("India", "INR", 25.0, 30, List.of(INDIAN)),
            new Country("United States", "USD", 1.0, 20, List.of(AMERICAN)),
            new Country("United Kingdom", "GBP", 0.67, 8, List.of(BRITISH)),
            new Country("Germany", "EUR", 0.78, 8, List.of(GERMAN)),
            new Country("France", "EUR", 0.72, 5, List.of(FRENCH)),
            new Country("Canada", "CAD", 1.2, 6, List.of(CANADIAN_ENGLISH, CANADIAN_FRENCH)),
            new Country("Australia", "AUD", 1.4, 5, List.of(AUSTRALIAN)),
            new Country("Singapore", "SGD", 1.2, 5,
                    List.of(SINGAPOREAN_CHINESE, SINGAPOREAN_INDIAN, SINGAPOREAN_MALAY)),
            new Country("Brazil", "BRL", 1.5, 5, List.of(BRAZILIAN)),
            new Country("Japan", "JPY", 105.0, 8, List.of(JAPANESE)));

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
        Country country = pickCountry();
        NameGroup names = pick(country.nameGroups());
        String firstName = pick(names.firstNames());
        String lastName = pick(names.lastNames());
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
        String address = firstName + "." + lastName + "." + sequence + "@acme.com";
        return address.toLowerCase().replaceAll("\\s+", "");
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
