package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class TimekeeperDayDto {
    private UUID employeeId;
    private String employeeNumber;
    private String employeeName;
    private UUID timesheetId;
    private UUID timesheetDayId;
    private LocalDate workDate;
    private String timesheetStatus;
    private String shiftCode;
    private String shiftName;
    private LocalTime plannedIn;
    private LocalTime plannedOut;
    private LocalTime actualIn;
    private LocalTime actualOut;
    private String timeTypeCode;
    private BigDecimal plannedHours = BigDecimal.ZERO;
    private BigDecimal workedHours = BigDecimal.ZERO;
    private BigDecimal normalHours = BigDecimal.ZERO;
    private BigDecimal otHours = BigDecimal.ZERO;
    private boolean editable;
    private String blockedReason;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public UUID getTimesheetId() { return timesheetId; }
    public void setTimesheetId(UUID timesheetId) { this.timesheetId = timesheetId; }
    public UUID getTimesheetDayId() { return timesheetDayId; }
    public void setTimesheetDayId(UUID timesheetDayId) { this.timesheetDayId = timesheetDayId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public String getTimesheetStatus() { return timesheetStatus; }
    public void setTimesheetStatus(String timesheetStatus) { this.timesheetStatus = timesheetStatus; }
    public String getShiftCode() { return shiftCode; }
    public void setShiftCode(String shiftCode) { this.shiftCode = shiftCode; }
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    public LocalTime getPlannedIn() { return plannedIn; }
    public void setPlannedIn(LocalTime plannedIn) { this.plannedIn = plannedIn; }
    public LocalTime getPlannedOut() { return plannedOut; }
    public void setPlannedOut(LocalTime plannedOut) { this.plannedOut = plannedOut; }
    public LocalTime getActualIn() { return actualIn; }
    public void setActualIn(LocalTime actualIn) { this.actualIn = actualIn; }
    public LocalTime getActualOut() { return actualOut; }
    public void setActualOut(LocalTime actualOut) { this.actualOut = actualOut; }
    public String getTimeTypeCode() { return timeTypeCode; }
    public void setTimeTypeCode(String timeTypeCode) { this.timeTypeCode = timeTypeCode; }
    public BigDecimal getPlannedHours() { return plannedHours; }
    public void setPlannedHours(BigDecimal plannedHours) { this.plannedHours = plannedHours; }
    public BigDecimal getWorkedHours() { return workedHours; }
    public void setWorkedHours(BigDecimal workedHours) { this.workedHours = workedHours; }
    public BigDecimal getNormalHours() { return normalHours; }
    public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }
    public BigDecimal getOtHours() { return otHours; }
    public void setOtHours(BigDecimal otHours) { this.otHours = otHours; }
    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) { this.editable = editable; }
    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
}
