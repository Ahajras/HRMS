package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShiftDto {

    private UUID id;
    private UUID companyId;
    private String code;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private int breakMinutes;
    private BigDecimal standardHours;
    private boolean crossesMidnight;
    private String weeklyOff;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
    /** The sample week (per day-of-week normal hours / declared OT / weekly-off). */
    private List<ShiftDayDto> days = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public int getBreakMinutes() { return breakMinutes; }
    public void setBreakMinutes(int breakMinutes) { this.breakMinutes = breakMinutes; }

    public BigDecimal getStandardHours() { return standardHours; }
    public void setStandardHours(BigDecimal standardHours) { this.standardHours = standardHours; }

    public boolean isCrossesMidnight() { return crossesMidnight; }
    public void setCrossesMidnight(boolean crossesMidnight) { this.crossesMidnight = crossesMidnight; }

    public String getWeeklyOff() { return weeklyOff; }
    public void setWeeklyOff(String weeklyOff) { this.weeklyOff = weeklyOff; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<ShiftDayDto> getDays() { return days; }
    public void setDays(List<ShiftDayDto> days) { this.days = days; }
}
