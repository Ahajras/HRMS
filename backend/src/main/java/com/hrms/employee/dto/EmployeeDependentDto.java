package com.hrms.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport object for {@link com.hrms.employee.domain.EmployeeDependent}.
 */
public class EmployeeDependentDto {

    private UUID id;

    @NotNull
    private UUID employeeId;

    @NotBlank
    @Size(max = 150)
    private String fullName;

    @Size(max = 30)
    private String relationship;

    @Size(max = 10)
    private String gender;

    private LocalDate dateOfBirth;

    @Size(min = 2, max = 2)
    private String nationalityCountryCode;

    private boolean beneficiary;

    private String status = "ACTIVE";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
