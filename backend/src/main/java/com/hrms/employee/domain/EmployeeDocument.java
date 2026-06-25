package com.hrms.employee.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Identity document held by an employee (passport, national/residence id, visa,
 * labour card, ...). An employee may have many documents, each with its own
 * expiry for renewal tracking. {@code documentType} draws from lookup DOCUMENT_TYPE.
 */
@Entity
@Table(name = "employee_document")
public class EmployeeDocument extends AuditableEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    @Column(name = "issuing_country_code", length = 2)
    private String issuingCountryCode;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "issuing_authority", length = 150)
    private String issuingAuthority;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

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
