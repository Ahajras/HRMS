package com.hrms.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport object for {@link com.hrms.employee.domain.Contract}.
 */
public class ContractDto {

    private UUID id;

    @NotNull
    private UUID employeeId;

    @Size(max = 50)
    private String contractNumber;

    @NotBlank
    @Size(max = 30)
    private String contractType;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    @Size(min = 3, max = 3)
    private String baseCurrencyCode;

    private String status = "ACTIVE";

    // Reference-only standard terms (actual worked hours come from the timesheet, Phase 4).
    private BigDecimal workingHoursPerWeek;

    private Integer workingDaysPerWeek;

    @Size(max = 10)
    private String overtimeCategory;

    @Size(max = 60)
    private String overtimeCategoryDesc;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getBaseCurrencyCode() { return baseCurrencyCode; }
    public void setBaseCurrencyCode(String baseCurrencyCode) { this.baseCurrencyCode = baseCurrencyCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getWorkingHoursPerWeek() { return workingHoursPerWeek; }
    public void setWorkingHoursPerWeek(BigDecimal workingHoursPerWeek) { this.workingHoursPerWeek = workingHoursPerWeek; }

    public Integer getWorkingDaysPerWeek() { return workingDaysPerWeek; }
    public void setWorkingDaysPerWeek(Integer workingDaysPerWeek) { this.workingDaysPerWeek = workingDaysPerWeek; }

    public String getOvertimeCategory() { return overtimeCategory; }
    public void setOvertimeCategory(String overtimeCategory) { this.overtimeCategory = overtimeCategory; }

    public String getOvertimeCategoryDesc() { return overtimeCategoryDesc; }
    public void setOvertimeCategoryDesc(String overtimeCategoryDesc) { this.overtimeCategoryDesc = overtimeCategoryDesc; }
}
