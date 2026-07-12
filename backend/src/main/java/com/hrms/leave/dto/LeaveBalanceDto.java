package com.hrms.leave.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class LeaveBalanceDto {
    private UUID employeeId;
    private UUID leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;
    private LocalDate asOfDate;
    private BigDecimal annualRate = BigDecimal.ZERO;
    private BigDecimal entitledToDate = BigDecimal.ZERO;
    private BigDecimal adjustments = BigDecimal.ZERO;
    private BigDecimal usedApproved = BigDecimal.ZERO;
    private BigDecimal usedTimesheet = BigDecimal.ZERO;
    private BigDecimal pending = BigDecimal.ZERO;
    private BigDecimal balance = BigDecimal.ZERO;
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(UUID leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public String getLeaveTypeCode() { return leaveTypeCode; }
    public void setLeaveTypeCode(String leaveTypeCode) { this.leaveTypeCode = leaveTypeCode; }
    public String getLeaveTypeName() { return leaveTypeName; }
    public void setLeaveTypeName(String leaveTypeName) { this.leaveTypeName = leaveTypeName; }
    public LocalDate getAsOfDate() { return asOfDate; }
    public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
    public BigDecimal getAnnualRate() { return annualRate; }
    public void setAnnualRate(BigDecimal annualRate) { this.annualRate = annualRate; }
    public BigDecimal getEntitledToDate() { return entitledToDate; }
    public void setEntitledToDate(BigDecimal entitledToDate) { this.entitledToDate = entitledToDate; }
    public BigDecimal getAdjustments() { return adjustments; }
    public void setAdjustments(BigDecimal adjustments) { this.adjustments = adjustments; }
    public BigDecimal getUsedApproved() { return usedApproved; }
    public void setUsedApproved(BigDecimal usedApproved) { this.usedApproved = usedApproved; }
    public BigDecimal getUsedTimesheet() { return usedTimesheet; }
    public void setUsedTimesheet(BigDecimal usedTimesheet) { this.usedTimesheet = usedTimesheet; }
    public BigDecimal getPending() { return pending; }
    public void setPending(BigDecimal pending) { this.pending = pending; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
