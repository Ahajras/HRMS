package com.hrms.timesheet.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The "sample week" of a {@link Shift} (FTDD Vol.1 Ch.4 / legacy PAYCAL week).
 * One row per day-of-week (MON..SUN): the planned normal hours, the declared
 * overtime ceiling, and whether the day is the weekly rest day.
 */
@Entity
@Table(name = "shift_day")
public class ShiftDay extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @Column(name = "day_of_week", nullable = false, length = 3)
    private String dayOfWeek;

    @Column(name = "normal_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal normalHours = BigDecimal.ZERO;

    @Column(name = "declared_ot", nullable = false, precision = 5, scale = 2)
    private BigDecimal declaredOt = BigDecimal.ZERO;

    @Column(name = "weekly_off", nullable = false)
    private boolean weeklyOff;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public BigDecimal getNormalHours() { return normalHours; }
    public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }

    public BigDecimal getDeclaredOt() { return declaredOt; }
    public void setDeclaredOt(BigDecimal declaredOt) { this.declaredOt = declaredOt; }

    public boolean isWeeklyOff() { return weeklyOff; }
    public void setWeeklyOff(boolean weeklyOff) { this.weeklyOff = weeklyOff; }
}
