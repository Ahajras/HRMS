package com.hrms.reference.domain;

import com.hrms.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A single day within a {@link Calendar}. Flags drive working-day counts,
 * proration and overtime classification (FTDD Vol.1 Ch.2).
 */
@Entity
@Table(name = "calendar_day")
public class CalendarDay extends BaseEntity {

    @Column(name = "calendar_id", nullable = false)
    private UUID calendarId;

    @Column(name = "day_date", nullable = false)
    private LocalDate dayDate;

    @Column(name = "day_name", length = 20)
    private String dayName;

    @Column(name = "public_holiday", nullable = false)
    private boolean publicHoliday = false;

    @Column(name = "special_holiday", nullable = false)
    private boolean specialHoliday = false;

    @Column(name = "working_day", nullable = false)
    private boolean workingDay = true;

    public UUID getCalendarId() { return calendarId; }
    public void setCalendarId(UUID calendarId) { this.calendarId = calendarId; }

    public LocalDate getDayDate() { return dayDate; }
    public void setDayDate(LocalDate dayDate) { this.dayDate = dayDate; }

    public String getDayName() { return dayName; }
    public void setDayName(String dayName) { this.dayName = dayName; }

    public boolean isPublicHoliday() { return publicHoliday; }
    public void setPublicHoliday(boolean publicHoliday) { this.publicHoliday = publicHoliday; }

    public boolean isSpecialHoliday() { return specialHoliday; }
    public void setSpecialHoliday(boolean specialHoliday) { this.specialHoliday = specialHoliday; }

    public boolean isWorkingDay() { return workingDay; }
    public void setWorkingDay(boolean workingDay) { this.workingDay = workingDay; }
}
