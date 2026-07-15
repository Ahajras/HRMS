package com.hrms.approval.repository;

import com.hrms.approval.domain.ApprovalInstanceStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalInstanceStepRepository extends JpaRepository<ApprovalInstanceStep, UUID> {
    List<ApprovalInstanceStep> findByInstanceIdOrderByStepOrderAsc(UUID instanceId);
    Optional<ApprovalInstanceStep> findByInstanceIdAndStatus(UUID instanceId, String status);
    List<ApprovalInstanceStep> findAllByInstanceIdAndStatus(UUID instanceId, String status);
}
