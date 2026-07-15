package com.hrms.approval.service;

import com.hrms.approval.domain.ApprovalInstance;
import com.hrms.approval.domain.ApprovalInstanceStep;
import com.hrms.approval.domain.ApprovalWorkflow;
import com.hrms.approval.domain.ApprovalWorkflowStep;
import com.hrms.approval.dto.ApprovalTaskDto;
import com.hrms.approval.repository.ApprovalInstanceRepository;
import com.hrms.approval.repository.ApprovalInstanceStepRepository;
import com.hrms.approval.repository.ApprovalWorkflowRepository;
import com.hrms.approval.repository.ApprovalWorkflowStepRepository;
import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.project.domain.ProjectApprovalRole;
import com.hrms.project.repository.ProjectApprovalRoleRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.security.AuthenticatedUser;
import com.hrms.timesheet.repository.TimesheetRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ApprovalService {
    public static final String TIMESHEET_PROCESS = "TIMESHEET_SUBMIT";
    public static final String TIMESHEET_ENTITY = "TIMESHEET";
    public static final String LEAVE_PROCESS = "LEAVE_REQUEST";
    public static final String LEAVE_ENTITY = "LEAVE_REQUEST";

    private final ApprovalWorkflowRepository workflowRepo;
    private final ApprovalWorkflowStepRepository workflowStepRepo;
    private final ApprovalInstanceRepository instanceRepo;
    private final ApprovalInstanceStepRepository instanceStepRepo;
    private final EmployeeRepository employeeRepo;
    private final ProjectApprovalRoleRepository projectApprovalRoleRepo;
    private final ProjectRepository projectRepo;
    private final TimesheetRepository timesheetRepo;
    private final LeaveRequestRepository leaveRequestRepo;
    private final LeaveTypeRepository leaveTypeRepo;
    private final ApprovalNotificationService notificationService;

    public ApprovalService(ApprovalWorkflowRepository workflowRepo,
                           ApprovalWorkflowStepRepository workflowStepRepo,
                           ApprovalInstanceRepository instanceRepo,
                           ApprovalInstanceStepRepository instanceStepRepo,
                           EmployeeRepository employeeRepo,
                           ProjectApprovalRoleRepository projectApprovalRoleRepo,
                           ProjectRepository projectRepo,
                           TimesheetRepository timesheetRepo,
                           LeaveRequestRepository leaveRequestRepo,
                           LeaveTypeRepository leaveTypeRepo,
                           ApprovalNotificationService notificationService) {
        this.workflowRepo = workflowRepo;
        this.workflowStepRepo = workflowStepRepo;
        this.instanceRepo = instanceRepo;
        this.instanceStepRepo = instanceStepRepo;
        this.employeeRepo = employeeRepo;
        this.projectApprovalRoleRepo = projectApprovalRoleRepo;
        this.projectRepo = projectRepo;
        this.timesheetRepo = timesheetRepo;
        this.leaveRequestRepo = leaveRequestRepo;
        this.leaveTypeRepo = leaveTypeRepo;
        this.notificationService = notificationService;
    }

    public void startTimesheetApproval(UUID timesheetId, UUID employeeId, UUID projectId, String payGroup) {
        startApproval(TIMESHEET_PROCESS, TIMESHEET_ENTITY, timesheetId, employeeId, projectId, payGroup,
                "Define an active timesheet approval workflow for this project before submitting.");
    }

    public void startLeaveApproval(UUID leaveRequestId, UUID employeeId, UUID projectId, String payGroup) {
        startApproval(LEAVE_PROCESS, LEAVE_ENTITY, leaveRequestId, employeeId, projectId, payGroup,
                "Define an active leave approval workflow for this project before submitting.");
    }

    private void startApproval(String processCode, String entityType, UUID entityId, UUID employeeId, UUID projectId,
                               String payGroup, String missingWorkflowMessage) {
        UUID companyId = TenantContext.requireCompanyId();
        if (instanceRepo.findFirstByCompanyIdAndEntityTypeAndEntityIdAndStatusIn(
                companyId, entityType, entityId, List.of("PENDING", "APPROVED")).isPresent()) {
            return;
        }
        ApprovalWorkflow workflow = workflowRepo
                .findFirstByCompanyIdAndProcessCodeAndProjectIdAndPayGroupAndStatus(
                        companyId, processCode, projectId, normalizePayGroup(payGroup), "ACTIVE")
                .or(() -> workflowRepo.findFirstByCompanyIdAndProcessCodeAndProjectIdAndPayGroupAndStatus(
                        companyId, processCode, projectId, "ALL", "ACTIVE"))
                .orElseThrow(() -> new BusinessRuleException("approval.workflow.required",
                        missingWorkflowMessage));
        List<ApprovalWorkflowStep> steps = workflowStepRepo.findByWorkflowIdAndStatusOrderByStepOrderAsc(workflow.getId(), "ACTIVE");
        if (steps.isEmpty()) {
            throw new BusinessRuleException("approval.workflow.steps.required", "Approval workflow has no active steps.");
        }

        ApprovalInstance instance = new ApprovalInstance();
        instance.setCompanyId(companyId);
        instance.setWorkflowId(workflow.getId());
        instance.setProcessCode(processCode);
        instance.setEntityType(entityType);
        instance.setEntityId(entityId);
        instance.setProjectId(projectId);
        instance.setEmployeeId(employeeId);
        instance.setStatus("PENDING");
        instance.setCurrentStepOrder(steps.get(0).getStepOrder());
        instance.setSubmittedBy(currentUsername());
        instance.setSubmittedAt(Instant.now());
        instance = instanceRepo.save(instance);

        List<ApprovalInstanceStep> firstPending = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            ApprovalWorkflowStep src = steps.get(i);
            for (UUID approverId : resolveApproverEmployees(src, employeeId, projectId)) {
                ApprovalInstanceStep step = new ApprovalInstanceStep();
                step.setInstanceId(instance.getId());
                step.setStepOrder(src.getStepOrder());
                step.setName(src.getName());
                step.setApproverType(src.getApproverType());
                step.setApproverRoleCode(src.getApproverRoleCode());
                step.setApproverEmployeeId(approverId);
                step.setStatus(i == 0 ? "PENDING" : "WAITING");
                step = instanceStepRepo.save(step);
                if (i == 0) {
                    firstPending.add(step);
                }
            }
        }
        ApprovalInstance savedInstance = instance;
        firstPending.forEach(step -> notificationService.notifyPending(savedInstance, step));
    }

    public boolean approveTimesheetStep(UUID timesheetId) {
        return approveStep(TIMESHEET_ENTITY, timesheetId);
    }

    public boolean approveLeaveStep(UUID leaveRequestId) {
        return approveStep(LEAVE_ENTITY, leaveRequestId);
    }

    private boolean approveStep(String entityType, UUID entityId) {
        UUID companyId = TenantContext.requireCompanyId();
        ApprovalInstance instance = instanceRepo.findFirstByCompanyIdAndEntityTypeAndEntityIdAndStatusIn(
                        companyId, entityType, entityId, List.of("PENDING"))
                .orElseThrow(() -> new BusinessRuleException("approval.instance.required",
                        "This item has no pending approval workflow."));
        UUID currentEmployeeId = currentEmployeeId();
        List<ApprovalInstanceStep> pendingSteps = instanceStepRepo.findAllByInstanceIdAndStatus(instance.getId(), "PENDING");
        ApprovalInstanceStep current = pendingSteps.stream()
                .filter(s -> currentEmployeeId != null && currentEmployeeId.equals(s.getApproverEmployeeId()))
                .findFirst()
                .orElse(null);
        if (current == null) {
            throw new BusinessRuleException("approval.step.unauthorized",
                    "This approval is waiting for another approver.");
        }
        current.setStatus("APPROVED");
        current.setDecidedAt(Instant.now());
        current.setDecidedBy(currentUsername());
        instanceStepRepo.save(current);
        for (ApprovalInstanceStep peer : pendingSteps) {
            if (!peer.getId().equals(current.getId()) && peer.getStepOrder() == current.getStepOrder()) {
                peer.setStatus("SKIPPED");
                peer.setDecidedAt(Instant.now());
                peer.setDecidedBy(currentUsername());
                instanceStepRepo.save(peer);
            }
        }

        List<ApprovalInstanceStep> steps = instanceStepRepo.findByInstanceIdOrderByStepOrderAsc(instance.getId());
        Integer nextOrder = steps.stream()
                .filter(s -> "WAITING".equals(s.getStatus()))
                .map(ApprovalInstanceStep::getStepOrder)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (nextOrder == null) {
            instance.setStatus("APPROVED");
            instance.setCompletedAt(Instant.now());
            instanceRepo.save(instance);
            return true;
        }
        List<ApprovalInstanceStep> nextSteps = steps.stream()
                .filter(s -> "WAITING".equals(s.getStatus()) && s.getStepOrder() == nextOrder)
                .toList();
        for (ApprovalInstanceStep next : nextSteps) {
            next.setStatus("PENDING");
            instanceStepRepo.save(next);
        }
        instance.setCurrentStepOrder(nextOrder);
        instanceRepo.save(instance);
        nextSteps.forEach(next -> notificationService.notifyPending(instance, next));
        return false;
    }

    public void voidTimesheetApproval(UUID timesheetId) {
        voidApproval(TIMESHEET_ENTITY, timesheetId);
    }

    public void voidLeaveApproval(UUID leaveRequestId) {
        voidApproval(LEAVE_ENTITY, leaveRequestId);
    }

    private void voidApproval(String entityType, UUID entityId) {
        UUID companyId = TenantContext.requireCompanyId();
        instanceRepo.findFirstByCompanyIdAndEntityTypeAndEntityIdAndStatusIn(
                companyId, entityType, entityId, List.of("PENDING", "APPROVED")).ifPresent(instance -> {
            instance.setStatus("VOID");
            instance.setCompletedAt(Instant.now());
            instanceRepo.save(instance);
            for (ApprovalInstanceStep step : instanceStepRepo.findByInstanceIdOrderByStepOrderAsc(instance.getId())) {
                if ("PENDING".equals(step.getStatus()) || "WAITING".equals(step.getStatus())) {
                    step.setStatus("VOID");
                    instanceStepRepo.save(step);
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public List<ApprovalTaskDto> myPendingTasks() {
        UUID companyId = TenantContext.requireCompanyId();
        UUID employeeId = currentEmployeeId();
        if (employeeId == null) {
            return List.of();
        }
        List<ApprovalTaskDto> out = new ArrayList<>();
        for (ApprovalInstance instance : instanceRepo.findByCompanyIdAndStatus(companyId, "PENDING")) {
            if (TIMESHEET_ENTITY.equals(instance.getEntityType()) && timesheetRepo.findById(instance.getEntityId()).isEmpty()) {
                continue;
            }
            if (LEAVE_ENTITY.equals(instance.getEntityType()) && leaveRequestRepo.findById(instance.getEntityId()).isEmpty()) {
                continue;
            }
            for (ApprovalInstanceStep step : instanceStepRepo.findAllByInstanceIdAndStatus(instance.getId(), "PENDING")) {
                if (employeeId.equals(step.getApproverEmployeeId())) {
                    out.add(toTask(instance, step));
                }
            }
        }
        return out;
    }

    private ApprovalTaskDto toTask(ApprovalInstance instance, ApprovalInstanceStep step) {
        ApprovalTaskDto dto = new ApprovalTaskDto();
        dto.setInstanceId(instance.getId());
        dto.setStepId(step.getId());
        dto.setProcessCode(instance.getProcessCode());
        dto.setEntityType(instance.getEntityType());
        dto.setEntityId(instance.getEntityId());
        dto.setEmployeeId(instance.getEmployeeId());
        dto.setProjectId(instance.getProjectId());
        dto.setStepOrder(step.getStepOrder());
        dto.setStepName(step.getName());
        dto.setStatus(step.getStatus());
        dto.setSubmittedAt(instance.getSubmittedAt());
        employeeRepo.findById(instance.getEmployeeId()).ifPresent(e -> {
            dto.setEmployeeNumber(e.getEmployeeNumber());
            dto.setEmployeeName((safe(e.getFirstName()) + " " + safe(e.getLastName())).trim());
            dto.setPayGroup(e.getPayStatus());
        });
        if (instance.getProjectId() != null) {
            projectRepo.findById(instance.getProjectId()).ifPresent(p -> dto.setProjectCode(p.getCode()));
        }
        if (TIMESHEET_ENTITY.equals(instance.getEntityType())) {
            timesheetRepo.findById(instance.getEntityId()).ifPresent(ts -> {
                dto.setPeriodYear(ts.getPeriodYear());
                dto.setPeriodMonth(ts.getPeriodMonth());
                dto.setTimesheetStatus(ts.getStatus());
                dto.setTotalWorkedHours(ts.getTotalWorkedHours());
                dto.setTotalOtHours(ts.getTotalOtHours());
                dto.setTotalAbsenceDays(ts.getTotalAbsenceDays());
            });
        }
        if (LEAVE_ENTITY.equals(instance.getEntityType())) {
            leaveRequestRepo.findById(instance.getEntityId()).ifPresent(leave -> {
                dto.setLeaveStartDate(leave.getStartDate());
                dto.setLeaveEndDate(leave.getEndDate());
                dto.setLeaveReturnDate(leave.getReturnDate());
                dto.setLeaveTotalDays(leave.getTotalDays());
                dto.setLeaveStatus(leave.getStatus());
                leaveTypeRepo.findById(leave.getLeaveTypeId()).ifPresent(type -> {
                    dto.setLeaveTypeCode(type.getCode());
                    dto.setLeaveTypeName(type.getName());
                });
            });
        }
        return dto;
    }

    private List<UUID> resolveApproverEmployees(ApprovalWorkflowStep step, UUID employeeId, UUID projectId) {
        String type = step.getApproverType() == null ? "" : step.getApproverType().toUpperCase(Locale.ROOT);
        if ("SPECIFIC_EMPLOYEE".equals(type)) {
            return List.of(require(step.getApproverEmployeeId(), "Specific approval employee is not configured."));
        }
        if ("SUPERVISOR".equals(type)) {
            Employee employee = employeeRepo.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
            return List.of(require(employee.getSupervisorEmployeeId(), "Employee has no supervisor configured for approval."));
        }
        if ("PROJECT_ROLE".equals(type)) {
            List<ProjectApprovalRole> rows = projectApprovalRoleRepo.findByCompanyIdAndProjectIdAndRoleCodeAndStatus(
                    TenantContext.requireCompanyId(), projectId, step.getApproverRoleCode(), "ACTIVE");
            List<UUID> approvers = rows.stream()
                    .map(ProjectApprovalRole::getEmployeeId)
                    .filter(id -> id != null && !id.equals(employeeId))
                    .distinct()
                    .toList();
            if (approvers.isEmpty()) {
                throw new BusinessRuleException("approval.project.role.required",
                        "No active non-self " + step.getApproverRoleCode() + " approver is assigned to this project.");
            }
            return approvers;
        }
        throw new BusinessRuleException("approval.approver.type", "Unknown approver type: " + step.getApproverType());
    }

    private UUID require(UUID id, String message) {
        if (id == null) {
            throw new BusinessRuleException("approval.approver.required", message);
        }
        return id;
    }

    private String normalizePayGroup(String payGroup) {
        return payGroup == null || payGroup.isBlank() ? "ALL" : payGroup.trim().toUpperCase(Locale.ROOT);
    }

    private UUID currentEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth != null ? auth.getPrincipal() : null;
        return principal instanceof AuthenticatedUser au ? au.employeeId() : null;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "system" : auth.getName();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
