package com.hrms.approval.repository;

import com.hrms.approval.domain.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, UUID> {
    Optional<ApprovalWorkflow> findFirstByCompanyIdAndProcessCodeAndProjectIdAndPayGroupAndStatus(
            UUID companyId, String processCode, UUID projectId, String payGroup, String status);
    Optional<ApprovalWorkflow> findFirstByCompanyIdAndProcessCodeAndProjectIdAndPayGroupAndStatusOrderByCreatedAtDesc(
            UUID companyId, String processCode, UUID projectId, String payGroup, String status);
    List<ApprovalWorkflow> findByCompanyIdOrderByProcessCodeAscProjectIdAscPayGroupAsc(UUID companyId);
}
