package com.hrms.approval.repository;

import com.hrms.approval.domain.ApprovalInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, UUID> {
    Optional<ApprovalInstance> findFirstByCompanyIdAndEntityTypeAndEntityIdAndStatusIn(
            UUID companyId, String entityType, UUID entityId, List<String> statuses);
    List<ApprovalInstance> findByCompanyIdAndStatus(UUID companyId, String status);
    long countByWorkflowId(UUID workflowId);
}
