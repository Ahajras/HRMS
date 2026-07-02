package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * One payroll run for a period (optionally scoped to a project).
 * Lifecycle DRAFT -> CALCULATED -> APPROVED -> LOCKED (FTDD Vol.2 Ch.24 §24.4).
 */
@Entity
@Table(name = "payroll_run")
public class PayrollRun extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "period_id", nullable = false)
    private UUID periodId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "pay_group", nullable = false, length = 20)
    private String payGroup = "ALL";

    @Column(name = "run_type", nullable = false, length = 20)
    private String runType = "REGULAR";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "notes", length = 500)
    private String notes;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getPeriodId() { return periodId; }
    public void setPeriodId(UUID periodId) { this.periodId = periodId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }

    public String getRunType() { return runType; }
    public void setRunType(String runType) { this.runType = runType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
