package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PayrollResultDto {
    private UUID id;
    private UUID employeeId;
    private String employeeNumber;
    private String employeeName;
    private String payStatus;
    private BigDecimal workedDays = BigDecimal.ZERO;
    private BigDecimal normalHours = BigDecimal.ZERO;
    private BigDecimal otHours = BigDecimal.ZERO;
    private BigDecimal gross = BigDecimal.ZERO;
    private BigDecimal totalEarnings = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    private BigDecimal net = BigDecimal.ZERO;
    private String status;
    private String message;
    private Integer periodYear;
    private Integer periodMonth;
    private List<PayrollResultLineDto> lines = new ArrayList<>();
    private List<TimeTypeSummaryDto> timeTypeSummary = new ArrayList<>();
    private List<CostSummaryDto> costSummary = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }
    public BigDecimal getWorkedDays() { return workedDays; }
    public void setWorkedDays(BigDecimal workedDays) { this.workedDays = workedDays; }
    public BigDecimal getNormalHours() { return normalHours; }
    public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }
    public BigDecimal getOtHours() { return otHours; }
    public void setOtHours(BigDecimal otHours) { this.otHours = otHours; }
    public BigDecimal getGross() { return gross; }
    public void setGross(BigDecimal gross) { this.gross = gross; }
    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    public BigDecimal getNet() { return net; }
    public void setNet(BigDecimal net) { this.net = net; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Integer getPeriodYear() { return periodYear; }
    public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
    public Integer getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
    public List<PayrollResultLineDto> getLines() { return lines; }
    public void setLines(List<PayrollResultLineDto> lines) { this.lines = lines; }
    public List<TimeTypeSummaryDto> getTimeTypeSummary() { return timeTypeSummary; }
    public void setTimeTypeSummary(List<TimeTypeSummaryDto> timeTypeSummary) { this.timeTypeSummary = timeTypeSummary; }
    public List<CostSummaryDto> getCostSummary() { return costSummary; }
    public void setCostSummary(List<CostSummaryDto> costSummary) { this.costSummary = costSummary; }

    public static class TimeTypeSummaryDto {
        private String code;
        private String name;
        private String category;
        private BigDecimal days = BigDecimal.ZERO;
        private BigDecimal paidHours = BigDecimal.ZERO;
        private BigDecimal unpaidHours = BigDecimal.ZERO;
        private BigDecimal normalOtHours = BigDecimal.ZERO;
        private BigDecimal restOtHours = BigDecimal.ZERO;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public BigDecimal getDays() { return days; }
        public void setDays(BigDecimal days) { this.days = days; }
        public BigDecimal getPaidHours() { return paidHours; }
        public void setPaidHours(BigDecimal paidHours) { this.paidHours = paidHours; }
        public BigDecimal getUnpaidHours() { return unpaidHours; }
        public void setUnpaidHours(BigDecimal unpaidHours) { this.unpaidHours = unpaidHours; }
        public BigDecimal getNormalOtHours() { return normalOtHours; }
        public void setNormalOtHours(BigDecimal normalOtHours) { this.normalOtHours = normalOtHours; }
        public BigDecimal getRestOtHours() { return restOtHours; }
        public void setRestOtHours(BigDecimal restOtHours) { this.restOtHours = restOtHours; }
    }

    public static class CostSummaryDto {
        private String projectCode;
        private String projectName;
        private String costCode;
        private String costName;
        private BigDecimal hours = BigDecimal.ZERO;

        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getCostCode() { return costCode; }
        public void setCostCode(String costCode) { this.costCode = costCode; }
        public String getCostName() { return costName; }
        public void setCostName(String costName) { this.costName = costName; }
        public BigDecimal getHours() { return hours; }
        public void setHours(BigDecimal hours) { this.hours = hours; }
    }
}
