package com.hrms.employee.repository;

import com.hrms.employee.domain.EmployeeBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeBankAccountRepository extends JpaRepository<EmployeeBankAccount, UUID> {

    List<EmployeeBankAccount> findByEmployeeIdOrderByPrimaryDesc(UUID employeeId);
}
