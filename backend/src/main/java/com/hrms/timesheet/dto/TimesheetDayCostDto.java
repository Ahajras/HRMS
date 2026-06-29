package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TimesheetDayCostDto {

    private UUID id;
    private UUID projectId;
    private UUID costCodeId;
    private BigDecimal hours;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }

    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
}
