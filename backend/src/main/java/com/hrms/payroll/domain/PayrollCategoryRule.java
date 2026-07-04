package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_category_rule")
public class PayrollCategoryRule extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "payroll_rule_id", nullable = false)
    private UUID payrollRuleId;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "basis", nullable = false, length = 30)
    private String basis = "ACTUAL_PAYABLE";

    @Column(name = "divisor_mode", nullable = false, length = 20)
    private String divisorMode = "INHERIT";

    @Column(name = "month_divisor", precision = 6, scale = 2)
    private BigDecimal monthDivisor;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
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
