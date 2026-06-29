package com.hrms.timesheet.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Splits a {@link TimesheetDay}'s worked hours across cost codes
 * (legacy PAYIN HR_CC1..HR_CC8). When a day has no allocation rows, its own
 * project/cost code is the single target.
 */
@Entity
@Table(name = "timesheet_day_cost")
public class TimesheetDayCost extends AuditableEntity {

    @Column(name = "timesheet_day_id", nullable = false)
    private UUID timesheetDayId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "cost_code_id")
    private UUID costCodeId;

    @Column(name = "hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal hours = BigDecimal.ZERO;

    public UUID getTimesheetDayId() { return timesheetDayId; }
    public void setTimesheetDayId(UUID timesheetDayId) { this.timesheetDayId = timesheetDayId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }

    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
}
