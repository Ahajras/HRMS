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

    /** The corrected time type for this specific day, if the correction
     * changed the type (e.g. Normal -> Sick). Null when the correction was
     * only about worked hours on an otherwise-unchanged rest day. Stored
     * as a structured column (not just inside the free-text reason) so
     * annual/consecutive usage counting can pick it up automatically. */
    @Column(name = "new_time_type_id")
    private UUID newTimeTypeId;

    /** The original timesheet day this correction applies to — lets usage
     * counting find and "replace" that day's contribution with the
     * corrected type, instead of double counting or missing it entirely. */
    @Column(name = "timesheet_day_id")
    private UUID timesheetDayId;

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
    public UUID getNewTimeTypeId() { return newTimeTypeId; }
    public void setNewTimeTypeId(UUID newTimeTypeId) { this.newTimeTypeId = newTimeTypeId; }
    public UUID getTimesheetDayId() { return timesheetDayId; }
    public void setTimesheetDayId(UUID timesheetDayId) { this.timesheetDayId = timesheetDayId; }
}
