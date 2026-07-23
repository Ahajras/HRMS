package com.hrms.crew.dto;

import java.util.UUID;

public class CrewDto {

    private UUID id;
    private UUID companyId;
    private String code;
    private String name;
    private UUID projectId;
    private String projectCode;
    private UUID foremanEmployeeId;
    private String foremanName;
    private UUID supervisorEmployeeId;
    private String supervisorName;
    private UUID timekeeperEmployeeId;
    private String timekeeperName;
    private UUID parentCrewId;
    private String status;
    private int memberCount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public UUID getForemanEmployeeId() { return foremanEmployeeId; }
    public void setForemanEmployeeId(UUID foremanEmployeeId) { this.foremanEmployeeId = foremanEmployeeId; }

    public String getForemanName() { return foremanName; }
    public void setForemanName(String foremanName) { this.foremanName = foremanName; }

    public UUID getSupervisorEmployeeId() { return supervisorEmployeeId; }
    public void setSupervisorEmployeeId(UUID supervisorEmployeeId) { this.supervisorEmployeeId = supervisorEmployeeId; }

    public String getSupervisorName() { return supervisorName; }
    public void setSupervisorName(String supervisorName) { this.supervisorName = supervisorName; }

    public UUID getTimekeeperEmployeeId() { return timekeeperEmployeeId; }
    public void setTimekeeperEmployeeId(UUID timekeeperEmployeeId) { this.timekeeperEmployeeId = timekeeperEmployeeId; }

    public String getTimekeeperName() { return timekeeperName; }
    public void setTimekeeperName(String timekeeperName) { this.timekeeperName = timekeeperName; }

    public UUID getParentCrewId() { return parentCrewId; }
    public void setParentCrewId(UUID parentCrewId) { this.parentCrewId = parentCrewId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}
