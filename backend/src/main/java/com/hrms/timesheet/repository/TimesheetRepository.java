package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.Timesheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetRepository extends JpaRepository<Timesheet, UUID> {

    Optional<Timesheet> findByCompanyIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
            UUID companyId, UUID employeeId, int periodYear, int periodMonth);

    List<Timesheet> findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(
            UUID companyId, int periodYear, int periodMonth);

    /** Scoped variant — only the given employees' timesheets for the period,
     * not every timesheet the whole company has for that month (which can
     * include thousands of unrelated employees on other projects). */
    List<Timesheet> findByCompanyIdAndPeriodYearAndPeriodMonthAndEmployeeIdIn(
            UUID companyId, int periodYear, int periodMonth, Collection<UUID> employeeIds);

    @Query(value = """
            select t.*
            from timesheet t
            join employee e on e.id = t.employee_id
            join assignment a on a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
            where t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status in ('APPROVED', 'LOCKED')
              and (:projectId is null or a.project_id = :projectId)
              and (:payGroup = 'ALL'
                   or (:payGroup = 'DAILY' and upper(coalesce(e.pay_status, '')) like '%DAILY%')
                   or (:payGroup = 'MONTHLY' and upper(coalesce(e.pay_status, '')) like '%MONTH%'))
            order by e.employee_number
            """, nativeQuery = true)
    List<Timesheet> findPayrollScope(@Param("companyId") UUID companyId,
                                     @Param("year") int year,
                                     @Param("month") int month,
                                     @Param("projectId") UUID projectId,
                                     @Param("payGroup") String payGroup);

    @Query(value = """
            select t from Timesheet t
            join Employee e on e.id = t.employeeId
            join Assignment a on a.employeeId = e.id
            where t.companyId = :companyId
              and t.periodYear = :year
              and t.periodMonth = :month
              and a.projectId = :projectId
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            order by e.employeeNumber
            """,
            countQuery = """
            select count(t.id) from Timesheet t
            join Employee e on e.id = t.employeeId
            join Assignment a on a.employeeId = e.id
            where t.companyId = :companyId
              and t.periodYear = :year
              and t.periodMonth = :month
              and a.projectId = :projectId
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            """)
    Page<Timesheet> searchByProject(@Param("companyId") UUID companyId,
                                    @Param("year") int year,
                                    @Param("month") int month,
                                    @Param("projectId") UUID projectId,
                                    @Param("q") String q,
                                    Pageable pageable);

    @Query(value = """
            select t from Timesheet t
            join Employee e on e.id = t.employeeId
            join Assignment a on a.employeeId = e.id
            where t.companyId = :companyId
              and t.periodYear = :year
              and t.periodMonth = :month
              and a.projectId in :projectIds
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            order by e.employeeNumber
            """,
            countQuery = """
            select count(t.id) from Timesheet t
            join Employee e on e.id = t.employeeId
            join Assignment a on a.employeeId = e.id
            where t.companyId = :companyId
              and t.periodYear = :year
              and t.periodMonth = :month
              and a.projectId in :projectIds
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primaryAssignment = true
              and a.effectiveTo is null
              and (:q is null or :q = '' or (
                    lower(e.employeeNumber) like lower(concat('%', :q, '%'))
                 or lower(e.firstName) like lower(concat('%', :q, '%'))
                 or lower(e.lastName) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.middleName, '')) like lower(concat('%', :q, '%'))
                 or lower(coalesce(e.arabicName, '')) like lower(concat('%', :q, '%'))
              ))
            """)
    Page<Timesheet> searchByProjects(@Param("companyId") UUID companyId,
                                     @Param("year") int year,
                                     @Param("month") int month,
                                     @Param("projectIds") Collection<UUID> projectIds,
                                     @Param("q") String q,
                                     Pageable pageable);

    @Query(value = """
            select count(t.id)
            from timesheet t
            join assignment a on a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
            where t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status = :status
              and (:projectId is null or a.project_id = :projectId)
            """, nativeQuery = true)
    int countStatusByProject(@Param("companyId") UUID companyId,
                             @Param("year") int year,
                             @Param("month") int month,
                             @Param("status") String status,
                             @Param("projectId") UUID projectId);

    @Query(value = """
            select count(t.id)
            from timesheet t
            join assignment a on a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
            where t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status = :status
              and a.project_id in (:projectIds)
            """, nativeQuery = true)
    int countStatusByProjects(@Param("companyId") UUID companyId,
                              @Param("year") int year,
                              @Param("month") int month,
                              @Param("status") String status,
                              @Param("projectIds") Collection<UUID> projectIds);

    @Query(value = """
            select count(distinct t.id)
            from timesheet t
            join assignment a on a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
            join timesheet_day td on td.timesheet_id = t.id
            where t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status = :status
              and (:projectId is null or a.project_id = :projectId)
              and (coalesce(td.normal_hours, 0) + coalesce(td.ot_hours, 0)) > 0
              and (td.project_id is null or td.cost_code_id is null)
              and not exists (
                select 1
                from timesheet_day_cost tdc
                where tdc.timesheet_day_id = td.id
                  and tdc.project_id is not null
                  and tdc.cost_code_id is not null
                  and coalesce(tdc.hours, 0) > 0
              )
            """, nativeQuery = true)
    int countMissingCostAllocationsByProject(@Param("companyId") UUID companyId,
                                             @Param("year") int year,
                                             @Param("month") int month,
                                             @Param("status") String status,
                                             @Param("projectId") UUID projectId);

    @Query(value = """
            select count(distinct t.id)
            from timesheet t
            join assignment a on a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
            join timesheet_day td on td.timesheet_id = t.id
            where t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status = :status
              and a.project_id in (:projectIds)
              and (coalesce(td.normal_hours, 0) + coalesce(td.ot_hours, 0)) > 0
              and (td.project_id is null or td.cost_code_id is null)
              and not exists (
                select 1
                from timesheet_day_cost tdc
                where tdc.timesheet_day_id = td.id
                  and tdc.project_id is not null
                  and tdc.cost_code_id is not null
                  and coalesce(tdc.hours, 0) > 0
              )
            """, nativeQuery = true)
    int countMissingCostAllocationsByProjects(@Param("companyId") UUID companyId,
                                              @Param("year") int year,
                                              @Param("month") int month,
                                              @Param("status") String status,
                                              @Param("projectIds") Collection<UUID> projectIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            with scoped as (
              select t.id
              from timesheet t
              join assignment a on a.employee_id = t.employee_id
                and upper(coalesce(a.status, '')) = 'ACTIVE'
                and a.primary_assignment = true
                and a.effective_to is null
              where t.company_id = :companyId
                and t.period_year = :year
                and t.period_month = :month
                and t.status = 'DRAFT'
                and (:projectId is null or a.project_id = :projectId)
            ),
            totals as (
              select td.timesheet_id,
                     coalesce(sum(td.worked_hours), 0) as worked,
                     coalesce(sum(td.ot_hours), 0) as ot,
                     coalesce(sum(case when tt.category = 'ABSENCE' then 1 else 0 end), 0) as absence
              from timesheet_day td
              join scoped s on s.id = td.timesheet_id
              left join time_type tt on tt.id = td.time_type_id
              group by td.timesheet_id
            )
            update timesheet t
            set status = 'SUBMITTED',
                submitted_at = :submittedAt,
                total_worked_hours = coalesce(totals.worked, 0),
                total_ot_hours = coalesce(totals.ot, 0),
                total_absence_days = coalesce(totals.absence, 0),
                updated_at = :submittedAt
            from scoped s
            left join totals on totals.timesheet_id = s.id
            where t.id = s.id
            """, nativeQuery = true)
    int submitDraftsByProject(@Param("companyId") UUID companyId,
                              @Param("year") int year,
                              @Param("month") int month,
                              @Param("projectId") UUID projectId,
                              @Param("submittedAt") Instant submittedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            with scoped as (
              select t.id
              from timesheet t
              join assignment a on a.employee_id = t.employee_id
                and upper(coalesce(a.status, '')) = 'ACTIVE'
                and a.primary_assignment = true
                and a.effective_to is null
              where t.company_id = :companyId
                and t.period_year = :year
                and t.period_month = :month
                and t.status = 'DRAFT'
                and a.project_id in (:projectIds)
            ),
            totals as (
              select td.timesheet_id,
                     coalesce(sum(td.worked_hours), 0) as worked,
                     coalesce(sum(td.ot_hours), 0) as ot,
                     coalesce(sum(case when tt.category = 'ABSENCE' then 1 else 0 end), 0) as absence
              from timesheet_day td
              join scoped s on s.id = td.timesheet_id
              left join time_type tt on tt.id = td.time_type_id
              group by td.timesheet_id
            )
            update timesheet t
            set status = 'SUBMITTED',
                submitted_at = :submittedAt,
                total_worked_hours = coalesce(totals.worked, 0),
                total_ot_hours = coalesce(totals.ot, 0),
                total_absence_days = coalesce(totals.absence, 0),
                updated_at = :submittedAt
            from scoped s
            left join totals on totals.timesheet_id = s.id
            where t.id = s.id
            """, nativeQuery = true)
    int submitDraftsByProjects(@Param("companyId") UUID companyId,
                               @Param("year") int year,
                               @Param("month") int month,
                               @Param("projectIds") Collection<UUID> projectIds,
                               @Param("submittedAt") Instant submittedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update timesheet t
            set status = 'APPROVED',
                approved_at = :approvedAt,
                approved_by = :approvedBy,
                updated_at = :approvedAt
            from assignment a
            where a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
              and t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status = 'SUBMITTED'
              and (:projectId is null or a.project_id = :projectId)
            """, nativeQuery = true)
    int approveSubmittedByProject(@Param("companyId") UUID companyId,
                                  @Param("year") int year,
                                  @Param("month") int month,
                                  @Param("projectId") UUID projectId,
                                  @Param("approvedAt") Instant approvedAt,
                                  @Param("approvedBy") String approvedBy);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update timesheet t
            set status = 'APPROVED',
                approved_at = :approvedAt,
                approved_by = :approvedBy,
                updated_at = :approvedAt
            from assignment a
            where a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
              and t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status = 'SUBMITTED'
              and a.project_id in (:projectIds)
            """, nativeQuery = true)
    int approveSubmittedByProjects(@Param("companyId") UUID companyId,
                                   @Param("year") int year,
                                   @Param("month") int month,
                                   @Param("projectIds") Collection<UUID> projectIds,
                                   @Param("approvedAt") Instant approvedAt,
                                   @Param("approvedBy") String approvedBy);

    @Query(value = """
            select count(t.id)
            from timesheet t
            join employee e on e.id = t.employee_id
            join assignment a on a.employee_id = t.employee_id
              and upper(coalesce(a.status, '')) = 'ACTIVE'
              and a.primary_assignment = true
              and a.effective_to is null
            where t.company_id = :companyId
              and t.period_year = :year
              and t.period_month = :month
              and t.status = 'APPROVED'
              and a.project_id = :projectId
              and (:payGroup = 'ALL' or e.pay_status = :payGroup)
            """, nativeQuery = true)
    int countApprovedForProjectLock(@Param("companyId") UUID companyId,
                                    @Param("year") int year,
                                    @Param("month") int month,
                                    @Param("projectId") UUID projectId,
                                    @Param("payGroup") String payGroup);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update timesheet t
            set status = 'LOCKED',
                updated_at = :lockedAt
            from employee e, assignment a
            where e.id = t.employee_id
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
            """, nativeQuery = true)
    int lockApprovedForProject(@Param("companyId") UUID companyId,
                               @Param("year") int year,
                               @Param("month") int month,
                               @Param("projectId") UUID projectId,
                               @Param("payGroup") String payGroup,
                               @Param("lockedAt") Instant lockedAt);

    @Query(value = """
            with period_row as (
              select id, start_date, end_date
              from payroll_period
              where company_id = :companyId
                and period_year = :year
                and period_month = :month
              limit 1
            ),
            eligible as (
              select p.id as project_id, p.code as project_code, p.name as project_name, count(distinct e.id) as eligible
              from project p
              join assignment a on a.project_id = p.id
                and upper(coalesce(a.status, '')) = 'ACTIVE'
                and a.primary_assignment = true
                and a.effective_to is null
              join employee e on e.id = a.employee_id
                and e.company_id = :companyId
                and upper(coalesce(e.status, '')) = 'ACTIVE'
              join period_row pr on true
              where p.company_id = :companyId
                and (:projectId is null or p.id = :projectId)
                and exists (
                  select 1
                  from employee_shift es
                  where es.employee_id = e.id
                    and es.company_id = :companyId
                    and upper(coalesce(es.status, '')) = 'ACTIVE'
                    and (es.effective_from is null or es.effective_from <= pr.end_date)
                    and (es.effective_to is null or es.effective_to >= pr.start_date)
                )
              group by p.id, p.code, p.name
            ),
            generated as (
              select p.id as project_id,
                     count(distinct t.id) as generated,
                     sum(case when t.status = 'DRAFT' then 1 else 0 end) as draft,
                     sum(case when t.status = 'SUBMITTED' then 1 else 0 end) as submitted,
                     sum(case when t.status = 'APPROVED' then 1 else 0 end) as approved,
                     sum(case when t.status = 'LOCKED' then 1 else 0 end) as locked
              from timesheet t
              join employee e on e.id = t.employee_id
              join assignment a on a.employee_id = e.id
                and upper(coalesce(a.status, '')) = 'ACTIVE'
                and a.primary_assignment = true
                and a.effective_to is null
              join project p on p.id = a.project_id
              where t.company_id = :companyId
                and t.period_year = :year
                and t.period_month = :month
                and (:projectId is null or p.id = :projectId)
              group by p.id
            )
            select e.project_id,
                   e.project_code,
                   e.project_name,
                   e.eligible,
                   coalesce(g.generated, 0) as generated,
                   greatest(e.eligible - coalesce(g.generated, 0), 0) as missing,
                   coalesce(g.draft, 0) as draft,
                   coalesce(g.submitted, 0) as submitted,
                   coalesce(g.approved, 0) as approved,
                   coalesce(g.locked, 0) as locked
            from eligible e
            left join generated g on g.project_id = e.project_id
            order by e.project_code
            """, nativeQuery = true)
    List<Object[]> projectSummary(@Param("companyId") UUID companyId,
                                  @Param("year") int year,
                                  @Param("month") int month,
                                  @Param("projectId") UUID projectId);

    List<Timesheet> findByCompanyIdAndEmployeeIdAndPeriodYearOrderByPeriodMonth(
            UUID companyId, UUID employeeId, int periodYear);

    List<Timesheet> findByPeriodId(UUID periodId);
}
