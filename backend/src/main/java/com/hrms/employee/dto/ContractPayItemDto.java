package com.hrms.employee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport object for {@link com.hrms.employee.domain.ContractPayItem}.
 */
public class ContractPayItemDto {

    private UUID id;

    @NotNull
    private UUID contractId;

    private UUID employeeId;

    @NotNull
    private UUID payComponentId;

    @NotNull
    private BigDecimal amount;

    @Size(min = 3, max = 3)
    private String currencyCode;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String status = "ACTIVE";

    @Size(max = 255)
    private String remarks;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getContractId() { return contractId; }
    public void setContractId(UUID contractId) { this.contractId = contractId; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public UUID getPayComponentId() { return payComponentId; }
    public void setPayComponentId(UUID payComponentId) { this.payComponentId = payComponentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
