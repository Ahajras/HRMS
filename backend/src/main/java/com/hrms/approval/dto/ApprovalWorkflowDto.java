package com.hrms.approval.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ApprovalWorkflowDto {
    private UUID id;
    private String processCode;
    private UUID projectId;
    private String projectCode;
    private String payGroup = "ALL";
    private String name;
    private String status = "ACTIVE";
    private List<StepDto> steps = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String processCode) { this.processCode = processCode; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<StepDto> getSteps() { return steps; }
    public void setSteps(List<StepDto> steps) { this.steps = steps; }

    public static class StepDto {
        private UUID id;
        private int stepOrder;
        private String name;
        private String approverType;
        private String approverRoleCode;
        private UUID approverEmployeeId;
        private String approverEmployeeNumber;
        private String approverEmployeeName;
        private String status = "ACTIVE";

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
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
        public String getApproverEmployeeNumber() { return approverEmployeeNumber; }
        public void setApproverEmployeeNumber(String approverEmployeeNumber) { this.approverEmployeeNumber = approverEmployeeNumber; }
        public String getApproverEmployeeName() { return approverEmployeeName; }
        public void setApproverEmployeeName(String approverEmployeeName) { this.approverEmployeeName = approverEmployeeName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
