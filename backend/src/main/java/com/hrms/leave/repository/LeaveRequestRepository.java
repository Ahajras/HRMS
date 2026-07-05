package com.hrms.leave.repository;

import com.hrms.leave.domain.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByCompanyIdOrderByStartDateDesc(UUID companyId);
    List<LeaveRequest> findByCompanyIdAndEmployeeIdOrderByStartDateDesc(UUID companyId, UUID employeeId);
    List<LeaveRequest> findByCompanyIdAndEmployeeIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
            UUID companyId, UUID employeeId, String status, LocalDate start, LocalDate end);
    List<LeaveRequest> findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndStatusInAndStartDateLessThanEqual(
            UUID companyId, UUID employeeId, UUID leaveTypeId, Collection<String> statuses, LocalDate asOfDate);
}
