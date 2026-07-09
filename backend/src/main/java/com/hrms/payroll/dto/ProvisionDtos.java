package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProvisionDtos {
    private ProvisionDtos() {}

    public static class CreateRequest {
        private UUID periodId;
        private UUID projectId;
        private String payGroup = "ALL";
        private String provisionType = "LEAVE";

        public UUID getPeriodId() { return periodId; }
        public void setPeriodId(UUID periodId) { this.periodId = periodId; }
        public UUID getProjectId() { return projectId; }
        public void setProjectId(UUID projectId) { this.projectId = projectId; }
        public String getPayGroup() { return payGroup; }
        public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
        public String getProvisionType() { return provisionType; }
        public void setProvisionType(String provisionType) { this.provisionType = provisionType; }
    }

    public static class RunDto {
        private UUID id;
        private UUID periodId;
        private String periodName;
        private LocalDate periodStartDate;
        private LocalDate periodEndDate;
        private UUID projectId;
        private String payGroup;
        private String provisionType;
        private String status;
        private Instant calculatedAt;
        private int employeeCount;
        private BigDecimal totalEligibleAmount = BigDecimal.ZERO;
        private BigDecimal totalProvisionAmount = BigDecimal.ZERO;
        private String notes;
        private List<ResultDto> results = new ArrayList<>();

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getPeriodId() { return periodId; }
        public void setPeriodId(UUID periodId) { this.periodId = periodId; }
        public String getPeriodName() { return periodName; }
        public void setPeriodName(String periodName) { this.periodName = periodName; }
        public LocalDate getPeriodStartDate() { return periodStartDate; }
        public void setPeriodStartDate(LocalDate periodStartDate) { this.periodStartDate = periodStartDate; }
        public LocalDate getPeriodEndDate() { return periodEndDate; }
        public void setPeriodEndDate(LocalDate periodEndDate) { this.periodEndDate = periodEndDate; }
        public UUID getProjectId() { return projectId; }
        public void setProjectId(UUID projectId) { this.projectId = projectId; }
        public String getPayGroup() { return payGroup; }
        public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
        public String getProvisionType() { return provisionType; }
        public void setProvisionType(String provisionType) { this.provisionType = provisionType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getCalculatedAt() { return calculatedAt; }
        public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        public BigDecimal getTotalEligibleAmount() { return totalEligibleAmount; }
        public void setTotalEligibleAmount(BigDecimal totalEligibleAmount) { this.totalEligibleAmount = totalEligibleAmount; }
        public BigDecimal getTotalProvisionAmount() { return totalProvisionAmount; }
        public void setTotalProvisionAmount(BigDecimal totalProvisionAmount) { this.totalProvisionAmount = totalProvisionAmount; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public List<ResultDto> getResults() { return results; }
        public void setResults(List<ResultDto> results) { this.results = results; }
    }

    public static class ResultDto {
        private UUID id;
        private UUID employeeId;
        private String employeeNumber;
        private String employeeName;
        private UUID projectId;
        private String payGroup;
        private BigDecimal eligibleAmount = BigDecimal.ZERO;
        private BigDecimal provisionAmount = BigDecimal.ZERO;
        private String formulaNote;
        private String status;
        private String message;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getEmployeeId() { return employeeId; }
        public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
        public String getEmployeeNumber() { return employeeNumber; }
        public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public UUID getProjectId() { return projectId; }
        public void setProjectId(UUID projectId) { this.projectId = projectId; }
        public String getPayGroup() { return payGroup; }
        public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
        public BigDecimal getEligibleAmount() { return eligibleAmount; }
        public void setEligibleAmount(BigDecimal eligibleAmount) { this.eligibleAmount = eligibleAmount; }
        public BigDecimal getProvisionAmount() { return provisionAmount; }
        public void setProvisionAmount(BigDecimal provisionAmount) { this.provisionAmount = provisionAmount; }
        public String getFormulaNote() { return formulaNote; }
        public void setFormulaNote(String formulaNote) { this.formulaNote = formulaNote; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
