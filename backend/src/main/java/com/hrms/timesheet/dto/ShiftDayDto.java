package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class ShiftDayDto {

    private UUID id;
    private String dayOfWeek;
    private BigDecimal normalHours;
    private BigDecimal declaredOt;
    private boolean weeklyOff;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public BigDecimal getNormalHours() { return normalHours; }
    public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }

    public BigDecimal getDeclaredOt() { return declaredOt; }
    public void setDeclaredOt(BigDecimal declaredOt) { this.declaredOt = declaredOt; }

    public boolean isWeeklyOff() { return weeklyOff; }
    public void setWeeklyOff(boolean weeklyOff) { this.weeklyOff = weeklyOff; }
}
