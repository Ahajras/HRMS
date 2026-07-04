package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PayrollListingReportDto {
    private UUID runId;
    private UUID periodId;
    private String periodName;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private UUID projectId;
    private String projectCode;
    private String projectName;
    private String payGroup;
    private String status;
    private int employeeCount;
    private BigDecimal totalBasic = BigDecimal.ZERO;
    private BigDecimal totalAllowances = BigDecimal.ZERO;
    private BigDecimal totalOvertime = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    private BigDecimal totalGross = BigDecimal.ZERO;
    private BigDecimal totalNet = BigDecimal.ZERO;
    private List<String> componentCodes = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();

    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
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
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
    public BigDecimal getTotalBasic() { return totalBasic; }
    public void setTotalBasic(BigDecimal totalBasic) { this.totalBasic = totalBasic; }
    public BigDecimal getTotalAllowances() { return totalAllowances; }
    public void setTotalAllowances(BigDecimal totalAllowances) { this.totalAllowances = totalAllowances; }
    public BigDecimal getTotalOvertime() { return totalOvertime; }
    public void setTotalOvertime(BigDecimal totalOvertime) { this.totalOvertime = totalOvertime; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public BigDecimal getTotalGross() { return totalGross; }
    public void setTotalGross(BigDecimal totalGross) { this.totalGross = totalGross; }
    public BigDecimal getTotalNet() { return totalNet; }
    public void setTotalNet(BigDecimal totalNet) { this.totalNet = totalNet; }
    public List<String> getComponentCodes() { return componentCodes; }
    public void setComponentCodes(List<String> componentCodes) { this.componentCodes = componentCodes; }
    public List<Row> getRows() { return rows; }
    public void setRows(List<Row> rows) { this.rows = rows; }

    public static class Row {
        private UUID employeeId;
        private String employeeNumber;
        private String employeeName;
        private String payGroup;
        private UUID projectId;
        private String projectCode;
        private String projectName;
        private UUID costCodeId;
        private String costCode;
        private String costCodeName;
        private BigDecimal workedDays = BigDecimal.ZERO;
        private BigDecimal normalHours = BigDecimal.ZERO;
        private BigDecimal otHours = BigDecimal.ZERO;
        private BigDecimal basic = BigDecimal.ZERO;
        private BigDecimal allowances = BigDecimal.ZERO;
        private BigDecimal overtime = BigDecimal.ZERO;
        private BigDecimal deductions = BigDecimal.ZERO;
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;
        private String status;
        private String message;
        private Map<String, BigDecimal> componentAmounts = new LinkedHashMap<>();

        public UUID getEmployeeId() { return employeeId; }
        public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
        public String getEmployeeNumber() { return employeeNumber; }
        public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public String getPayGroup() { return payGroup; }
        public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
        public UUID getProjectId() { return projectId; }
        public void setProjectId(UUID projectId) { this.projectId = projectId; }
        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public UUID getCostCodeId() { return costCodeId; }
        public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }
        public String getCostCode() { return costCode; }
        public void setCostCode(String costCode) { this.costCode = costCode; }
        public String getCostCodeName() { return costCodeName; }
        public void setCostCodeName(String costCodeName) { this.costCodeName = costCodeName; }
        public BigDecimal getWorkedDays() { return workedDays; }
        public void setWorkedDays(BigDecimal workedDays) { this.workedDays = workedDays; }
        public BigDecimal getNormalHours() { return normalHours; }
        public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }
        public BigDecimal getOtHours() { return otHours; }
        public void setOtHours(BigDecimal otHours) { this.otHours = otHours; }
        public BigDecimal getBasic() { return basic; }
        public void setBasic(BigDecimal basic) { this.basic = basic; }
        public BigDecimal getAllowances() { return allowances; }
        public void setAllowances(BigDecimal allowances) { this.allowances = allowances; }
        public BigDecimal getOvertime() { return overtime; }
        public void setOvertime(BigDecimal overtime) { this.overtime = overtime; }
        public BigDecimal getDeductions() { return deductions; }
        public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
        public BigDecimal getGross() { return gross; }
        public void setGross(BigDecimal gross) { this.gross = gross; }
        public BigDecimal getNet() { return net; }
        public void setNet(BigDecimal net) { this.net = net; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, BigDecimal> getComponentAmounts() { return componentAmounts; }
        public void setComponentAmounts(Map<String, BigDecimal> componentAmounts) { this.componentAmounts = componentAmounts; }
    }
}
