package com.hrms.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport object for {@link com.hrms.employee.domain.Employee}.
 */
public class EmployeeDto {

    private UUID id;

    private UUID companyId;

    @NotBlank
    @Size(max = 50)
    private String employeeNumber;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Size(max = 100)
    private String middleName;

    @Size(min = 2, max = 2)
    private String nationalityCountryCode;

    @Size(max = 20)
    private String maritalStatus;

    @Size(max = 255)
    private String addressLine;

    @Size(max = 100)
    private String city;

    @Size(min = 2, max = 2)
    private String countryOfResidenceCode;

    private LocalDate dateOfBirth;

    @Size(max = 10)
    private String gender;

    @NotNull
    private LocalDate hireDate;

    private LocalDate terminationDate;

    @Email
    @Size(max = 150)
    private String email;

    @Size(max = 30)
    private String phone;

    private String status = "ACTIVE";

    @Size(max = 150)
    private String jobTitle;

    @Size(max = 20)
    private String jobTitleCode;

    @Size(max = 30)
    private String payStatus;

    @Size(max = 150)
    private String arabicName;

    private UUID supervisorEmployeeId;
    private String supervisorName;
    private String photoUrl;

    public UUID getSupervisorEmployeeId() { return supervisorEmployeeId; }
    public void setSupervisorEmployeeId(UUID supervisorEmployeeId) { this.supervisorEmployeeId = supervisorEmployeeId; }

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountryOfResidenceCode() { return countryOfResidenceCode; }
    public void setCountryOfResidenceCode(String countryOfResidenceCode) { this.countryOfResidenceCode = countryOfResidenceCode; }

    public String getNationalityCountryCode() { return nationalityCountryCode; }
    public void setNationalityCountryCode(String nationalityCountryCode) { this.nationalityCountryCode = nationalityCountryCode; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getJobTitleCode() { return jobTitleCode; }
    public void setJobTitleCode(String jobTitleCode) { this.jobTitleCode = jobTitleCode; }

    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }

    public String getArabicName() { return arabicName; }
    public void setArabicName(String arabicName) { this.arabicName = arabicName; }
}
