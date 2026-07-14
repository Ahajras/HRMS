package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollResult;
import com.hrms.payroll.domain.PayrollRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PayrollResultRepository extends JpaRepository<PayrollResult, UUID> {

    List<PayrollResult> findByRunIdOrderByEmployeeId(UUID runId);

    /** Used by Day Zero to find what an employee was actually paid for a
     * prior, already-locked/calculated period. */
    java.util.Optional<PayrollResult> findByRunIdAndEmployeeId(UUID runId, UUID employeeId);

    /** Day Zero — the single most-recently-CALCULATED result for this
     * employee within a period, regardless of which run scope (e.g. an
     * "ALL employees" run and a "MONTHLY only" run can both legitimately
     * exist for the same period/project and both include this employee).
     * Ordering by the RUN's creation date is not enough — a run created
     * earlier can have been recalculated more recently than one created
     * later. This orders by the RESULT's own timestamp instead, so we
     * always compare against the freshest actual number. */
    @Query("""
            select r from PayrollResult r, PayrollRun run
            where run.id = r.runId
              and run.periodId = :periodId
              and r.employeeId = :employeeId
            order by r.createdAt desc
            """)
    List<PayrollResult> findByPeriodIdAndEmployeeIdOrderByCreatedAtDesc(
            @Param("periodId") UUID periodId, @Param("employeeId") UUID employeeId);

    /** Paginated — used by the run detail screen so opening a large run
     * (thousands of employees) stays fast. */
    Page<PayrollResult> findByRunId(UUID runId, Pageable pageable);

    /** Paginated + restricted to a specific set of employees (used for search). */
    Page<PayrollResult> findByRunIdAndEmployeeIdIn(UUID runId, Collection<UUID> employeeIds, Pageable pageable);

    /** Combine several runs together — e.g. every project's run for one
     * period — for reports that need a whole-month view rather than one
     * project's run at a time. */
    List<PayrollResult> findByRunIdInOrderByEmployeeId(Collection<UUID> runIds);

    Page<PayrollResult> findByRunIdIn(Collection<UUID> runIds, Pageable pageable);

    Page<PayrollResult> findByRunIdInAndEmployeeIdIn(Collection<UUID> runIds, Collection<UUID> employeeIds, Pageable pageable);

    void deleteByRunId(UUID runId);

    /** Self-service — every payslip an employee has ever had, most recent
     * first. Used only by /api/v1/me endpoints, which always pass the
     * caller's OWN employeeId from the security context. */
    @Query("""
            select r from PayrollResult r, PayrollRun run
            where run.id = r.runId
              and r.employeeId = :employeeId
            order by r.createdAt desc
            """)
    List<PayrollResult> findByEmployeeIdOrderByCreatedAtDesc(@Param("employeeId") UUID employeeId);

    @Query("""
            select count(r), coalesce(sum(r.gross), 0), coalesce(sum(r.net), 0)
            from PayrollResult r
            where r.runId = :runId
            """)
    List<Object[]> summarizeRun(@Param("runId") UUID runId);

    /** Management dashboard — total net/deductions and payslip count for
     * every LOCKED run in a given period (across all projects/pay
     * groups), aggregated directly rather than fetching every result. */
    @Query("""
            select count(r), coalesce(sum(r.net), 0), coalesce(sum(r.totalDeductions), 0)
            from PayrollResult r, PayrollRun run
            where run.id = r.runId
              and run.periodId = :periodId
              and upper(run.status) = 'LOCKED'
            """)
    List<Object[]> summarizeLockedPeriod(@Param("periodId") UUID periodId);
}
