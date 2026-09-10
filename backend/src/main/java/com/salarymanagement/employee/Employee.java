package com.salarymanagement.employee;

import com.salarymanagement.salary.SalaryRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An employee of the organization.
 *
 * <p>{@code employeeNumber} is the unique business identifier. {@code createdAt}/{@code updatedAt}
 * are managed automatically via JPA lifecycle callbacks. The current salary is deliberately NOT
 * stored here (see {@link SalaryRecord}) — it is always derived from salary history.
 */
@Entity
@Table(
        name = "employee",
        indexes = {
                @Index(name = "idx_employee_country", columnList = "country"),
                @Index(name = "idx_employee_department", columnList = "department")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Setter
    @Column(name = "employee_number", nullable = false, unique = true, length = 50)
    private String employeeNumber;

    @NotBlank
    @Setter
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Setter
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Email
    @Setter
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @NotBlank
    @Setter
    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @NotBlank
    @Setter
    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Inverse side of the Employee-to-SalaryRecord association. Lazy and read-only from here:
     * salary records are created and persisted through {@code SalaryRecordRepository}, keyed to
     * this employee via the owning {@code SalaryRecord.employee} association, not through this
     * collection.
     */
    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY)
    private List<SalaryRecord> salaryRecords = new ArrayList<>();

    public Employee(String employeeNumber, String firstName, String lastName,
                    String email, String country, String department) {
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.country = country;
        this.department = department;
    }

    public List<SalaryRecord> getSalaryRecords() {
        return Collections.unmodifiableList(salaryRecords);
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Employee other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        // Constant hash code for entities: avoids a mutating hashCode as the transient id is
        // assigned on persist, which would break contracts of hash-based collections.
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        // Deliberately excludes salaryRecords (lazy collection - would trigger loading, and could
        // recurse back into this employee via SalaryRecord).
        return "Employee{" +
                "id=" + id +
                ", employeeNumber='" + employeeNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", country='" + country + '\'' +
                ", department='" + department + '\'' +
                '}';
    }
}
