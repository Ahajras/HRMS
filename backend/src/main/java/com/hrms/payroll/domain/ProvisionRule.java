package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import com.hrms.common.domain.EffectiveDated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "provision_rule")
public class ProvisionRule extends AuditableEntity implements EffectiveDated {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "pay_group", nullable = false, length = 30)
    private String payGroup = "ALL";

    @Column(name = "provision_type", nullable = false, length = 30)
    private String provisionType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "basis_mode", nullable = false, length = 30)
    private String basisMode = "COMPONENT_FLAGS";

    @Column(name = "basis_categories", length = 500)
    private String basisCategories;

    @Column(name = "basis_component_codes", length = 1000)
    private String basisComponentCodes;

    @Column(name = "formula_expression", nullable = false, length = 1000)
    private String formulaExpression;

    @Column(name = "divisor", nullable = false, precision = 18, scale = 4)
    private BigDecimal divisor = BigDecimal.valueOf(365);

    @Column(name = "fixed_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal fixedAmount = BigDecimal.ZERO;

    @Column(name = "entitlement_days_under_five", nullable = false, precision = 18, scale = 4)
    private BigDecimal entitlementDaysUnderFive = BigDecimal.valueOf(21);

    @Column(name = "entitlement_days_five_or_more", nullable = false, precision = 18, scale = 4)
    private BigDecimal entitlementDaysFiveOrMore = BigDecimal.valueOf(28);

    @Column(name = "ticket_cycle_months", nullable = false)
    private int ticketCycleMonths = 12;

    @Column(name = "ticket_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal ticketQuantity = BigDecimal.ONE;

    @Column(name = "ticket_expiry_months", nullable = false)
    private int ticketExpiryMonths = 0;

    @Column(name = "ticket_entitlement_mode", nullable = false, length = 30)
    private String ticketEntitlementMode = "ON_CYCLE_DATE";

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "notes", length = 500)
    private String notes;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
    public String getProvisionType() { return provisionType; }
    public void setProvisionType(String provisionType) { this.provisionType = provisionType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBasisMode() { return basisMode; }
    public void setBasisMode(String basisMode) { this.basisMode = basisMode; }
    public String getBasisCategories() { return basisCategories; }
    public void setBasisCategories(String basisCategories) { this.basisCategories = basisCategories; }
    public String getBasisComponentCodes() { return basisComponentCodes; }
    public void setBasisComponentCodes(String basisComponentCodes) { this.basisComponentCodes = basisComponentCodes; }
    public String getFormulaExpression() { return formulaExpression; }
    public void setFormulaExpression(String formulaExpression) { this.formulaExpression = formulaExpression; }
    public BigDecimal getDivisor() { return divisor; }
    public void setDivisor(BigDecimal divisor) { this.divisor = divisor; }
    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
    public BigDecimal getEntitlementDaysUnderFive() { return entitlementDaysUnderFive; }
    public void setEntitlementDaysUnderFive(BigDecimal entitlementDaysUnderFive) { this.entitlementDaysUnderFive = entitlementDaysUnderFive; }
    public BigDecimal getEntitlementDaysFiveOrMore() { return entitlementDaysFiveOrMore; }
    public void setEntitlementDaysFiveOrMore(BigDecimal entitlementDaysFiveOrMore) { this.entitlementDaysFiveOrMore = entitlementDaysFiveOrMore; }
    public int getTicketCycleMonths() { return ticketCycleMonths; }
    public void setTicketCycleMonths(int ticketCycleMonths) { this.ticketCycleMonths = ticketCycleMonths; }
    public BigDecimal getTicketQuantity() { return ticketQuantity; }
    public void setTicketQuantity(BigDecimal ticketQuantity) { this.ticketQuantity = ticketQuantity; }
    public int getTicketExpiryMonths() { return ticketExpiryMonths; }
    public void setTicketExpiryMonths(int ticketExpiryMonths) { this.ticketExpiryMonths = ticketExpiryMonths; }
    public String getTicketEntitlementMode() { return ticketEntitlementMode; }
    public void setTicketEntitlementMode(String ticketEntitlementMode) { this.ticketEntitlementMode = ticketEntitlementMode; }
    @Override
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    @Override
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
