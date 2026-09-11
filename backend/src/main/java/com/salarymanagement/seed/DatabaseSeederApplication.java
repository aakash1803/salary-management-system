package com.salarymanagement.seed;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entry point for the developer-invoked './gradlew seedDatabase' task.
 *
 * <p>Bootstraps the Spring Boot application context in CLI mode (no web server)
 * and executes the database reset and seeding workflow.
 */
@SpringBootApplication(scanBasePackages = "com.salarymanagement")
public class DatabaseSeederApplication {

    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext context = new SpringApplicationBuilder(DatabaseSeederApplication.class)
                    .web(WebApplicationType.NONE)
                    .run(args);

            DatabaseSeederService seeder = context.getBean(DatabaseSeederService.class);

            System.out.println("Starting database reset...");
            DatabaseSeederService.SeedResult result = seeder.seed();

            System.out.printf("Deleted salary records: %d%n", result.deletedSalaryRecords());
            System.out.printf("Deleted employees: %d%n", result.deletedEmployees());
            System.out.println("Creating 10,000 employees...");
            System.out.println("Creating salary records...");
            System.out.println("Seed completed successfully.");
            System.out.printf("Employees: %d%n", result.createdEmployees());
            System.out.printf("Salary records: %d%n", result.createdSalaryRecords());
            System.out.printf("Duration: %d ms%n", result.durationMs());

            context.close();
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Database seeding failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
