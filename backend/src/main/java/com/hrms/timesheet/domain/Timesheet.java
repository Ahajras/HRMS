package com.hrms.timesheet.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One employee's timesheet for a calendar month (FTDD Vol.1 Ch.3). Source of
 * actual worked hours for every later engine. Lifecycle:
 * DRAFT -> SUBMITTED -> APPROVED -> LOCKED (reopen returns it to DRAFT).
 */
@Entity
@Table(name = "timesheet")
public class Timesheet extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "period_year", nullable = false)
    private int periodYear;

    @Column(name = "period_month", nullable = false)
    private int periodMonth;

    @Column(name = "shift_id")
    private UUID shiftId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "total_worked_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalWorkedHours = BigDecimal.ZERO;

    @Column(name = "total_ot_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal totalOtHours = BigDecimal.ZERO;

    @Column(name = "total_absence_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalAbsenceDays = BigDecimal.ZERO;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalWorkedHours() { return totalWorkedHours; }
    public void setTotalWorkedHours(BigDecimal totalWorkedHours) { this.totalWorkedHours = totalWorkedHours; }

    public BigDecimal getTotalOtHours() { return totalOtHours; }
    public void setTotalOtHours(BigDecimal totalOtHours) { this.totalOtHours = totalOtHours; }

    public BigDecimal getTotalAbsenceDays() { return totalAbsenceDays; }
    public void setTotalAbsenceDays(BigDecimal totalAbsenceDays) { this.totalAbsenceDays = totalAbsenceDays; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
}
