package com.hrms.project.repository;

import com.hrms.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByCompanyIdOrderByName(UUID companyId);

    List<Project> findByCompanyIdAndIdInOrderByName(UUID companyId, Collection<UUID> ids);

    /** Management dashboard — project count without fetching every row. */
    long countByCompanyIdAndStatus(UUID companyId, String status);
}
