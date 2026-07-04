package com.hrms.payroll.dto;

import java.math.BigDecimal;
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
    private String status;

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
