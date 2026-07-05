package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimesheetDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetDayRepository extends JpaRepository<TimesheetDay, UUID> {

    List<TimesheetDay> findByTimesheetIdOrderByWorkDate(UUID timesheetId);

    Optional<TimesheetDay> findByTimesheetIdAndWorkDate(UUID timesheetId, LocalDate workDate);

    void deleteByTimesheetId(UUID timesheetId);
}
