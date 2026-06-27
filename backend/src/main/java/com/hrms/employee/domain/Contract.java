package com.hrms.employee.domain;

import com.hrms.common.domain.AuditableEntity;
import com.hrms.common.domain.EffectiveDated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Employment contract for an employee (FTDD Vol.1 Ch.2). Effective-dated so a
 * career can span multiple contracts over time.
 */
@Entity
@Table(name = "contract")
public class Contract extends AuditableEntity implements EffectiveDated {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "contract_number", length = 50)
    private String contractNumber;

    @Column(name = "contract_type", nullable = false, length = 30)
    private String contractType;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "base_currency_code", length = 3)
    private String baseCurrencyCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    // Standard/reference employment terms (NOT actual worked hours — those come
    // from the timesheet/shift in Phase 4). Carried from the legacy employee file.
    @Column(name = "working_hours_per_week")
    private java.math.BigDecimal workingHoursPerWeek;

    @Column(name = "working_days_per_week")
    private Integer workingDaysPerWeek;

    @Column(name = "overtime_category", length = 10)
    private String overtimeCategory;

    @Column(name = "overtime_category_desc", length = 60)
    private String overtimeCategoryDesc;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getContractNumber() { return contractNumber; }
    public void setContractNumber(String contractNumber) { this.contractNumber = contractNumber; }

    public String getContractType() { return contractType; }
    public void setContractType(String contractType) { this.contractType = contractType; }

    @Override
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    @Override
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getBaseCurrencyCode() { return baseCurrencyCode; }
    public void setBaseCurrencyCode(String baseCurrencyCode) { this.baseCurrencyCode = baseCurrencyCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public java.math.BigDecimal getWorkingHoursPerWeek() { return workingHoursPerWeek; }
    public void setWorkingHoursPerWeek(java.math.BigDecimal workingHoursPerWeek) { this.workingHoursPerWeek = workingHoursPerWeek; }

    public Integer getWorkingDaysPerWeek() { return workingDaysPerWeek; }
    public void setWorkingDaysPerWeek(Integer workingDaysPerWeek) { this.workingDaysPerWeek = workingDaysPerWeek; }

    public String getOvertimeCategory() { return overtimeCategory; }
    public void setOvertimeCategory(String overtimeCategory) { this.overtimeCategory = overtimeCategory; }

    public String getOvertimeCategoryDesc() { return overtimeCategoryDesc; }
    public void setOvertimeCategoryDesc(String overtimeCategoryDesc) { this.overtimeCategoryDesc = overtimeCategoryDesc; }
}
