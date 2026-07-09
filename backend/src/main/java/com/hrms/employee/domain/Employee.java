package com.hrms.employee.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Core employee record (FTDD Vol.1 Ch.2). Company-scoped; employment history
 * is captured by {@link Contract} and {@link Assignment} effective-dated rows.
 */
@Entity
@Table(name = "employee")
public class Employee extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "employee_number", nullable = false, length = 50)
    private String employeeNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "nationality_country_code", length = 2)
    private String nationalityCountryCode;

    @Column(name = "marital_status", length = 20)
    private String maritalStatus;

    @Column(name = "address_line", length = 255)
    private String addressLine;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country_of_residence_code", length = 2)
    private String countryOfResidenceCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @Column(name = "job_title_code", length = 20)
    private String jobTitleCode;

    @Column(name = "pay_status", length = 30)
    private String payStatus;

    @Column(name = "overtime_category_code", length = 20)
    private String overtimeCategoryCode;

    @Column(name = "band", length = 20)
    private String band;

    @Column(name = "arabic_name", length = 150)
    private String arabicName;

    @Column(name = "supervisor_employee_id")
    private UUID supervisorEmployeeId;

    @Column(name = "timekeeper_employee_id")
    private UUID timekeeperEmployeeId;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "home_airport_code", length = 20)
    private String homeAirportCode;

    @Column(name = "work_airport_code", length = 20)
    private String workAirportCode;

    public UUID getSupervisorEmployeeId() { return supervisorEmployeeId; }
    public void setSupervisorEmployeeId(UUID supervisorEmployeeId) { this.supervisorEmployeeId = supervisorEmployeeId; }

    public UUID getTimekeeperEmployeeId() { return timekeeperEmployeeId; }
    public void setTimekeeperEmployeeId(UUID timekeeperEmployeeId) { this.timekeeperEmployeeId = timekeeperEmployeeId; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getHomeAirportCode() { return homeAirportCode; }
    public void setHomeAirportCode(String homeAirportCode) { this.homeAirportCode = homeAirportCode; }

    public String getWorkAirportCode() { return workAirportCode; }
    public void setWorkAirportCode(String workAirportCode) { this.workAirportCode = workAirportCode; }

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

    public String getOvertimeCategoryCode() { return overtimeCategoryCode; }
    public void setOvertimeCategoryCode(String overtimeCategoryCode) { this.overtimeCategoryCode = overtimeCategoryCode; }

    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public String getArabicName() { return arabicName; }
    public void setArabicName(String arabicName) { this.arabicName = arabicName; }
}
