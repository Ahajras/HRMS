package com.hrms.employee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Transport object for {@link com.hrms.employee.domain.EmployeeBankAccount}.
 */
public class EmployeeBankAccountDto {

    private UUID id;

    @NotNull
    private UUID employeeId;

    private UUID bankId;

    @Size(max = 150)
    private String accountHolderName;

    @Size(max = 34)
    private String iban;

    @Size(max = 50)
    private String accountNumber;

    @Size(min = 3, max = 3)
    private String currencyCode;

    private boolean primary = true;

    private String status = "ACTIVE";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
