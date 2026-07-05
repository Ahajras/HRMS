package com.hrms.timesheet.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class TimekeeperMarkRequest {
    private UUID employeeId;
    private LocalDate workDate;
    private String action;
    private LocalTime actualIn;
    private LocalTime actualOut;
    private String remarks;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalTime getActualIn() { return actualIn; }
    public void setActualIn(LocalTime actualIn) { this.actualIn = actualIn; }
    public LocalTime getActualOut() { return actualOut; }
    public void setActualOut(LocalTime actualOut) { this.actualOut = actualOut; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
