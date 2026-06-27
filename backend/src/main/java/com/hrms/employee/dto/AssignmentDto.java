package com.hrms.employee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Transport object for {@link com.hrms.employee.domain.Assignment}.
 */
public class AssignmentDto {

    private UUID id;

    @NotNull
    private UUID employeeId;

    @NotNull
    private UUID organizationUnitId;

    @Size(max = 150)
    private String positionTitle;

    private UUID supervisorEmployeeId;

    private UUID projectId;

    private UUID costCodeId;

    private boolean primaryAssignment = true;

    @NotNull
    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private String status = "ACTIVE";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public UUID getOrganizationUnitId() { return organizationUnitId; }
    public void setOrganizationUnitId(UUID organizationUnitId) { this.organizationUnitId = organizationUnitId; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public UUID getSupervisorEmployeeId() { return supervisorEmployeeId; }
    public void setSupervisorEmployeeId(UUID supervisorEmployeeId) { this.supervisorEmployeeId = supervisorEmployeeId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }

    public boolean isPrimaryAssignment() { return primaryAssignment; }
    public void setPrimaryAssignment(boolean primaryAssignment) { this.primaryAssignment = primaryAssignment; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
