package com.hrms.leave.repository;

import com.hrms.leave.domain.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    List<LeaveType> findByCompanyIdOrderByCode(UUID companyId);
    Optional<LeaveType> findByCompanyIdAndCode(UUID companyId, String code);
}
