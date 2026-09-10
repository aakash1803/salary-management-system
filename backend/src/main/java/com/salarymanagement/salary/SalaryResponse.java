    package com.salarymanagement.salary;

    import java.math.BigDecimal;
    import java.time.Instant;
    import java.time.LocalDate;

    /**
     * API-facing representation of a {@link SalaryRecord}, used for the create response, the current
     * salary, and each entry in salary history.
     */
    public record SalaryResponse(
            Long id,
            Long employeeId,
            BigDecimal amount,
            String currency,
            LocalDate effectiveFrom,
            Instant createdAt
    ) {

        public static SalaryResponse from(SalaryRecord salaryRecord) {
            return new SalaryResponse(
                    salaryRecord.getId(),
                    salaryRecord.getEmployee().getId(),
                    salaryRecord.getAmount(),
                    salaryRecord.getCurrency(),
                    salaryRecord.getEffectiveFrom(),
                    salaryRecord.getCreatedAt()
            );
        }
    }
