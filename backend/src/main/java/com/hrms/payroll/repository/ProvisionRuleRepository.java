package com.hrms.payroll.repository;

import com.hrms.payroll.domain.ProvisionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvisionRuleRepository extends JpaRepository<ProvisionRule, UUID> {

    List<ProvisionRule> findByCompanyIdOrderByProvisionTypeAscPayGroupAscNameAsc(UUID companyId);

    Optional<ProvisionRule> findByCompanyIdAndProvisionTypeAndProjectIdIsNullAndPayGroup(UUID companyId, String provisionType, String payGroup);

    @Query("""
            select r from ProvisionRule r
            where r.companyId = :companyId
              and upper(r.provisionType) = upper(:provisionType)
              and upper(coalesce(r.status, '')) = 'ACTIVE'
              and r.effectiveFrom <= :asOfDate
              and (r.effectiveTo is null or r.effectiveTo >= :asOfDate)
              and (r.projectId is null or r.projectId = :projectId)
              and (upper(r.payGroup) = 'ALL' or upper(r.payGroup) = upper(:payGroup))
            order by
              case when r.projectId = :projectId then 0 else 1 end,
              case when upper(r.payGroup) = upper(:payGroup) then 0 else 1 end,
              r.effectiveFrom desc
            """)
    List<ProvisionRule> findMatching(@Param("companyId") UUID companyId,
                                     @Param("provisionType") String provisionType,
                                     @Param("projectId") UUID projectId,
                                     @Param("payGroup") String payGroup,
                                     @Param("asOfDate") LocalDate asOfDate);
}
