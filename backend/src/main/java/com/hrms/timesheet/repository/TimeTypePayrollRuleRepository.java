package com.hrms.timesheet.repository;

import com.hrms.timesheet.domain.TimeTypePayrollRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;

public interface TimeTypePayrollRuleRepository extends JpaRepository<TimeTypePayrollRule, UUID> {
    List<TimeTypePayrollRule> findByCompanyIdAndTimeTypeIdOrderBySortOrderAsc(UUID companyId, UUID timeTypeId);
    List<TimeTypePayrollRule> findByCompanyIdAndTimeTypeIdIn(UUID companyId, Collection<UUID> timeTypeIds);
    Optional<TimeTypePayrollRule> findByCompanyIdAndTimeTypeIdAndPayrollComponentId(UUID companyId, UUID timeTypeId, UUID payrollComponentId);
    void deleteByCompanyIdAndTimeTypeIdAndPayrollComponentId(UUID companyId, UUID timeTypeId, UUID payrollComponentId);
}
