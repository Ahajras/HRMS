package com.hrms.leave.repository;

import com.hrms.leave.domain.LeaveAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LeaveAdjustmentRepository extends JpaRepository<LeaveAdjustment, UUID> {
    List<LeaveAdjustment> findByCompanyIdAndEmployeeIdOrderByEffectiveDateDesc(UUID companyId, UUID employeeId);
    List<LeaveAdjustment> findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndEffectiveDateLessThanEqual(
            UUID companyId, UUID employeeId, UUID leaveTypeId, LocalDate asOfDate);
}
