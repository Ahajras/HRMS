package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A pending Day Zero correction — created when something (e.g. an approved
 * leave) turns out to affect an ESTIMATED day whose period is already
 * locked. The locked period is never touched; this amount waits here until
 * the employee's next payroll run picks it up as an extra line.
 */
@Entity
@Table(name = "payroll_adjustment")
public class PayrollAdjustment extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "original_period_id", nullable = false)
    private UUID originalPeriodId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** Positive = extra pay owed to the employee next run; negative = to be deducted. */
    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "SYSTEM";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "applied_run_id")
    private UUID appliedRunId;

    @Column(name = "applied_at")
    private Instant appliedAt;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public UUID getOriginalPeriodId() { return originalPeriodId; }
    public void setOriginalPeriodId(UUID originalPeriodId) { this.originalPeriodId = originalPeriodId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getAppliedRunId() { return appliedRunId; }
    public void setAppliedRunId(UUID appliedRunId) { this.appliedRunId = appliedRunId; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
}
