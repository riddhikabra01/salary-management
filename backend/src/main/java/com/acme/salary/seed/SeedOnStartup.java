package com.acme.salary.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds sample employees at startup when app.seed.enabled=true. Does nothing if data already exists.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SeedOnStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedOnStartup.class);

    private final EmployeeSeeder seeder;
    private final int count;

    public SeedOnStartup(EmployeeSeeder seeder, @Value("${app.seed.count:10000}") int count) {
        this.seeder = seeder;
        this.count = count;
    }

    @Override
    public void run(ApplicationArguments args) {
        int inserted = seeder.seedIfEmpty(count);
        if (inserted == 0) {
            log.info("Seeding skipped because employees already exist");
        } else {
            log.info("Seeded {} employees", inserted);
        }
    }
}
