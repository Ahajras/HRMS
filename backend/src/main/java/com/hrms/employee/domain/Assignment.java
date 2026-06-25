package com.hrms.employee.domain;

import com.hrms.common.domain.AuditableEntity;
import com.hrms.common.domain.EffectiveDated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Posting of an employee to an organisation unit (FTDD Vol.1 Ch.2). One
 * employee may hold multiple assignments over a career; history is mandatory
 * and effective-dated.
 */
@Entity
@Table(name = "assignment")
public class Assignment extends AuditableEntity implements EffectiveDated {

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "organization_unit_id", nullable = false)
    private UUID organizationUnitId;

    @Column(name = "position_title", length = 150)
    private String positionTitle;

    @Column(name = "supervisor_employee_id")
    private UUID supervisorEmployeeId;

    @Column(name = "primary_assignment", nullable = false)
    private boolean primaryAssignment = true;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public UUID getOrganizationUnitId() { return organizationUnitId; }
    public void setOrganizationUnitId(UUID organizationUnitId) { this.organizationUnitId = organizationUnitId; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public UUID getSupervisorEmployeeId() { return supervisorEmployeeId; }
    public void setSupervisorEmployeeId(UUID supervisorEmployeeId) { this.supervisorEmployeeId = supervisorEmployeeId; }

    public boolean isPrimaryAssignment() { return primaryAssignment; }
    public void setPrimaryAssignment(boolean primaryAssignment) { this.primaryAssignment = primaryAssignment; }

    @Override
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    @Override
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
