package com.hrms.approval.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "approval_workflow_step")
public class ApprovalWorkflowStep extends AuditableEntity {
    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;
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
    private String status = "ACTIVE";

    public UUID getWorkflowId() { return workflowId; }
    public void setWorkflowId(UUID workflowId) { this.workflowId = workflowId; }
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
}
