package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** One (project, cost code) line of allocated hours + monetary value. */
public class CostCodeLineDto {

    private UUID projectId;
    private String projectCode;
    private String projectName;
    private UUID costCodeId;
    private String costCodeCode;
    private String costCodeName;
    private BigDecimal hours = BigDecimal.ZERO;
    private BigDecimal value = BigDecimal.ZERO;

    public CostCodeLineDto() { }

    public CostCodeLineDto(UUID projectId, String projectCode, String projectName,
                           UUID costCodeId, String costCodeCode, String costCodeName) {
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.costCodeId = costCodeId;
        this.costCodeCode = costCodeCode;
        this.costCodeName = costCodeName;
    }

    public void add(BigDecimal hoursToAdd, BigDecimal valueToAdd) {
        this.hours = this.hours.add(hoursToAdd);
        this.value = this.value.add(valueToAdd);
    }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }
    public String getCostCodeCode() { return costCodeCode; }
    public void setCostCodeCode(String costCodeCode) { this.costCodeCode = costCodeCode; }
    public String getCostCodeName() { return costCodeName; }
    public void setCostCodeName(String costCodeName) { this.costCodeName = costCodeName; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}
