package com.hrms.timesheet.dto;

import java.util.UUID;

/** Request to generate (or regenerate) a monthly timesheet for one employee. */
public class GenerateTimesheetRequest {

    private UUID employeeId;
    /** The payroll period this timesheet belongs to (must be OPEN). */
    private UUID periodId;
    private int year;
    private int month;
    private UUID shiftId;
    /** When true, an existing DRAFT timesheet is wiped and regenerated. */
    private boolean overwrite;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public UUID getPeriodId() { return periodId; }
    public void setPeriodId(UUID periodId) { this.periodId = periodId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public boolean isOverwrite() { return overwrite; }
    public void setOverwrite(boolean overwrite) { this.overwrite = overwrite; }
}
