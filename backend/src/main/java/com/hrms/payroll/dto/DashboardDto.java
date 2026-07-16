package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardDto {
    private int projectCount;
    private int activeEmployeeCount;
    private int periodYear;
    private int periodMonth;
    private boolean isCurrentMonth;

    // Today (only meaningful when the selected period is the current month)
    private int presentToday;
    private int onLeaveToday;
    private int absentToday;
    private int notMarkedToday;

    // The selected period so far (or in full, if it's a past period)
    private int presentDaysMonth;
    private int leaveDaysMonth;
    private int absentDaysMonth;

    // Latest LOCKED payroll for the selected period
    private BigDecimal netDisbursed = BigDecimal.ZERO;
    private BigDecimal totalAllowances = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    private int payslipCount;
    private boolean periodLocked;

    private List<ProjectStat> projectStats = new ArrayList<>();
    private List<CategoryStat> categoryStats = new ArrayList<>();
    private List<ProvisionMonthStat> provisionMonths = new ArrayList<>();

    public static class ProjectStat {
        private String projectCode;
        private String projectName;
        private int headcount;
        private BigDecimal manHours = BigDecimal.ZERO;
        private BigDecimal netPay = BigDecimal.ZERO;

        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public int getHeadcount() { return headcount; }
        public void setHeadcount(int headcount) { this.headcount = headcount; }
        public BigDecimal getManHours() { return manHours; }
        public void setManHours(BigDecimal manHours) { this.manHours = manHours; }
        public BigDecimal getNetPay() { return netPay; }
        public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
    }

    public static class CategoryStat {
        private String category;
        private BigDecimal amount = BigDecimal.ZERO;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class ProvisionMonthStat {
        private int year;
        private int month;
        private String label;
        private int runCount;
        private int employeeCount;
        private BigDecimal accrualAmount = BigDecimal.ZERO;
        private BigDecimal provisionAmount = BigDecimal.ZERO;
        private BigDecimal leaveProvision = BigDecimal.ZERO;
        private BigDecimal eosProvision = BigDecimal.ZERO;
        private BigDecimal ticketProvision = BigDecimal.ZERO;
        private BigDecimal otherProvision = BigDecimal.ZERO;

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getRunCount() { return runCount; }
        public void setRunCount(int runCount) { this.runCount = runCount; }
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        public BigDecimal getAccrualAmount() { return accrualAmount; }
        public void setAccrualAmount(BigDecimal accrualAmount) { this.accrualAmount = accrualAmount; }
        public BigDecimal getProvisionAmount() { return provisionAmount; }
        public void setProvisionAmount(BigDecimal provisionAmount) { this.provisionAmount = provisionAmount; }
        public BigDecimal getLeaveProvision() { return leaveProvision; }
        public void setLeaveProvision(BigDecimal leaveProvision) { this.leaveProvision = leaveProvision; }
        public BigDecimal getEosProvision() { return eosProvision; }
        public void setEosProvision(BigDecimal eosProvision) { this.eosProvision = eosProvision; }
        public BigDecimal getTicketProvision() { return ticketProvision; }
        public void setTicketProvision(BigDecimal ticketProvision) { this.ticketProvision = ticketProvision; }
        public BigDecimal getOtherProvision() { return otherProvision; }
        public void setOtherProvision(BigDecimal otherProvision) { this.otherProvision = otherProvision; }
    }

    public int getProjectCount() { return projectCount; }
    public void setProjectCount(int projectCount) { this.projectCount = projectCount; }
    public int getActiveEmployeeCount() { return activeEmployeeCount; }
    public void setActiveEmployeeCount(int activeEmployeeCount) { this.activeEmployeeCount = activeEmployeeCount; }
    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
    public boolean isCurrentMonth() { return isCurrentMonth; }
    public void setCurrentMonth(boolean currentMonth) { isCurrentMonth = currentMonth; }
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
    public List<ProjectStat> getProjectStats() { return projectStats; }
    public void setProjectStats(List<ProjectStat> projectStats) { this.projectStats = projectStats; }
    public List<CategoryStat> getCategoryStats() { return categoryStats; }
    public void setCategoryStats(List<CategoryStat> categoryStats) { this.categoryStats = categoryStats; }
    public List<ProvisionMonthStat> getProvisionMonths() { return provisionMonths; }
    public void setProvisionMonths(List<ProvisionMonthStat> provisionMonths) { this.provisionMonths = provisionMonths; }
}
