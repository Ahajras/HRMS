package com.hrms.workpackage.repository;

import com.hrms.workpackage.domain.WorkPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkPackageRepository extends JpaRepository<WorkPackage, UUID> {
    List<WorkPackage> findByCompanyIdOrderByCode(UUID companyId);
    List<WorkPackage> findByCompanyIdAndProjectIdOrderByCode(UUID companyId, UUID projectId);
    boolean existsByCompanyIdAndProjectIdAndCode(UUID companyId, UUID projectId, String code);
    List<WorkPackage> findByCompanyIdAndProjectIdAndCodeStartingWithOrderByCode(UUID companyId, UUID projectId, String prefix);
}
