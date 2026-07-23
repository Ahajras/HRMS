package com.hrms.approval.service;

import com.hrms.approval.domain.ApprovalWorkflow;
import com.hrms.approval.domain.ApprovalWorkflowStep;
import com.hrms.approval.dto.ApprovalWorkflowDto;
import com.hrms.approval.repository.ApprovalInstanceRepository;
import com.hrms.approval.repository.ApprovalWorkflowRepository;
import com.hrms.approval.repository.ApprovalWorkflowStepRepository;
import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.project.domain.Project;
import com.hrms.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ApprovalWorkflowAdminService {
    private final ApprovalWorkflowRepository workflowRepo;
    private final ApprovalWorkflowStepRepository stepRepo;
    private final ApprovalInstanceRepository instanceRepo;
    private final ProjectRepository projectRepo;
    private final EmployeeRepository employeeRepo;

    public ApprovalWorkflowAdminService(ApprovalWorkflowRepository workflowRepo,
                                        ApprovalWorkflowStepRepository stepRepo,
                                        ApprovalInstanceRepository instanceRepo,
                                        ProjectRepository projectRepo,
                                        EmployeeRepository employeeRepo) {
        this.workflowRepo = workflowRepo;
        this.stepRepo = stepRepo;
        this.instanceRepo = instanceRepo;
        this.projectRepo = projectRepo;
        this.employeeRepo = employeeRepo;
    }

    @Transactional(readOnly = true)
    public List<ApprovalWorkflowDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return workflowRepo.findByCompanyIdAndStatusOrderByProcessCodeAscProjectIdAscPayGroupAsc(companyId, "ACTIVE")
                .stream().map(this::toDto).toList();
    }

    public ApprovalWorkflowDto save(ApprovalWorkflowDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        String processCode = normalize(dto.getProcessCode(), "TIMESHEET_SUBMIT");
        String payGroup = normalize(dto.getPayGroup(), "ALL");
        if (dto.getProjectId() == null) {
            throw new BusinessRuleException("approval.workflow.project.required", "Choose a project for this workflow.");
        }
        Project project = projectRepo.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
        if (!companyId.equals(project.getCompanyId())) {
            throw new ResourceNotFoundException("Project not found: " + dto.getProjectId());
        }
        ApprovalWorkflow target = workflowRepo.findByCompanyIdAndProcessCodeAndProjectIdAndPayGroup(companyId, processCode, dto.getProjectId(), payGroup)
                .orElse(null);
        ApprovalWorkflow workflow;
        if (dto.getId() == null) {
            workflow = target == null ? new ApprovalWorkflow() : target;
        } else {
            ApprovalWorkflow current = workflowRepo.findById(dto.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + dto.getId()));
            if (!companyId.equals(current.getCompanyId())) {
                throw new ResourceNotFoundException("Workflow not found: " + dto.getId());
            }
            if (target != null && !target.getId().equals(current.getId())) {
                retireWorkflow(current);
                workflow = target;
            } else {
                workflow = current;
            }
        }
        workflow.setCompanyId(companyId);
        workflow.setProcessCode(processCode);
        workflow.setProjectId(dto.getProjectId());
        workflow.setPayGroup(payGroup);
        workflow.setName(blank(dto.getName()) ? processCode + " - " + project.getCode() : dto.getName().trim());
        workflow.setStatus(blank(dto.getStatus()) ? "ACTIVE" : dto.getStatus().trim().toUpperCase(Locale.ROOT));
        workflow = workflowRepo.save(workflow);

        List<ApprovalWorkflowDto.StepDto> steps = dto.getSteps() == null ? List.of() : dto.getSteps();
        if (steps.isEmpty()) {
            throw new BusinessRuleException("approval.workflow.steps.required", "Add at least one approval step.");
        }
        stepRepo.deleteAll(stepRepo.findByWorkflowIdOrderByStepOrderAsc(workflow.getId()));
        stepRepo.flush();
        int order = 1;
        for (ApprovalWorkflowDto.StepDto s : steps.stream().sorted(Comparator.comparingInt(ApprovalWorkflowDto.StepDto::getStepOrder)).toList()) {
            String type = normalize(s.getApproverType(), "");
            if (type.isBlank()) {
                throw new BusinessRuleException("approval.workflow.step.type", "Choose an approver type for every step.");
            }
            ApprovalWorkflowStep row = new ApprovalWorkflowStep();
            row.setWorkflowId(workflow.getId());
            row.setStepOrder(order++);
            row.setName(blank(s.getName()) ? defaultStepName(type, s.getApproverRoleCode()) : s.getName().trim());
            row.setApproverType(type);
            row.setApproverRoleCode(blank(s.getApproverRoleCode()) ? null : s.getApproverRoleCode().trim().toUpperCase(Locale.ROOT));
            row.setApproverEmployeeId(s.getApproverEmployeeId());
            row.setStatus("ACTIVE");
            validateStep(row);
            stepRepo.save(row);
        }
        return toDto(workflow);
    }

    public void delete(UUID id) {
        ApprovalWorkflow workflow = workflowRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + id));
        if (instanceRepo.countByWorkflowId(id) == 0) {
            workflowRepo.delete(workflow);
            return;
        }
        workflow.setStatus("INACTIVE");
        workflowRepo.save(workflow);
        stepRepo.findByWorkflowIdAndStatusOrderByStepOrderAsc(id, "ACTIVE").forEach(step -> {
            step.setStatus("INACTIVE");
            stepRepo.save(step);
        });
    }

    private void retireWorkflow(ApprovalWorkflow workflow) {
        if (instanceRepo.countByWorkflowId(workflow.getId()) == 0) {
            workflowRepo.delete(workflow);
        } else {
            workflow.setStatus("INACTIVE");
            workflowRepo.save(workflow);
            stepRepo.findByWorkflowIdAndStatusOrderByStepOrderAsc(workflow.getId(), "ACTIVE").forEach(step -> {
                step.setStatus("INACTIVE");
                stepRepo.save(step);
            });
        }
        workflowRepo.flush();
        stepRepo.flush();
    }

    private void validateStep(ApprovalWorkflowStep row) {
        if ("PROJECT_ROLE".equals(row.getApproverType()) && blank(row.getApproverRoleCode())) {
            throw new BusinessRuleException("approval.workflow.step.role", "Project role step needs a role code.");
        }
        if ("SPECIFIC_EMPLOYEE".equals(row.getApproverType())) {
            if (row.getApproverEmployeeId() == null) {
                throw new BusinessRuleException("approval.workflow.step.employee", "Specific employee step needs an employee.");
            }
            employeeRepo.findById(row.getApproverEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + row.getApproverEmployeeId()));
        }
    }

    private ApprovalWorkflowDto toDto(ApprovalWorkflow row) {
        ApprovalWorkflowDto dto = new ApprovalWorkflowDto();
        dto.setId(row.getId());
        dto.setProcessCode(row.getProcessCode());
        dto.setProjectId(row.getProjectId());
        dto.setPayGroup(row.getPayGroup());
        dto.setName(row.getName());
        dto.setStatus(row.getStatus());
        if (row.getProjectId() != null) {
            projectRepo.findById(row.getProjectId()).ifPresent(p -> dto.setProjectCode(p.getCode()));
        }
        dto.setSteps(stepRepo.findByWorkflowIdAndStatusOrderByStepOrderAsc(row.getId(), "ACTIVE")
                .stream().map(this::toStepDto).toList());
        return dto;
    }

    private ApprovalWorkflowDto.StepDto toStepDto(ApprovalWorkflowStep row) {
        ApprovalWorkflowDto.StepDto dto = new ApprovalWorkflowDto.StepDto();
        dto.setId(row.getId());
        dto.setStepOrder(row.getStepOrder());
        dto.setName(row.getName());
        dto.setApproverType(row.getApproverType());
        dto.setApproverRoleCode(row.getApproverRoleCode());
        dto.setApproverEmployeeId(row.getApproverEmployeeId());
        dto.setStatus(row.getStatus());
        if (row.getApproverEmployeeId() != null) {
            employeeRepo.findById(row.getApproverEmployeeId()).ifPresent(e -> {
                dto.setApproverEmployeeNumber(e.getEmployeeNumber());
                dto.setApproverEmployeeName(name(e));
            });
        }
        return dto;
    }

    private String defaultStepName(String type, String role) {
        if ("SUPERVISOR".equals(type)) return "Direct supervisor";
        if ("PROJECT_ROLE".equals(type)) return "Project " + (blank(role) ? "role" : role);
        if ("SPECIFIC_EMPLOYEE".equals(type)) return "Specific employee";
        return type;
    }

    private String normalize(String value, String fallback) {
        return blank(value) ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String name(Employee e) {
        return ((e.getFirstName() == null ? "" : e.getFirstName()) + " " + (e.getLastName() == null ? "" : e.getLastName())).trim();
    }
}
