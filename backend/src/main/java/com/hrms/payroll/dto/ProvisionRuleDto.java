package com.hrms.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ProvisionRuleDto {
    private UUID id;
    private UUID projectId;
    private String payGroup = "ALL";
    private String provisionType;
    private String name;
    private String basisMode = "COMPONENT_FLAGS";
    private String basisCategories;
    private String basisComponentCodes;
    private String formulaExpression;
    private BigDecimal divisor = BigDecimal.valueOf(365);
    private BigDecimal fixedAmount = BigDecimal.ZERO;
    private BigDecimal entitlementDaysUnderFive = BigDecimal.valueOf(21);
    private BigDecimal entitlementDaysFiveOrMore = BigDecimal.valueOf(28);
    private int ticketCycleMonths = 12;
    private BigDecimal ticketQuantity = BigDecimal.ONE;
    private int ticketExpiryMonths = 0;
    private String ticketEntitlementMode = "ON_CYCLE_DATE";
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status = "ACTIVE";
    private String notes;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
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
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
