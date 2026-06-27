package com.hrms.employee.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A family member / dependent (معال) of an employee. Drives family-allowance,
 * air-ticket entitlement and other benefit eligibility. {@code relationship}
 * draws from lookup RELATIONSHIP. An employee may have many dependents.
 */
@Entity
@Table(name = "employee_dependent")
public class EmployeeDependent extends AuditableEntity {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "relationship", length = 30)
    private String relationship;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality_country_code", length = 2)
    private String nationalityCountryCode;

    @Column(name = "is_beneficiary", nullable = false)
    private boolean beneficiary = false;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNationalityCountryCode() { return nationalityCountryCode; }
    public void setNationalityCountryCode(String nationalityCountryCode) { this.nationalityCountryCode = nationalityCountryCode; }

    public boolean isBeneficiary() { return beneficiary; }
    public void setBeneficiary(boolean beneficiary) { this.beneficiary = beneficiary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
