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
     * every day alone — the feature is opt-in per project/pay-group.
     * Rest/holiday days ARE included — the day TYPE is known in advance,
     * but whether the employee actually showed up and worked overtime on
     * that rest day is not, so it still needs to be correctable later. */
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
              and (:payGroup = 'ALL' or upper(coalesce(e.pay_status, '')) = :payGroup)
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

    /** Day Zero screen — every estimated day, on an already-locked period,
     * for one employee, most recent first. */
    @Query("""
            select td from TimesheetDay td, Timesheet t
            where t.id = td.timesheetId
              and t.companyId = :companyId
              and t.employeeId = :employeeId
              and td.estimated = true
              and t.status = 'LOCKED'
            order by td.workDate desc
            """)
    List<TimesheetDay> findEstimatedLockedDaysForEmployee(@Param("companyId") UUID companyId,
                                                          @Param("employeeId") UUID employeeId);

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

    @Query(value = """
            select coalesce(sum(
                case
                    when coalesce(td.planned_hours, 0) > 0
                         and greatest(coalesce(td.normal_hours, 0), coalesce(td.planned_hours, 0), coalesce(td.worked_hours, 0)) > 0
                        then greatest(coalesce(td.normal_hours, 0), coalesce(td.planned_hours, 0), coalesce(td.worked_hours, 0)) / td.planned_hours
                    else 1
                end
            ), 0)
            from timesheet_day td
            join timesheet t on t.id = td.timesheet_id
            join time_type tt on tt.id = td.time_type_id
            where t.company_id = :companyId
              and t.employee_id = :employeeId
              and td.time_type_id = :timeTypeId
              and td.work_date <= :asOf
              and td.leave_request_id is null
              and tt.affects_leave = true
            """, nativeQuery = true)
    java.math.BigDecimal sumManualLeaveDays(@Param("companyId") UUID companyId,
                                            @Param("employeeId") UUID employeeId,
                                            @Param("timeTypeId") UUID timeTypeId,
                                            @Param("asOf") LocalDate asOf);

    @Query(value = """
            select coalesce(sum(
                case
                    when coalesce(td.planned_hours, 0) > 0
                         and greatest(coalesce(td.normal_hours, 0), coalesce(td.planned_hours, 0), coalesce(td.worked_hours, 0)) > 0
                        then greatest(coalesce(td.normal_hours, 0), coalesce(td.planned_hours, 0), coalesce(td.worked_hours, 0)) / td.planned_hours
                    else 1
                end
            ), 0)
            from timesheet_day td
            join timesheet t on t.id = td.timesheet_id
            join time_type tt on tt.id = td.time_type_id
            where t.company_id = :companyId
              and t.employee_id = :employeeId
              and td.time_type_id = :timeTypeId
              and td.work_date <= :asOf
              and td.leave_request_id is null
              and (:excludeDayId is null or td.id <> :excludeDayId)
              and tt.affects_leave = true
            """, nativeQuery = true)
    java.math.BigDecimal sumManualLeaveDaysExcludingDay(@Param("companyId") UUID companyId,
                                                        @Param("employeeId") UUID employeeId,
                                                        @Param("timeTypeId") UUID timeTypeId,
                                                        @Param("asOf") LocalDate asOf,
                                                        @Param("excludeDayId") UUID excludeDayId);

    Optional<TimesheetDay> findByTimesheetIdAndWorkDate(UUID timesheetId, LocalDate workDate);

    void deleteByTimesheetId(UUID timesheetId);

    /** Management dashboard — how many distinct employees fall into each
     * attendance bucket for one specific day, aggregated directly in the
     * database (never fetched row-by-row, so this stays fast regardless
     * of company size). */
    @Query(value = """
            select
              count(distinct case when tt.counts_as_worked then td.timesheet_id end) as present,
              count(distinct case when not tt.counts_as_worked
                    and upper(coalesce(tt.category,'')) not in ('REST','HOLIDAY','UNPAID') then td.timesheet_id end) as on_leave,
              count(distinct case when upper(coalesce(tt.category,'')) = 'UNPAID' then td.timesheet_id end) as absent,
              count(distinct td.timesheet_id) as marked
            from timesheet_day td
            join timesheet t on t.id = td.timesheet_id
            join time_type tt on tt.id = td.time_type_id
            where t.company_id = :companyId and td.work_date = :day
            """, nativeQuery = true)
    Object[] dailyAttendanceBreakdown(@Param("companyId") UUID companyId, @Param("day") LocalDate day);

    /** Management dashboard — cumulative day-counts (present/leave/absent)
     * for the whole month so far, aggregated in the database. */
    @Query(value = """
            select
              count(case when tt.counts_as_worked then 1 end) as present_days,
              count(case when not tt.counts_as_worked
                    and upper(coalesce(tt.category,'')) not in ('REST','HOLIDAY','UNPAID') then 1 end) as leave_days,
              count(case when upper(coalesce(tt.category,'')) = 'UNPAID' then 1 end) as absent_days
            from timesheet_day td
            join timesheet t on t.id = td.timesheet_id
            join time_type tt on tt.id = td.time_type_id
            where t.company_id = :companyId and t.period_year = :year and t.period_month = :month
              and td.work_date <= :asOf
            """, nativeQuery = true)
    Object[] monthlyAttendanceBreakdown(@Param("companyId") UUID companyId, @Param("year") int year,
                                       @Param("month") int month, @Param("asOf") LocalDate asOf);
}
