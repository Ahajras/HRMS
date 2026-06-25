package com.hrms.organization.repository;

import com.hrms.organization.domain.OrganizationUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationUnitRepository extends JpaRepository<OrganizationUnit, UUID> {

    List<OrganizationUnit> findByCompanyId(UUID companyId);

    List<OrganizationUnit> findByCompanyIdAndParentId(UUID companyId, UUID parentId);

    List<OrganizationUnit> findByCompanyIdAndParentIdIsNull(UUID companyId);

    boolean existsByCompanyIdAndCode(UUID companyId, String code);
}
