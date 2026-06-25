package com.hrms.employee.repository;

import com.hrms.employee.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    List<Contract> findByEmployeeIdOrderByEffectiveFromDesc(UUID employeeId);
}
