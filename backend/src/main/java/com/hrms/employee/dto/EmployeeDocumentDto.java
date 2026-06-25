package com.hrms.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport object for {@link com.hrms.employee.domain.EmployeeDocument}.
 */
public class EmployeeDocumentDto {

    private UUID id;

    @NotNull
    private UUID employeeId;

    @NotBlank
    @Size(max = 30)
    private String documentType;

    @NotBlank
    @Size(max = 100)
    private String documentNumber;

    @Size(min = 2, max = 2)
    private String issuingCountryCode;

    private LocalDate issueDate;

    private LocalDate expiryDate;

    @Size(max = 150)
    private String issuingAuthority;

    private String status = "ACTIVE";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getIssuingCountryCode() { return issuingCountryCode; }
    public void setIssuingCountryCode(String issuingCountryCode) { this.issuingCountryCode = issuingCountryCode; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getIssuingAuthority() { return issuingAuthority; }
    public void setIssuingAuthority(String issuingAuthority) { this.issuingAuthority = issuingAuthority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
