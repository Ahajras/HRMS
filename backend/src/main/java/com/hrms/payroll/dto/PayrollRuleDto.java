package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PayrollRuleDto {
    private UUID id;
    private String payGroup;
    private String payItemBasis;
    private BigDecimal otMultiplier;
    private BigDecimal standardHoursPerDay;
    private BigDecimal restDayOtMultiplier;
    private boolean weeklyRestPaid;
    private BigDecimal monthDivisor;
    private String divisorMode;
    private java.util.UUID projectId;
    private String status;
    private Integer dayZeroCutoffDay;
    private List<PayrollCategoryRuleDto> categoryRules = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
    public String getPayItemBasis() { return payItemBasis; }
    public void setPayItemBasis(String payItemBasis) { this.payItemBasis = payItemBasis; }
    public BigDecimal getOtMultiplier() { return otMultiplier; }
    public void setOtMultiplier(BigDecimal otMultiplier) { this.otMultiplier = otMultiplier; }
    public BigDecimal getStandardHoursPerDay() { return standardHoursPerDay; }
    public void setStandardHoursPerDay(BigDecimal standardHoursPerDay) { this.standardHoursPerDay = standardHoursPerDay; }
    public BigDecimal getRestDayOtMultiplier() { return restDayOtMultiplier; }
    public void setRestDayOtMultiplier(BigDecimal restDayOtMultiplier) { this.restDayOtMultiplier = restDayOtMultiplier; }
    public boolean isWeeklyRestPaid() { return weeklyRestPaid; }
    public void setWeeklyRestPaid(boolean weeklyRestPaid) { this.weeklyRestPaid = weeklyRestPaid; }
    public BigDecimal getMonthDivisor() { return monthDivisor; }
    public void setMonthDivisor(BigDecimal monthDivisor) { this.monthDivisor = monthDivisor; }
    public String getDivisorMode() { return divisorMode; }
    public void setDivisorMode(String divisorMode) { this.divisorMode = divisorMode; }
    public java.util.UUID getProjectId() { return projectId; }
    public void setProjectId(java.util.UUID projectId) { this.projectId = projectId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getDayZeroCutoffDay() { return dayZeroCutoffDay; }
    public void setDayZeroCutoffDay(Integer dayZeroCutoffDay) { this.dayZeroCutoffDay = dayZeroCutoffDay; }
    public List<PayrollCategoryRuleDto> getCategoryRules() { return categoryRules; }
    public void setCategoryRules(List<PayrollCategoryRuleDto> categoryRules) { this.categoryRules = categoryRules; }
}
