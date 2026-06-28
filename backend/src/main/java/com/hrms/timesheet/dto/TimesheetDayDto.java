package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class TimesheetDayDto {

    private UUID id;
    private UUID timesheetId;
    private LocalDate workDate;
    private UUID shiftId;
    private UUID timeTypeId;
    private String timeTypeCode;
    private BigDecimal plannedHours;
    private LocalTime actualIn;
    private LocalTime actualOut;
    private BigDecimal workedHours;
    private BigDecimal otHours;
    private UUID projectId;
    private UUID costCodeId;
    private String remarks;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTimesheetId() { return timesheetId; }
    public void setTimesheetId(UUID timesheetId) { this.timesheetId = timesheetId; }

    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public UUID getTimeTypeId() { return timeTypeId; }
    public void setTimeTypeId(UUID timeTypeId) { this.timeTypeId = timeTypeId; }

    public String getTimeTypeCode() { return timeTypeCode; }
    public void setTimeTypeCode(String timeTypeCode) { this.timeTypeCode = timeTypeCode; }

    public BigDecimal getPlannedHours() { return plannedHours; }
    public void setPlannedHours(BigDecimal plannedHours) { this.plannedHours = plannedHours; }

    public LocalTime getActualIn() { return actualIn; }
    public void setActualIn(LocalTime actualIn) { this.actualIn = actualIn; }

    public LocalTime getActualOut() { return actualOut; }
    public void setActualOut(LocalTime actualOut) { this.actualOut = actualOut; }

    public BigDecimal getWorkedHours() { return workedHours; }
    public void setWorkedHours(BigDecimal workedHours) { this.workedHours = workedHours; }

    public BigDecimal getOtHours() { return otHours; }
    public void setOtHours(BigDecimal otHours) { this.otHours = otHours; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
