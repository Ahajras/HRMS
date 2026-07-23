package com.hrms.workpackage.dto;

import java.util.UUID;

public class WorkPackageRequirementDto {
    private UUID id;
    private UUID workPackageId;
    private String jobTitleCode;
    private String jobTitleName;
    private int requiredCount;
    private int assignedCount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWorkPackageId() { return workPackageId; }
    public void setWorkPackageId(UUID workPackageId) { this.workPackageId = workPackageId; }
    public String getJobTitleCode() { return jobTitleCode; }
    public void setJobTitleCode(String jobTitleCode) { this.jobTitleCode = jobTitleCode; }
    public String getJobTitleName() { return jobTitleName; }
    public void setJobTitleName(String jobTitleName) { this.jobTitleName = jobTitleName; }
    public int getRequiredCount() { return requiredCount; }
    public void setRequiredCount(int requiredCount) { this.requiredCount = requiredCount; }
    public int getAssignedCount() { return assignedCount; }
    public void setAssignedCount(int assignedCount) { this.assignedCount = assignedCount; }
}
