package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Header + totals only — no per-employee rows, so this stays small and
 * fast regardless of how many employees the run covers. */
public class PayrollListingSummaryDto {
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
}
