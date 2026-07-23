package com.hrms.workpackage.repository;

import com.hrms.workpackage.domain.WorkPackageRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkPackageRequirementRepository extends JpaRepository<WorkPackageRequirement, UUID> {
    List<WorkPackageRequirement> findByWorkPackageIdOrderByJobTitleCode(UUID workPackageId);
    boolean existsByWorkPackageIdAndJobTitleCode(UUID workPackageId, String jobTitleCode);
    int countByWorkPackageId(UUID workPackageId);
}
