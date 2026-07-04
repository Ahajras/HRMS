package com.hrms.payroll.repository;

import com.hrms.payroll.domain.PayrollRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface PayrollRuleRepository extends JpaRepository<PayrollRule, UUID> {
    List<PayrollRule> findByCompanyIdOrderByPayGroup(UUID companyId);
    Optional<PayrollRule> findByCompanyIdAndProjectIdAndPayGroupAndStatus(UUID companyId, UUID projectId, String payGroup, String status);

    @Query("""
            select r from PayrollRule r
            where r.companyId = :companyId
              and r.projectId is null
              and r.payGroup = :payGroup
              and r.status = :status
            """)
    Optional<PayrollRule> findDefaultByCompanyIdAndPayGroupAndStatus(@Param("companyId") UUID companyId,
                                                                     @Param("payGroup") String payGroup,
                                                                     @Param("status") String status);
}
