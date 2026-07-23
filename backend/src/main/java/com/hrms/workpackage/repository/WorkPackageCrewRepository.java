package com.hrms.workpackage.repository;

import com.hrms.workpackage.domain.WorkPackageCrew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkPackageCrewRepository extends JpaRepository<WorkPackageCrew, UUID> {
    List<WorkPackageCrew> findByWorkPackageIdOrderByPlannedStartAsc(UUID workPackageId);
    boolean existsByWorkPackageIdAndCrewId(UUID workPackageId, UUID crewId);
    int countByWorkPackageId(UUID workPackageId);
}
