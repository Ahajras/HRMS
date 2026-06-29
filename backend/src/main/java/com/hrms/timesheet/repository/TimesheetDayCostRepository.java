package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimesheetDayCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimesheetDayCostRepository extends JpaRepository<TimesheetDayCost, UUID> {

    List<TimesheetDayCost> findByTimesheetDayId(UUID timesheetDayId);

    void deleteByTimesheetDayId(UUID timesheetDayId);
}
