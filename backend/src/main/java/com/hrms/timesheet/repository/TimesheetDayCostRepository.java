package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimesheetDayCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TimesheetDayCostRepository extends JpaRepository<TimesheetDayCost, UUID> {

    List<TimesheetDayCost> findByTimesheetDayId(UUID timesheetDayId);

    /** Batch variant — fetch cost splits for many days in ONE query (avoids the
     * N+1 pattern that made bulk timesheet operations crawl earlier today). */
    List<TimesheetDayCost> findByTimesheetDayIdIn(Collection<UUID> timesheetDayIds);

    /** Scoped by TIMESHEET id instead of day id — a timesheet covers ~30 days,
     * so this keeps the IN-clause parameter count ~30x smaller. Needed for
     * large runs: passing every day id (thousands x 30) can exceed
     * PostgreSQL's 65,535-parameter-per-statement limit outright. */
    @Query("select tdc from TimesheetDayCost tdc where tdc.timesheetDayId in "
            + "(select td.id from TimesheetDay td where td.timesheetId in :timesheetIds)")
    List<TimesheetDayCost> findByTimesheetIdIn(@Param("timesheetIds") Collection<UUID> timesheetIds);

    void deleteByTimesheetDayId(UUID timesheetDayId);
}
