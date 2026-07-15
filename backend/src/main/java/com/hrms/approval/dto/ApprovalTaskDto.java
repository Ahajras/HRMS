package com.hrms.approval.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    private LocalDate leaveStartDate;
    private LocalDate leaveEndDate;
    private LocalDate leaveReturnDate;
    private BigDecimal leaveTotalDays;
    private String leaveStatus;
    private String leaveTypeCode;
    private String leaveTypeName;
    private String leaveReason;
    private boolean leaveRequiresTicket;
    private String leaveTicketFrom;
    private String leaveTicketTo;
    private LocalDate leaveTravelDate;
    private LocalDate leaveReturnTravelDate;
    private String leaveDestination;
    private String leavePassportNumber;
    private Integer leaveDependentCount;
    private String leaveTravelRemarks;
    private String leaveContactPhone;
    private String leaveContactEmail;
    private String leaveAddressDuringLeave;
    private String leaveEmergencyContactName;
    private String leaveEmergencyContactPhone;
    private List<StepHistoryDto> history = new ArrayList<>();

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
    public LocalDate getLeaveStartDate() { return leaveStartDate; }
    public void setLeaveStartDate(LocalDate leaveStartDate) { this.leaveStartDate = leaveStartDate; }
    public LocalDate getLeaveEndDate() { return leaveEndDate; }
    public void setLeaveEndDate(LocalDate leaveEndDate) { this.leaveEndDate = leaveEndDate; }
    public LocalDate getLeaveReturnDate() { return leaveReturnDate; }
    public void setLeaveReturnDate(LocalDate leaveReturnDate) { this.leaveReturnDate = leaveReturnDate; }
    public BigDecimal getLeaveTotalDays() { return leaveTotalDays; }
    public void setLeaveTotalDays(BigDecimal leaveTotalDays) { this.leaveTotalDays = leaveTotalDays; }
    public String getLeaveStatus() { return leaveStatus; }
    public void setLeaveStatus(String leaveStatus) { this.leaveStatus = leaveStatus; }
    public String getLeaveTypeCode() { return leaveTypeCode; }
    public void setLeaveTypeCode(String leaveTypeCode) { this.leaveTypeCode = leaveTypeCode; }
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public String getLeaveReason() { return leaveReason; }
    public void setLeaveReason(String leaveReason) { this.leaveReason = leaveReason; }
    public boolean isLeaveRequiresTicket() { return leaveRequiresTicket; }
    public void setLeaveRequiresTicket(boolean leaveRequiresTicket) { this.leaveRequiresTicket = leaveRequiresTicket; }
    public String getLeaveTicketFrom() { return leaveTicketFrom; }
    public void setLeaveTicketFrom(String leaveTicketFrom) { this.leaveTicketFrom = leaveTicketFrom; }
    public String getLeaveTicketTo() { return leaveTicketTo; }
    public void setLeaveTicketTo(String leaveTicketTo) { this.leaveTicketTo = leaveTicketTo; }
    public LocalDate getLeaveTravelDate() { return leaveTravelDate; }
    public void setLeaveTravelDate(LocalDate leaveTravelDate) { this.leaveTravelDate = leaveTravelDate; }
    public LocalDate getLeaveReturnTravelDate() { return leaveReturnTravelDate; }
    public void setLeaveReturnTravelDate(LocalDate leaveReturnTravelDate) { this.leaveReturnTravelDate = leaveReturnTravelDate; }
    public String getLeaveDestination() { return leaveDestination; }
    public void setLeaveDestination(String leaveDestination) { this.leaveDestination = leaveDestination; }
    public String getLeavePassportNumber() { return leavePassportNumber; }
    public void setLeavePassportNumber(String leavePassportNumber) { this.leavePassportNumber = leavePassportNumber; }
    public Integer getLeaveDependentCount() { return leaveDependentCount; }
    public void setLeaveDependentCount(Integer leaveDependentCount) { this.leaveDependentCount = leaveDependentCount; }
    public String getLeaveTravelRemarks() { return leaveTravelRemarks; }
    public void setLeaveTravelRemarks(String leaveTravelRemarks) { this.leaveTravelRemarks = leaveTravelRemarks; }
    public String getLeaveContactPhone() { return leaveContactPhone; }
    public void setLeaveContactPhone(String leaveContactPhone) { this.leaveContactPhone = leaveContactPhone; }
    public String getLeaveContactEmail() { return leaveContactEmail; }
    public void setLeaveContactEmail(String leaveContactEmail) { this.leaveContactEmail = leaveContactEmail; }
    public String getLeaveAddressDuringLeave() { return leaveAddressDuringLeave; }
    public void setLeaveAddressDuringLeave(String leaveAddressDuringLeave) { this.leaveAddressDuringLeave = leaveAddressDuringLeave; }
    public String getLeaveEmergencyContactName() { return leaveEmergencyContactName; }
    public void setLeaveEmergencyContactName(String leaveEmergencyContactName) { this.leaveEmergencyContactName = leaveEmergencyContactName; }
    public String getLeaveEmergencyContactPhone() { return leaveEmergencyContactPhone; }
    public void setLeaveEmergencyContactPhone(String leaveEmergencyContactPhone) { this.leaveEmergencyContactPhone = leaveEmergencyContactPhone; }
    public List<StepHistoryDto> getHistory() { return history; }
    public void setHistory(List<StepHistoryDto> history) { this.history = history; }

    public static class StepHistoryDto {
        private int stepOrder;
        private String stepName;
        private String status;
        private String approverNumber;
        private String approverName;
        private String decidedBy;
        private Instant decidedAt;
        private String remarks;

        public int getStepOrder() { return stepOrder; }
        public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getApproverNumber() { return approverNumber; }
        public void setApproverNumber(String approverNumber) { this.approverNumber = approverNumber; }
        public String getApproverName() { return approverName; }
        public void setApproverName(String approverName) { this.approverName = approverName; }
        public String getDecidedBy() { return decidedBy; }
        public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
        public Instant getDecidedAt() { return decidedAt; }
        public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }
}
