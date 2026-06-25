package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import com.hrms.common.domain.EffectiveDated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Master definition of a salary component (FTDD Vol.1 Ch.6: salary is a
 * collection of components; nothing is special). Calculation behaviour
 * (FIXED / PERCENTAGE / FORMULA) is resolved by the Rule Engine in Phase 3.
 */
@Entity
@Table(name = "payroll_component")
public class PayrollComponent extends AuditableEntity implements EffectiveDated {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "component_type", nullable = false, length = 20)
    private String componentType;

    @Column(name = "payment_frequency", nullable = false, length = 20)
    private String paymentFrequency = "MONTHLY";

    @Column(name = "calculation_method", nullable = false, length = 20)
    private String calculationMethod = "FIXED";

    @Column(name = "rounding_method", nullable = false, length = 20)
    private String roundingMethod = "HALF_UP";

    @Column(name = "rounding_scale", nullable = false)
    private int roundingScale = 2;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Column(name = "taxable", nullable = false)
    private boolean taxable = false;

    @Column(name = "insurable", nullable = false)
    private boolean insurable = false;

    @Column(name = "wps_included", nullable = false)
    private boolean wpsIncluded = true;

    @Column(name = "eos_included", nullable = false)
    private boolean eosIncluded = false;

    @Column(name = "provision_included", nullable = false)
    private boolean provisionIncluded = false;

    @Column(name = "leave_included", nullable = false)
    private boolean leaveIncluded = false;

    @Column(name = "visible_on_payslip", nullable = false)
    private boolean visibleOnPayslip = true;

    @Column(name = "visible_on_reports", nullable = false)
    private boolean visibleOnReports = true;

    @Column(name = "cost_allocation_required", nullable = false)
    private boolean costAllocationRequired = false;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired = false;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "remarks", length = 500)
    private String remarks;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getComponentType() { return componentType; }
    public void setComponentType(String componentType) { this.componentType = componentType; }

    public String getPaymentFrequency() { return paymentFrequency; }
    public void setPaymentFrequency(String paymentFrequency) { this.paymentFrequency = paymentFrequency; }

    public String getCalculationMethod() { return calculationMethod; }
    public void setCalculationMethod(String calculationMethod) { this.calculationMethod = calculationMethod; }

    public String getRoundingMethod() { return roundingMethod; }
    public void setRoundingMethod(String roundingMethod) { this.roundingMethod = roundingMethod; }

    public int getRoundingScale() { return roundingScale; }
    public void setRoundingScale(int roundingScale) { this.roundingScale = roundingScale; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isTaxable() { return taxable; }
    public void setTaxable(boolean taxable) { this.taxable = taxable; }

    public boolean isInsurable() { return insurable; }
    public void setInsurable(boolean insurable) { this.insurable = insurable; }

    public boolean isWpsIncluded() { return wpsIncluded; }
    public void setWpsIncluded(boolean wpsIncluded) { this.wpsIncluded = wpsIncluded; }

    public boolean isEosIncluded() { return eosIncluded; }
    public void setEosIncluded(boolean eosIncluded) { this.eosIncluded = eosIncluded; }

    public boolean isProvisionIncluded() { return provisionIncluded; }
    public void setProvisionIncluded(boolean provisionIncluded) { this.provisionIncluded = provisionIncluded; }

    public boolean isLeaveIncluded() { return leaveIncluded; }
    public void setLeaveIncluded(boolean leaveIncluded) { this.leaveIncluded = leaveIncluded; }

    public boolean isVisibleOnPayslip() { return visibleOnPayslip; }
    public void setVisibleOnPayslip(boolean visibleOnPayslip) { this.visibleOnPayslip = visibleOnPayslip; }

    public boolean isVisibleOnReports() { return visibleOnReports; }
    public void setVisibleOnReports(boolean visibleOnReports) { this.visibleOnReports = visibleOnReports; }

    public boolean isCostAllocationRequired() { return costAllocationRequired; }
    public void setCostAllocationRequired(boolean costAllocationRequired) { this.costAllocationRequired = costAllocationRequired; }

    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }

    @Override
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    @Override
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
