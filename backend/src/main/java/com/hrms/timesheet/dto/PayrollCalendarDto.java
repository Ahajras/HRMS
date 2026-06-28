package com.hrms.timesheet.dto;

import java.util.UUID;

public class PayrollCalendarDto {

    private UUID id;
    private UUID companyId;
    private String code;
    private String name;
    private String frequency;
    private String weekStart;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
