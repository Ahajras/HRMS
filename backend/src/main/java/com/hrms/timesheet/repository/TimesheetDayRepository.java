package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimesheetDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetDayRepository extends JpaRepository<TimesheetDay, UUID> {

    List<TimesheetDay> findByTimesheetIdOrderByWorkDate(UUID timesheetId);

    List<TimesheetDay> findByTimesheetIdInOrderByTimesheetIdAscWorkDateAsc(Collection<UUID> timesheetIds);

    @Query("""
            select d from TimesheetDay d
            join Timesheet t on t.id = d.timesheetId
            where t.companyId = :companyId
              and t.employeeId in :employeeIds
              and d.workDate >= :fromDate
              and d.workDate < :beforeDate
              and d.timeTypeId in :timeTypeIds
            order by t.employeeId, d.workDate
            """)
    List<TimesheetDay> findEmployeeTimeTypeDaysBefore(@Param("companyId") UUID companyId,
                                                      @Param("employeeIds") Collection<UUID> employeeIds,
                                                      @Param("timeTypeIds") Collection<UUID> timeTypeIds,
                                                      @Param("fromDate") LocalDate fromDate,
                                                      @Param("beforeDate") LocalDate beforeDate);

    Optional<TimesheetDay> findByTimesheetIdAndWorkDate(UUID timesheetId, LocalDate workDate);

    void deleteByTimesheetId(UUID timesheetId);
}
