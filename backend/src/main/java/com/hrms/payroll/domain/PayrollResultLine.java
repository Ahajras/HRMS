package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** A single payslip line (earning or deduction) of a {@link PayrollResult}. */
@Entity
@Table(name = "payroll_result_line")
public class PayrollResultLine extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "result_id", nullable = false)
    private UUID resultId;

    @Column(name = "component_code", length = 50)
    private String componentCode;

    @Column(name = "component_name", nullable = false, length = 150)
    private String componentName;

    @Column(name = "component_type", nullable = false, length = 20)
    private String componentType;

    @Column(name = "category", length = 40)
    private String category;

    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "rate", precision = 18, scale = 4)
    private BigDecimal rate;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "source", nullable = false, length = 30)
    private String source = "PAY_ITEM";

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 100;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getResultId() { return resultId; }
    public void setResultId(UUID resultId) { this.resultId = resultId; }

    public String getComponentCode() { return componentCode; }
    public void setComponentCode(String componentCode) { this.componentCode = componentCode; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
