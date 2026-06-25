package com.hrms.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport object for {@link com.hrms.payroll.domain.PayrollComponent}.
 */
public class PayrollComponentDto {

    private UUID id;
    private UUID companyId;

    @NotBlank
    @Size(max = 50)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 30)
    private String category;

    @NotBlank
    @Size(max = 20)
    private String componentType;

    private String paymentFrequency = "MONTHLY";
    private String calculationMethod = "FIXED";
    private String roundingMethod = "HALF_UP";
    private int roundingScale = 2;

    @Size(min = 3, max = 3)
    private String currencyCode;

    private int priority = 100;
    private boolean taxable = false;
    private boolean insurable = false;
    private boolean wpsIncluded = true;
    private boolean eosIncluded = false;
    private boolean provisionIncluded = false;
    private boolean leaveIncluded = false;
    private boolean visibleOnPayslip = true;
    private boolean visibleOnReports = true;
    private boolean costAllocationRequired = false;
    private boolean approvalRequired = false;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;
    private String status = "ACTIVE";

    @Size(max = 500)
    private String remarks;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
