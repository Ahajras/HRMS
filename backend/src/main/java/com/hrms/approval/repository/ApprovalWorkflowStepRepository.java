package com.hrms.approval.repository;

import com.hrms.approval.domain.ApprovalWorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApprovalWorkflowStepRepository extends JpaRepository<ApprovalWorkflowStep, UUID> {
    List<ApprovalWorkflowStep> findByWorkflowIdAndStatusOrderByStepOrderAsc(UUID workflowId, String status);
    List<ApprovalWorkflowStep> findByWorkflowIdOrderByStepOrderAsc(UUID workflowId);
}
