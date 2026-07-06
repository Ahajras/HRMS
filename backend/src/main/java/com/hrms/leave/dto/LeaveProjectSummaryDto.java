package com.hrms.leave.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class LeaveProjectSummaryDto {
    private UUID projectId;
    private String projectCode;
    private String projectName;
    private long total;
    private long pending;
    private long approved;
    private long rejected;
    private BigDecimal approvedDays = BigDecimal.ZERO;

    public LeaveProjectSummaryDto(UUID projectId, String projectCode, String projectName,
                                  long total, long pending, long approved, long rejected,
                                  BigDecimal approvedDays) {
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.total = total;
        this.pending = pending;
        this.approved = approved;
        this.rejected = rejected;
        this.approvedDays = approvedDays != null ? approvedDays : BigDecimal.ZERO;
    }

    public UUID getProjectId() { return projectId; }
    public String getProjectCode() { return projectCode; }
    public String getProjectName() { return projectName; }
    public long getTotal() { return total; }
    public long getPending() { return pending; }
    public long getApproved() { return approved; }
    public long getRejected() { return rejected; }
    public BigDecimal getApprovedDays() { return approvedDays; }
}
