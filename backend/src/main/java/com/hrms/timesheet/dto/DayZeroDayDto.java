package com.hrms.timesheet.dto;

import java.time.LocalDate;
import java.util.UUID;

/** One "estimated" day (Day Zero) available for a direct correction. */
public class DayZeroDayDto {
    private UUID id;
    private LocalDate workDate;
    private int periodYear;
    private int periodMonth;
    private String timeTypeCode;
    private String timeTypeName;

    public DayZeroDayDto() { }

    public DayZeroDayDto(UUID id, LocalDate workDate, int periodYear, int periodMonth,
                         String timeTypeCode, String timeTypeName) {
        this.id = id;
        this.workDate = workDate;
        this.periodYear = periodYear;
        this.periodMonth = periodMonth;
        this.timeTypeCode = timeTypeCode;
        this.timeTypeName = timeTypeName;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public int getPeriodYear() { return periodYear; }
    public void setPeriodYear(int periodYear) { this.periodYear = periodYear; }
    public int getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(int periodMonth) { this.periodMonth = periodMonth; }
    public String getTimeTypeCode() { return timeTypeCode; }
    public void setTimeTypeCode(String timeTypeCode) { this.timeTypeCode = timeTypeCode; }
    public String getTimeTypeName() { return timeTypeName; }
    public void setTimeTypeName(String timeTypeName) { this.timeTypeName = timeTypeName; }
}
