package com.hrms.employee.domain;

import com.hrms.common.domain.AuditableEntity;
import com.hrms.common.domain.EffectiveDated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A single financial line on a contract (e.g. basic salary, housing allowance),
 * chosen from {@code payroll_component}. Effective-dated: a change (such as a
 * raise) creates a new version and supersedes the previous one, which is kept as
 * history with status {@code INACTIVE} (FTDD: history is never overwritten).
 */
@Entity
@Table(name = "contract_pay_item")
public class ContractPayItem extends AuditableEntity implements EffectiveDated {

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "pay_component_id", nullable = false)
    private UUID payComponentId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "remarks", length = 255)
    private String remarks;

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
