package com.hrms.employee.repository;

import com.hrms.employee.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);

    List<Assignment> findByOrganizationUnitId(UUID organizationUnitId);
}
