package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class PayrollCategoryRuleDto {
    private UUID id;
    private UUID payrollRuleId;
    private String category;
    private String basis;
    private String divisorMode;
    private BigDecimal monthDivisor;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPayrollRuleId() { return payrollRuleId; }
    public void setPayrollRuleId(UUID payrollRuleId) { this.payrollRuleId = payrollRuleId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
    public String getDivisorMode() { return divisorMode; }
    public void setDivisorMode(String divisorMode) { this.divisorMode = divisorMode; }
    public BigDecimal getMonthDivisor() { return monthDivisor; }
    public void setMonthDivisor(BigDecimal monthDivisor) { this.monthDivisor = monthDivisor; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
