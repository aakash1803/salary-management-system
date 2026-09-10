package com.salarymanagement.salary;

import com.salarymanagement.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single, immutable salary entry effective from a given date for one employee.
 *
 * <p>Salary history is preserved by always creating a new {@code SalaryRecord} for a salary
 * change; existing records are never updated or deleted. Accordingly, this entity exposes no
 * setters — once persisted, a record's fields do not change. There is deliberately no
 * "current salary" field anywhere: the current salary is always derived by querying salary
 * records (see {@code SalaryRecordRepository}), never stored redundantly.
 *
 * <p>Future-dated {@code effectiveFrom} values are allowed; a record simply is not "current"
 * until its effective date arrives.
 */
@Entity
@Table(
        name = "salary_record",
        indexes = {
                @Index(name = "idx_salary_record_employee_effective", columnList = "employee_id, effective_from")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_salary_record_employee")
    )
    private Employee employee;

    @NotNull
    @Positive
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotBlank
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SalaryRecord(Employee employee, BigDecimal amount, String currency, LocalDate effectiveFrom) {
        this.employee = employee;
        this.amount = amount;
        this.currency = currency;
        this.effectiveFrom = effectiveFrom;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SalaryRecord other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        // References the employee only by id, never the Employee object itself, to avoid
        // recursing back into Employee (and to avoid forcing a lazy load just to log).
        return "SalaryRecord{" +
                "id=" + id +
                ", employeeId=" + (employee != null ? employee.getId() : null) +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", effectiveFrom=" + effectiveFrom +
                '}';
    }
}
