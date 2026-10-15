package com.acme.salary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. One Spring Boot app serves both the REST API and the built Angular UI,
 * so the whole product runs as a single jar on a single port.
 */
@SpringBootApplication
public class SalaryManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalaryManagementApplication.class, args);
    }
}
