package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimesheetDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Day Zero — marks any day past the project/pay-group's configured
     * cutoff day as "estimated" right before its timesheet gets locked. A
     * rule with no cutoff configured (day_zero_cutoff_day is null) leaves
     * every day alone — the feature is opt-in per project/pay-group. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update timesheet_day td
            set estimated = true
            from timesheet t, employee e, assignment a
            where t.id = td.timesheet_id
              and e.id = t.employee_id
              and a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
              and t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status = 'APPROVED'
              and a.project_id = :projectId
              and (:payGroup = 'ALL' or e.pay_status = :payGroup)
              and extract(day from td.work_date)::int > coalesce(
                    (select r.day_zero_cutoff_day from payroll_rule r
                       where r.company_id = :companyId and r.project_id = :projectId
                         and r.pay_group = e.pay_status and r.status = 'ACTIVE' limit 1),
                    (select r.day_zero_cutoff_day from payroll_rule r
                       where r.company_id = :companyId and r.project_id is null
                         and r.pay_group = e.pay_status and r.status = 'ACTIVE' limit 1),
                    999)
            """, nativeQuery = true)
    int markEstimatedForProjectLock(@Param("companyId") UUID companyId,
                                    @Param("year") int year,
                                    @Param("month") int month,
                                    @Param("projectId") UUID projectId,
                                    @Param("payGroup") String payGroup);

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
