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

    private final ApprovalWorkflowRepository workflowRepo;
    private final ApprovalWorkflowStepRepository workflowStepRepo;
    private final ApprovalInstanceRepository instanceRepo;
    private final ApprovalInstanceStepRepository instanceStepRepo;
    private final EmployeeRepository employeeRepo;
    private final ProjectApprovalRoleRepository projectApprovalRoleRepo;
    private final ProjectRepository projectRepo;
    private final TimesheetRepository timesheetRepo;
    private final ApprovalNotificationService notificationService;

    public ApprovalService(ApprovalWorkflowRepository workflowRepo,
                           ApprovalWorkflowStepRepository workflowStepRepo,
                           ApprovalInstanceRepository instanceRepo,
                           ApprovalInstanceStepRepository instanceStepRepo,
                           EmployeeRepository employeeRepo,
                           ProjectApprovalRoleRepository projectApprovalRoleRepo,
                           ProjectRepository projectRepo,
                           TimesheetRepository timesheetRepo,
                           ApprovalNotificationService notificationService) {
        this.workflowRepo = workflowRepo;
        this.workflowStepRepo = workflowStepRepo;
        this.instanceRepo = instanceRepo;
        this.instanceStepRepo = instanceStepRepo;
        this.employeeRepo = employeeRepo;
        this.projectApprovalRoleRepo = projectApprovalRoleRepo;
        this.projectRepo = projectRepo;
        this.timesheetRepo = timesheetRepo;
        this.notificationService = notificationService;
    }

    public void startTimesheetApproval(UUID timesheetId, UUID employeeId, UUID projectId, String payGroup) {
        UUID companyId = TenantContext.requireCompanyId();
        if (instanceRepo.findFirstByCompanyIdAndEntityTypeAndEntityIdAndStatusIn(
                companyId, TIMESHEET_ENTITY, timesheetId, List.of("PENDING", "APPROVED")).isPresent()) {
            return;
        }
        ApprovalWorkflow workflow = workflowRepo
                .findFirstByCompanyIdAndProcessCodeAndProjectIdAndPayGroupAndStatus(
                        companyId, TIMESHEET_PROCESS, projectId, normalizePayGroup(payGroup), "ACTIVE")
                .or(() -> workflowRepo.findFirstByCompanyIdAndProcessCodeAndProjectIdAndPayGroupAndStatus(
                        companyId, TIMESHEET_PROCESS, projectId, "ALL", "ACTIVE"))
                .orElseThrow(() -> new BusinessRuleException("approval.workflow.required",
                        "Define an active timesheet approval workflow for this project before submitting."));
        List<ApprovalWorkflowStep> steps = workflowStepRepo.findByWorkflowIdAndStatusOrderByStepOrderAsc(workflow.getId(), "ACTIVE");
        if (steps.isEmpty()) {
            throw new BusinessRuleException("approval.workflow.steps.required", "Approval workflow has no active steps.");
        }

        ApprovalInstance instance = new ApprovalInstance();
        instance.setCompanyId(companyId);
        instance.setWorkflowId(workflow.getId());
        instance.setProcessCode(TIMESHEET_PROCESS);
        instance.setEntityType(TIMESHEET_ENTITY);
        instance.setEntityId(timesheetId);
        instance.setProjectId(projectId);
        instance.setEmployeeId(employeeId);
        instance.setStatus("PENDING");
        instance.setCurrentStepOrder(steps.get(0).getStepOrder());
        instance.setSubmittedBy(currentUsername());
        instance.setSubmittedAt(Instant.now());
        instance = instanceRepo.save(instance);

        ApprovalInstanceStep firstPending = null;
        for (int i = 0; i < steps.size(); i++) {
            ApprovalWorkflowStep src = steps.get(i);
            ApprovalInstanceStep step = new ApprovalInstanceStep();
            step.setInstanceId(instance.getId());
            step.setStepOrder(src.getStepOrder());
            step.setName(src.getName());
            step.setApproverType(src.getApproverType());
            step.setApproverRoleCode(src.getApproverRoleCode());
            step.setApproverEmployeeId(resolveApproverEmployee(src, employeeId, projectId));
            step.setStatus(i == 0 ? "PENDING" : "WAITING");
            step = instanceStepRepo.save(step);
            if (i == 0) {
                firstPending = step;
            }
        }
        if (firstPending != null) {
            notificationService.notifyPending(instance, firstPending);
        }
    }

    public boolean approveTimesheetStep(UUID timesheetId) {
        UUID companyId = TenantContext.requireCompanyId();
        ApprovalInstance instance = instanceRepo.findFirstByCompanyIdAndEntityTypeAndEntityIdAndStatusIn(
                        companyId, TIMESHEET_ENTITY, timesheetId, List.of("PENDING"))
                .orElseThrow(() -> new BusinessRuleException("approval.instance.required",
                        "This timesheet has no pending approval workflow."));
        ApprovalInstanceStep current = instanceStepRepo.findByInstanceIdAndStatus(instance.getId(), "PENDING")
                .orElseThrow(() -> new BusinessRuleException("approval.step.required", "No pending approval step found."));
        UUID currentEmployeeId = currentEmployeeId();
        if (currentEmployeeId == null || !currentEmployeeId.equals(current.getApproverEmployeeId())) {
            throw new BusinessRuleException("approval.step.unauthorized",
                    "This approval is waiting for another approver.");
        }
        current.setStatus("APPROVED");
        current.setDecidedAt(Instant.now());
        current.setDecidedBy(currentUsername());
        instanceStepRepo.save(current);

        List<ApprovalInstanceStep> steps = instanceStepRepo.findByInstanceIdOrderByStepOrderAsc(instance.getId());
        ApprovalInstanceStep next = steps.stream()
                .filter(s -> "WAITING".equals(s.getStatus()))
                .min(Comparator.comparingInt(ApprovalInstanceStep::getStepOrder))
                .orElse(null);
        if (next == null) {
            instance.setStatus("APPROVED");
            instance.setCompletedAt(Instant.now());
            instanceRepo.save(instance);
            return true;
        }
        next.setStatus("PENDING");
        instanceStepRepo.save(next);
        instance.setCurrentStepOrder(next.getStepOrder());
        instanceRepo.save(instance);
        notificationService.notifyPending(instance, next);
        return false;
    }

    public void voidTimesheetApproval(UUID timesheetId) {
        UUID companyId = TenantContext.requireCompanyId();
        instanceRepo.findFirstByCompanyIdAndEntityTypeAndEntityIdAndStatusIn(
                companyId, TIMESHEET_ENTITY, timesheetId, List.of("PENDING", "APPROVED")).ifPresent(instance -> {
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
            ApprovalInstanceStep step = instanceStepRepo.findByInstanceIdAndStatus(instance.getId(), "PENDING").orElse(null);
            if (step == null || !employeeId.equals(step.getApproverEmployeeId())) {
                continue;
            }
            out.add(toTask(instance, step));
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
        return dto;
    }

    private UUID resolveApproverEmployee(ApprovalWorkflowStep step, UUID employeeId, UUID projectId) {
        String type = step.getApproverType() == null ? "" : step.getApproverType().toUpperCase(Locale.ROOT);
        if ("SPECIFIC_EMPLOYEE".equals(type)) {
            return require(step.getApproverEmployeeId(), "Specific approval employee is not configured.");
        }
        if ("SUPERVISOR".equals(type)) {
            Employee employee = employeeRepo.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
            return require(employee.getSupervisorEmployeeId(), "Employee has no supervisor configured for approval.");
        }
        if ("PROJECT_ROLE".equals(type)) {
            List<ProjectApprovalRole> rows = projectApprovalRoleRepo.findByCompanyIdAndProjectIdAndRoleCodeAndStatus(
                    TenantContext.requireCompanyId(), projectId, step.getApproverRoleCode(), "ACTIVE");
            return rows.stream().map(ProjectApprovalRole::getEmployeeId).findFirst()
                    .orElseThrow(() -> new BusinessRuleException("approval.project.role.required",
                            "No active " + step.getApproverRoleCode() + " approver is assigned to this project."));
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
