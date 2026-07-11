package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, UUID> {

    List<PayrollAdjustment> findByCompanyIdAndEmployeeIdAndStatus(UUID companyId, UUID employeeId, String status);

    /** Batch variant — used when calculating a whole payroll run, so we
     * don't query once per employee. */
    List<PayrollAdjustment> findByCompanyIdAndEmployeeIdInAndStatus(
            UUID companyId, Collection<UUID> employeeIds, String status);

    List<PayrollAdjustment> findByCompanyIdAndStatusOrderByWorkDateDesc(UUID companyId, String status);

    /** All Day Zero corrections (any status) tied to one employee's
     * timesheet period — used to show a read-only marker on the original
     * (locked) timesheet card, without touching the timesheet itself. */
    List<PayrollAdjustment> findByEmployeeIdAndOriginalPeriodIdOrderByWorkDate(UUID employeeId, UUID originalPeriodId);

    /** Audit Tools — every Day Zero adjustment for one employee, any status,
     * most recent first, so a manager can review and clean up as needed. */
    List<PayrollAdjustment> findByCompanyIdAndEmployeeIdOrderByCreatedAtDesc(UUID companyId, UUID employeeId);

    /** Time Usage / payroll threshold counting — every Day Zero correction
     * that changed a day's TYPE (not just its hours) for this employee
     * within a calendar year, so those days can be reclassified instead of
     * counted under their original (now superseded) type. */
    List<PayrollAdjustment> findByCompanyIdAndEmployeeIdAndWorkDateBetweenAndNewTimeTypeIdIsNotNull(
            UUID companyId, UUID employeeId, LocalDate from, LocalDate to);

    /** Batch variant of the above — used by the payroll engine's own
     * annual-threshold counting, which processes a whole run's employees
     * at once rather than one at a time. */
    List<PayrollAdjustment> findByCompanyIdAndEmployeeIdInAndWorkDateBetweenAndNewTimeTypeIdIsNotNull(
            UUID companyId, Collection<UUID> employeeIds, LocalDate from, LocalDate to);
}
