package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimesheetDayCost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TimesheetDayCostRepository extends JpaRepository<TimesheetDayCost, UUID> {

    List<TimesheetDayCost> findByTimesheetDayId(UUID timesheetDayId);

    /** Batch variant — fetch cost splits for many days in ONE query (avoids the
     * N+1 pattern that made bulk timesheet operations crawl earlier today). */
    List<TimesheetDayCost> findByTimesheetDayIdIn(Collection<UUID> timesheetDayIds);

    void deleteByTimesheetDayId(UUID timesheetDayId);
}
