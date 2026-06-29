package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Per-employee, per-period roll-up of a timesheet. Aggregates the daily records
 * by time-type category so the figures the payroll engine will consume are
 * visible (worked / overtime / absence / leave hours and day counts).
 */
public class TimesheetSummaryDto {

    private UUID timesheetId;
    private UUID employeeId;
    private String employeeName;
    private int periodYear;
    private int periodMonth;
    private String status;

    private BigDecimal normalHours = BigDecimal.ZERO;
    private BigDecimal overtimeHours = BigDecimal.ZERO;
    private BigDecimal workedHours = BigDecimal.ZERO;   // normal + overtime
    private BigDecimal absenceHours = BigDecimal.ZERO;
    private BigDecimal leaveHours = BigDecimal.ZERO;

    private int workedDays;
    private int absenceDays;
    private int leaveDays;
    private int restDays;
    private int holidayDays;
    private int totalDays;

    /** One line per time-type category present on the timesheet. */
    private List<CategoryLine> lines;

    public static class CategoryLine {
        private String category;
        private int days;
        private BigDecimal hours;
        private boolean paid;

        public CategoryLine() {
        }

        public CategoryLine(String category, int days, BigDecimal hours, boolean paid) {
            this.category = category;
            this.days = days;
            this.hours = hours;
            this.paid = paid;
        }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public int getDays() { return days; }
        public void setDays(int days) { this.days = days; }

        public BigDecimal getHours() { return hours; }
        public void setHours(BigDecimal hours) { this.hours = hours; }

        public boolean isPaid() { return paid; }
        public void setPaid(boolean paid) { this.paid = paid; }
    }

    public UUID getTimesheetId() { return timesheetId; }
    public void setTimesheetId(UUID timesheetId) { this.timesheetId = timesheetId; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }

    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getNormalHours() { return normalHours; }
    public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getWorkedHours() { return workedHours; }
    public void setWorkedHours(BigDecimal workedHours) { this.workedHours = workedHours; }

    public BigDecimal getAbsenceHours() { return absenceHours; }
    public void setAbsenceHours(BigDecimal absenceHours) { this.absenceHours = absenceHours; }

    public BigDecimal getLeaveHours() { return leaveHours; }
    public void setLeaveHours(BigDecimal leaveHours) { this.leaveHours = leaveHours; }

    public int getWorkedDays() { return workedDays; }
    public void setWorkedDays(int workedDays) { this.workedDays = workedDays; }

    public int getAbsenceDays() { return absenceDays; }
    public void setAbsenceDays(int absenceDays) { this.absenceDays = absenceDays; }

    public int getLeaveDays() { return leaveDays; }
    public void setLeaveDays(int leaveDays) { this.leaveDays = leaveDays; }

    public int getRestDays() { return restDays; }
    public void setRestDays(int restDays) { this.restDays = restDays; }

    public int getHolidayDays() { return holidayDays; }
    public void setHolidayDays(int holidayDays) { this.holidayDays = holidayDays; }

    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = totalDays; }

    public List<CategoryLine> getLines() { return lines; }
    public void setLines(List<CategoryLine> lines) { this.lines = lines; }
}
