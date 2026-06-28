package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimesheetDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimesheetDayRepository extends JpaRepository<TimesheetDay, UUID> {

    List<TimesheetDay> findByTimesheetIdOrderByWorkDate(UUID timesheetId);

    void deleteByTimesheetId(UUID timesheetId);
}
