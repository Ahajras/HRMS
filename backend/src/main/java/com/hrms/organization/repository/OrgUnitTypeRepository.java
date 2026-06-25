package com.hrms.organization.repository;

import com.hrms.organization.domain.OrgUnitType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrgUnitTypeRepository extends JpaRepository<OrgUnitType, UUID> {

    List<OrgUnitType> findByCompanyIdOrderByLevelOrder(UUID companyId);

    List<OrgUnitType> findByCompanyIdIsNullOrderByLevelOrder();

    /** Global default levels (company_id NULL) plus any company-specific ones. */
    List<OrgUnitType> findByCompanyIdIsNullOrCompanyIdOrderByLevelOrder(UUID companyId);
}
