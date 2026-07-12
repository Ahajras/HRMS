package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AuditDtos {

    public static class DayZeroAdjustmentRow {
        private UUID id;
        private LocalDate workDate;
        private int periodYear;
        private int periodMonth;
        private BigDecimal amount;
        private String status;
        private String reason;
        private Instant createdAt;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public LocalDate getWorkDate() { return workDate; }
        public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
        public int getPeriodYear() { return periodYear; }
        public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
        public int getPeriodMonth() { return periodMonth; }
        public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }

    public static class PayrollRunRow {
        private UUID id;
        private int periodYear;
        private int periodMonth;
        private String projectCode;
        private String payGroup;
        private String status;
        private Instant createdAt;
        private int employeeCount;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public int getPeriodYear() { return periodYear; }
        public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
        public int getPeriodMonth() { return periodMonth; }
        public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
        public String getPayGroup() { return payGroup; }
        public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
    }

    public static class LeaveDiscrepancyRow {
        private UUID leaveRequestId;
        private String leaveTypeCode;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal recordedDays;
        private BigDecimal actualDays;

        public UUID getLeaveRequestId() { return leaveRequestId; }
        public void setLeaveRequestId(UUID leaveRequestId) { this.leaveRequestId = leaveRequestId; }
        public String getLeaveTypeCode() { return leaveTypeCode; }
        public void setLeaveTypeCode(String leaveTypeCode) { this.leaveTypeCode = leaveTypeCode; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public BigDecimal getRecordedDays() { return recordedDays; }
        public void setRecordedDays(BigDecimal recordedDays) { this.recordedDays = recordedDays; }
        public BigDecimal getActualDays() { return actualDays; }
        public void setActualDays(BigDecimal actualDays) { this.actualDays = actualDays; }
    }

    public static class HandoverCleanupResult {
        private long employeesLeft;
        private long projectsLeft;
        private long appUsersLeft;
        private long timesheetsLeft;
        private long payrollResultsLeft;
        private long crewsLeft;
        private long shiftsLeft;
        private long costCodesLeft;

        public long getEmployeesLeft() { return employeesLeft; }
        public void setEmployeesLeft(long employeesLeft) { this.employeesLeft = employeesLeft; }
        public long getProjectsLeft() { return projectsLeft; }
        public void setProjectsLeft(long projectsLeft) { this.projectsLeft = projectsLeft; }
        public long getAppUsersLeft() { return appUsersLeft; }
        public void setAppUsersLeft(long appUsersLeft) { this.appUsersLeft = appUsersLeft; }
        public long getTimesheetsLeft() { return timesheetsLeft; }
        public void setTimesheetsLeft(long timesheetsLeft) { this.timesheetsLeft = timesheetsLeft; }
        public long getPayrollResultsLeft() { return payrollResultsLeft; }
        public void setPayrollResultsLeft(long payrollResultsLeft) { this.payrollResultsLeft = payrollResultsLeft; }
        public long getCrewsLeft() { return crewsLeft; }
        public void setCrewsLeft(long crewsLeft) { this.crewsLeft = crewsLeft; }
        public long getShiftsLeft() { return shiftsLeft; }
        public void setShiftsLeft(long shiftsLeft) { this.shiftsLeft = shiftsLeft; }
        public long getCostCodesLeft() { return costCodesLeft; }
        public void setCostCodesLeft(long costCodesLeft) { this.costCodesLeft = costCodesLeft; }
    }
}
