package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
