package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollCategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PayrollCategoryRuleRepository extends JpaRepository<PayrollCategoryRule, UUID> {
    List<PayrollCategoryRule> findByPayrollRuleIdAndStatusOrderByCategory(UUID payrollRuleId, String status);
    Optional<PayrollCategoryRule> findByPayrollRuleIdAndCategoryAndStatus(UUID payrollRuleId, String category, String status);
}
