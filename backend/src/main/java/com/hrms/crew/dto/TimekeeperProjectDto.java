package com.hrms.crew.dto;

import java.util.UUID;

public class TimekeeperProjectDto {

    private UUID id;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private UUID projectId;
    private String projectCode;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
