package com.hrms.approval.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_instance_step")
public class ApprovalInstanceStep extends AuditableEntity {
    @Column(name = "instance_id", nullable = false)
    private UUID instanceId;
    @Column(name = "step_order", nullable = false)
    private int stepOrder;
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    @Column(name = "approver_type", nullable = false, length = 40)
    private String approverType;
    @Column(name = "approver_role_code", length = 60)
    private String approverRoleCode;
    @Column(name = "approver_employee_id")
    private UUID approverEmployeeId;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "WAITING";
    @Column(name = "decided_by", length = 100)
    private String decidedBy;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "remarks")
    private String remarks;

    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getApproverType() { return approverType; }
    public void setApproverType(String approverType) { this.approverType = approverType; }
    public String getApproverRoleCode() { return approverRoleCode; }
    public void setApproverRoleCode(String approverRoleCode) { this.approverRoleCode = approverRoleCode; }
    public UUID getApproverEmployeeId() { return approverEmployeeId; }
    public void setApproverEmployeeId(UUID approverEmployeeId) { this.approverEmployeeId = approverEmployeeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
