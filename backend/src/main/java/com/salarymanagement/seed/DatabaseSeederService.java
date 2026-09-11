package com.salarymanagement.seed;

import com.salarymanagement.employee.Employee;
import com.salarymanagement.employee.EmployeeRepository;
import com.salarymanagement.salary.SalaryRecord;
import com.salarymanagement.salary.SalaryRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for resetting and seeding the database with 10,000 employees
 * and a realistic distribution of salary records (historical, active, and future-dated).
 */
@Service
public class DatabaseSeederService {

    private final EmployeeRepository employeeRepository;
    private final SalaryRecordRepository salaryRecordRepository;

    public static final int TARGET_EMPLOYEE_COUNT = 10000;

    private static final String[] FIRST_NAMES = {
            "Ada", "Alan", "Alex", "Alice", "Andrew", "Anita", "Arthur", "Barbara", "Benjamin", "Carol",
            "Charles", "Chloe", "Daniel", "David", "Diana", "Edward", "Elena", "Emma", "Ethan", "Fiona",
            "Frank", "George", "Grace", "Hannah", "Harry", "Ian", "Isabel", "Jack", "James", "Jennifer",
            "John", "Julia", "Katherine", "Kevin", "Laura", "Liam", "Lucas", "Margaret", "Maria", "Mark",
            "Matthew", "Megan", "Michael", "Nancy", "Nathan", "Oliver", "Olivia", "Patrick", "Paul", "Rachel"
    };

    private static final String[] LAST_NAMES = {
            "Adams", "Allen", "Anderson", "Baker", "Brown", "Campbell", "Carter", "Clark", "Davis", "Evans",
            "Garcia", "Hall", "Harris", "Jackson", "Jenkins", "Johnson", "Jones", "King", "Lee", "Lewis",
            "Lovelace", "Martin", "Miller", "Mitchell", "Moore", "Nelson", "Parker", "Phillips", "Roberts", "Robinson",
            "Rodriguez", "Ross", "Russell", "Scott", "Smith", "Taylor", "Thomas", "Turing", "Vance", "Walker",
            "White", "Williams", "Wilson", "Wright", "Young", "Bennett", "Chen", "Edwards", "Howard", "Ward"
    };

    private static final String[] COUNTRIES = {
            "Canada", "France", "Germany", "India", "Japan", "United Kingdom", "United States"
    };

    private static final String[] DEPARTMENTS = {
            "Engineering", "Executive", "Finance", "Human Resources", "Marketing", "Operations", "Product", "Sales", "Technology"
    };

    private static final String[] CURRENCIES = {
            "USD", "EUR", "GBP", "CAD", "INR", "JPY", "AUD"
    };

    public record SeedResult(
            long deletedSalaryRecords,
            long deletedEmployees,
            long createdEmployees,
            long createdSalaryRecords,
            long durationMs
    ) {}

    public DatabaseSeederService(EmployeeRepository employeeRepository, SalaryRecordRepository salaryRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.salaryRecordRepository = salaryRecordRepository;
    }

    @Transactional
    public SeedResult seed() {
        long startTime = System.currentTimeMillis();

        long initialSalaryCount = salaryRecordRepository.count();
        long initialEmployeeCount = employeeRepository.count();

        // Step 1: Delete existing data in reverse foreign-key dependency order
        salaryRecordRepository.deleteAllInBatch();
        employeeRepository.deleteAllInBatch();

        // Step 2: Deterministically generate 10,000 employees in batch chunks
        List<Employee> createdEmployeesList = new ArrayList<>(TARGET_EMPLOYEE_COUNT);
        List<Employee> employeeBatch = new ArrayList<>(1000);

        for (int i = 1; i <= TARGET_EMPLOYEE_COUNT; i++) {
            String empNum = "EMP-" + (1000 + i);
            String firstName = FIRST_NAMES[(i - 1) % FIRST_NAMES.length];
            String lastName = LAST_NAMES[((i - 1) * 7) % LAST_NAMES.length];
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@company.com";
            String country = COUNTRIES[(i - 1) % COUNTRIES.length];
            String department = DEPARTMENTS[(i - 1) % DEPARTMENTS.length];

            Employee emp = new Employee(empNum, firstName, lastName, email, country, department);
            employeeBatch.add(emp);

            if (employeeBatch.size() == 1000 || i == TARGET_EMPLOYEE_COUNT) {
                List<Employee> saved = employeeRepository.saveAllAndFlush(employeeBatch);
                createdEmployeesList.addAll(saved);
                employeeBatch.clear();
            }
        }

        // Step 3: Deterministically generate salary records for created employees
        List<SalaryRecord> salaryBatch = new ArrayList<>(1000);
        long totalSalaryRecords = 0;

        for (int i = 1; i <= createdEmployeesList.size(); i++) {
            Employee emp = createdEmployeesList.get(i - 1);
            long baseSalary = 45000 + ((long) i * 12345) % 110000;
            String currency = CURRENCIES[(i - 1) % CURRENCIES.length];

            int recordCount = 1 + ((i - 1) % 3);

            // Record 1: Initial salary (historical)
            LocalDate eff1 = LocalDate.of(2023, 1 + ((i * 3) % 12), 1 + ((i * 5) % 28));
            SalaryRecord r1 = new SalaryRecord(emp, BigDecimal.valueOf(baseSalary), currency, eff1);
            salaryBatch.add(r1);
            totalSalaryRecords++;

            // Record 2: Salary adjustment / promotion
            if (recordCount >= 2) {
                LocalDate eff2 = LocalDate.of(2024, 1 + ((i * 7) % 12), 1 + ((i * 11) % 28));
                SalaryRecord r2 = new SalaryRecord(emp, BigDecimal.valueOf(baseSalary + 5000 + ((i * 100) % 15000)), currency, eff2);
                salaryBatch.add(r2);
                totalSalaryRecords++;
            }

            // Record 3: Historical 2025 or future-dated 2028 record
            if (recordCount >= 3) {
                LocalDate eff3 = (i % 2 == 0)
                        ? LocalDate.of(2025, 1 + (i % 6), 1 + (i % 28))
                        : LocalDate.of(2027 + (i % 2), 1 + (i % 12), 1 + (i % 28));
                SalaryRecord r3 = new SalaryRecord(emp, BigDecimal.valueOf(baseSalary + 15000 + ((i * 200) % 20000)), currency, eff3);
                salaryBatch.add(r3);
                totalSalaryRecords++;
            }

            if (salaryBatch.size() >= 1000) {
                salaryRecordRepository.saveAllAndFlush(salaryBatch);
                salaryBatch.clear();
            }
        }

        if (!salaryBatch.isEmpty()) {
            salaryRecordRepository.saveAllAndFlush(salaryBatch);
            salaryBatch.clear();
        }

        long endTime = System.currentTimeMillis();

        return new SeedResult(
                initialSalaryCount,
                initialEmployeeCount,
                createdEmployeesList.size(),
                totalSalaryRecords,
                endTime - startTime
        );
    }
}
