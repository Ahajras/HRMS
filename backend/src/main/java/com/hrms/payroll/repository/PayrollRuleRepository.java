package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PayrollRuleRepository extends JpaRepository<PayrollRule, UUID> {
    List<PayrollRule> findByCompanyIdOrderByPayGroup(UUID companyId);
    Optional<PayrollRule> findByCompanyIdAndPayGroupAndStatus(UUID companyId, String payGroup, String status);
}
