package com.hrms.reference.repository;

import com.hrms.reference.domain.CalendarDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CalendarDayRepository extends JpaRepository<CalendarDay, UUID> {

    List<CalendarDay> findByCalendarIdOrderByDayDate(UUID calendarId);

    List<CalendarDay> findByCalendarIdAndDayDateBetween(UUID calendarId, LocalDate from, LocalDate to);
}
