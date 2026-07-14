package com.hrms.project.repository;

import com.hrms.project.domain.ProjectApprovalRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectApprovalRoleRepository extends JpaRepository<ProjectApprovalRole, UUID> {
    List<ProjectApprovalRole> findByCompanyIdOrderByProjectIdAscRoleCodeAsc(UUID companyId);
    List<ProjectApprovalRole> findByCompanyIdAndProjectIdAndRoleCodeAndStatus(UUID companyId, UUID projectId, String roleCode, String status);
    boolean existsByCompanyIdAndProjectIdAndRoleCodeAndEmployeeId(UUID companyId, UUID projectId, String roleCode, UUID employeeId);
}
