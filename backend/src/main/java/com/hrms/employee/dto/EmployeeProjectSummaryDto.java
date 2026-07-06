package com.hrms.employee.dto;

import java.util.UUID;

public class EmployeeProjectSummaryDto {

    private UUID projectId;
    private String projectCode;
    private String projectName;
    private long total;
    private long active;
    private long monthly;
    private long daily;

    public EmployeeProjectSummaryDto(UUID projectId, String projectCode, String projectName,
                                     long total, long active, long monthly, long daily) {
        this.projectId = projectId;
        this.projectCode = projectCode;
        this.projectName = projectName;
        this.total = total;
        this.active = active;
        this.monthly = monthly;
        this.daily = daily;
    }

    public UUID getProjectId() { return projectId; }
    public String getProjectCode() { return projectCode; }
    public String getProjectName() { return projectName; }
    public long getTotal() { return total; }
    public long getActive() { return active; }
    public long getMonthly() { return monthly; }
    public long getDaily() { return daily; }
}
