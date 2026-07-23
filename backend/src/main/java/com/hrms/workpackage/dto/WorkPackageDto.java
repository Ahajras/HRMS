package com.hrms.workpackage.dto;

import java.time.LocalDate;
import java.util.UUID;

public class WorkPackageDto {
    private UUID id;
    private UUID companyId;
    private UUID projectId;
    private String projectCode;
    private String projectName;
    private UUID costCodeId;
    private String costCode;
    private String costCodeName;
    private String code;
    private String name;
    private String description;
    private LocalDate plannedStart;
    private LocalDate plannedEnd;
    private String status;
    private int requirementCount;
    private int crewCount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }
    public String getCostCode() { return costCode; }
    public void setCostCode(String costCode) { this.costCode = costCode; }
    public String getCostCodeName() { return costCodeName; }
    public void setCostCodeName(String costCodeName) { this.costCodeName = costCodeName; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDate plannedStart) { this.plannedStart = plannedStart; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDate plannedEnd) { this.plannedEnd = plannedEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRequirementCount() { return requirementCount; }
    public void setRequirementCount(int requirementCount) { this.requirementCount = requirementCount; }
    public int getCrewCount() { return crewCount; }
    public void setCrewCount(int crewCount) { this.crewCount = crewCount; }
}
