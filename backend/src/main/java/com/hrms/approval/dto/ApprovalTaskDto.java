package com.hrms.approval.dto;

import java.time.Instant;
import java.util.UUID;
import java.math.BigDecimal;

public class ApprovalTaskDto {
    private UUID instanceId;
    private UUID stepId;
    private String processCode;
    private String entityType;
    private UUID entityId;
    private UUID employeeId;
    private String employeeNumber;
    private String employeeName;
    private UUID projectId;
    private String projectCode;
    private int stepOrder;
    private String stepName;
    private String status;
    private Instant submittedAt;
    private Integer periodYear;
    private Integer periodMonth;
    private String payGroup;
    private String timesheetStatus;
    private BigDecimal totalWorkedHours;
    private BigDecimal totalOtHours;
    private BigDecimal totalAbsenceDays;

    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }
    public UUID getStepId() { return stepId; }
    public void setStepId(UUID stepId) { this.stepId = stepId; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String processCode) { this.processCode = processCode; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
    public String getTimesheetStatus() { return timesheetStatus; }
    public void setTimesheetStatus(String timesheetStatus) { this.timesheetStatus = timesheetStatus; }
    public BigDecimal getTotalWorkedHours() { return totalWorkedHours; }
    public void setTotalWorkedHours(BigDecimal totalWorkedHours) { this.totalWorkedHours = totalWorkedHours; }
    public BigDecimal getTotalOtHours() { return totalOtHours; }
    public void setTotalOtHours(BigDecimal totalOtHours) { this.totalOtHours = totalOtHours; }
    public BigDecimal getTotalAbsenceDays() { return totalAbsenceDays; }
    public void setTotalAbsenceDays(BigDecimal totalAbsenceDays) { this.totalAbsenceDays = totalAbsenceDays; }
}
