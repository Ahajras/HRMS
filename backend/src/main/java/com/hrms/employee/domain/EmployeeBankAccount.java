package com.hrms.employee.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Employee bank account used for salary disbursement / WPS export. An employee
 * may hold several accounts; exactly one is normally flagged {@code primary}.
 */
@Entity
@Table(name = "employee_bank_account")
public class EmployeeBankAccount extends AuditableEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "bank_id")
    private UUID bankId;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = true;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public UUID getBankId() { return bankId; }
    public void setBankId(UUID bankId) { this.bankId = bankId; }

    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
