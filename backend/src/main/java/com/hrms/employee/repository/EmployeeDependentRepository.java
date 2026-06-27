package com.hrms.employee.repository;

import com.hrms.employee.domain.EmployeeDependent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeDependentRepository extends JpaRepository<EmployeeDependent, UUID> {

    List<EmployeeDependent> findByEmployeeIdOrderByFullName(UUID employeeId);

    Optional<EmployeeDependent> findByEmployeeIdAndFullName(UUID employeeId, String fullName);
}
