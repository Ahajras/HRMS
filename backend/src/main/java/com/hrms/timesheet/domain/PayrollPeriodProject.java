package com.hrms.timesheet.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Per-project lock state inside a {@link PayrollPeriod}: OPEN -> LOCKED -> CLOSED. */
@Entity
@Table(name = "payroll_period_project")
public class PayrollPeriodProject extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "period_id", nullable = false)
    private UUID periodId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getPeriodId() { return periodId; }
    public void setPeriodId(UUID periodId) { this.periodId = periodId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
