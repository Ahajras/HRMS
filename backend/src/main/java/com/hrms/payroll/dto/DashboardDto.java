package com.hrms.payroll.dto;

import java.math.BigDecimal;

public class DashboardDto {
    private int projectCount;
    private int activeEmployeeCount;
    private int periodYear;
    private int periodMonth;

    // Today
    private int presentToday;
    private int onLeaveToday;
    private int absentToday;
    private int notMarkedToday;

    // This month so far
    private int presentDaysMonth;
    private int leaveDaysMonth;
    private int absentDaysMonth;

    // Latest LOCKED payroll for the reference period
    private BigDecimal netDisbursed = BigDecimal.ZERO;
    private BigDecimal totalAllowances = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    private int payslipCount;
    private boolean periodLocked;

    public int getProjectCount() { return projectCount; }
    public void setProjectCount(int projectCount) { this.projectCount = projectCount; }
    public int getActiveEmployeeCount() { return activeEmployeeCount; }
    public void setActiveEmployeeCount(int activeEmployeeCount) { this.activeEmployeeCount = activeEmployeeCount; }
    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
    public int getPresentToday() { return presentToday; }
    public void setPresentToday(int presentToday) { this.presentToday = presentToday; }
    public int getOnLeaveToday() { return onLeaveToday; }
    public void setOnLeaveToday(int onLeaveToday) { this.onLeaveToday = onLeaveToday; }
    public int getAbsentToday() { return absentToday; }
    public void setAbsentToday(int absentToday) { this.absentToday = absentToday; }
    public int getNotMarkedToday() { return notMarkedToday; }
    public void setNotMarkedToday(int notMarkedToday) { this.notMarkedToday = notMarkedToday; }
    public int getPresentDaysMonth() { return presentDaysMonth; }
    public void setPresentDaysMonth(int presentDaysMonth) { this.presentDaysMonth = presentDaysMonth; }
    public int getLeaveDaysMonth() { return leaveDaysMonth; }
    public void setLeaveDaysMonth(int leaveDaysMonth) { this.leaveDaysMonth = leaveDaysMonth; }
    public int getAbsentDaysMonth() { return absentDaysMonth; }
    public void setAbsentDaysMonth(int absentDaysMonth) { this.absentDaysMonth = absentDaysMonth; }
    public BigDecimal getNetDisbursed() { return netDisbursed; }
    public void setNetDisbursed(BigDecimal netDisbursed) { this.netDisbursed = netDisbursed; }
    public BigDecimal getTotalAllowances() { return totalAllowances; }
    public void setTotalAllowances(BigDecimal totalAllowances) { this.totalAllowances = totalAllowances; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public int getPayslipCount() { return payslipCount; }
    public void setPayslipCount(int payslipCount) { this.payslipCount = payslipCount; }
    public boolean isPeriodLocked() { return periodLocked; }
    public void setPeriodLocked(boolean periodLocked) { this.periodLocked = periodLocked; }
}
