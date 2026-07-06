package com.hrms.timesheet.dto;

import java.util.UUID;

public class TimesheetProjectSummaryDto {

    private UUID projectId;
    private String projectCode;
    private String projectName;
    private long eligible;
    private long generated;
    private long missing;
    private long draft;
    private long submitted;
    private long approved;
    private long locked;

    public TimesheetProjectSummaryDto(UUID projectId, String projectCode, String projectName,
                                      long eligible, long generated, long missing,
                                      long draft, long submitted, long approved, long locked) {
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.eligible = eligible;
        this.generated = generated;
        this.missing = missing;
        this.draft = draft;
        this.submitted = submitted;
        this.approved = approved;
        this.locked = locked;
    }

    public UUID getProjectId() { return projectId; }
    public String getProjectCode() { return projectCode; }
    public String getProjectName() { return projectName; }
    public long getEligible() { return eligible; }
    public long getGenerated() { return generated; }
    public long getMissing() { return missing; }
    public long getDraft() { return draft; }
    public long getSubmitted() { return submitted; }
    public long getApproved() { return approved; }
    public long getLocked() { return locked; }
}
