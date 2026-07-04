package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.EmployeeShift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeShiftRepository extends JpaRepository<EmployeeShift, UUID> {

    List<EmployeeShift> findByCompanyIdOrderByEffectiveFromDesc(UUID companyId);

    List<EmployeeShift> findByCompanyIdAndEmployeeIdOrderByEffectiveFromDesc(UUID companyId, UUID employeeId);

    List<EmployeeShift> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);
}
