package com.hrms.project.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.project.domain.Project;
import com.hrms.project.domain.ProjectApprovalRole;
import com.hrms.project.dto.ProjectApprovalRoleDto;
import com.hrms.project.dto.RoleEmployeeCandidateDto;
import com.hrms.project.repository.ProjectApprovalRoleRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.security.domain.AppUser;
import com.hrms.security.domain.Role;
import com.hrms.security.repository.AppUserRepository;
import com.hrms.security.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ProjectApprovalRoleService {

    private final ProjectApprovalRoleRepository repository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;

    public ProjectApprovalRoleService(ProjectApprovalRoleRepository repository,
                                      ProjectRepository projectRepository,
                                      EmployeeRepository employeeRepository,
                                      AppUserRepository userRepository,
                                      RoleRepository roleRepository) {
        this.repository = repository;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectApprovalRoleDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderByProjectIdAscRoleCodeAsc(companyId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleEmployeeCandidateDto> candidates(String roleCode) {
        UUID companyId = TenantContext.requireCompanyId();
        String code = normalizeRole(roleCode);
        return userRepository.findByCompanyId(companyId).stream()
                .filter(user -> user.getEmployeeId() != null)
                .filter(user -> hasRole(user, code))
                .map(this::candidate)
                .filter(c -> c.getEmployeeId() != null)
                .sorted(Comparator.comparing(c -> nullSafe(c.getEmployeeNumber())))
                .toList();
    }

    public ProjectApprovalRoleDto create(ProjectApprovalRoleDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        String roleCode = normalizeRole(dto.getRoleCode());
        roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessRuleException("project.approval.role.unknown", "Unknown role: " + roleCode));
        Project project = projectRepository.findById(dto.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + dto.getProjectId()));
        if (!companyId.equals(project.getCompanyId())) {
            throw new ResourceNotFoundException("Project not found: " + dto.getProjectId());
        }
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + dto.getEmployeeId()));
        if (!companyId.equals(employee.getCompanyId())) {
            throw new ResourceNotFoundException("Employee not found: " + dto.getEmployeeId());
        }
        boolean eligible = candidates(roleCode).stream().anyMatch(c -> dto.getEmployeeId().equals(c.getEmployeeId()));
        if (!eligible) {
            throw new BusinessRuleException("project.approval.role.employee",
                    "This employee must be linked to an active user with role " + roleCode + ".");
        }
        if (repository.existsByCompanyIdAndProjectIdAndRoleCodeAndEmployeeId(companyId, dto.getProjectId(), roleCode, dto.getEmployeeId())) {
            throw new BusinessRuleException("project.approval.role.duplicate",
                    "This employee already has this approval role on the project.");
        }
        ProjectApprovalRole row = new ProjectApprovalRole();
        row.setCompanyId(companyId);
        row.setProjectId(dto.getProjectId());
        row.setRoleCode(roleCode);
        row.setEmployeeId(dto.getEmployeeId());
        row.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? "ACTIVE" : dto.getStatus());
        return toDto(repository.save(row));
    }

    public void delete(UUID id) {
        repository.findById(id).ifPresent(repository::delete);
    }

    private boolean hasRole(AppUser user, String code) {
        return user.getRoles().stream().map(Role::getCode).anyMatch(code::equalsIgnoreCase);
    }

    private RoleEmployeeCandidateDto candidate(AppUser user) {
        RoleEmployeeCandidateDto dto = new RoleEmployeeCandidateDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        employeeRepository.findById(user.getEmployeeId()).ifPresent(employee -> {
            dto.setEmployeeId(employee.getId());
            dto.setEmployeeNumber(employee.getEmployeeNumber());
            dto.setEmployeeName(name(employee));
        });
        return dto;
    }

    private ProjectApprovalRoleDto toDto(ProjectApprovalRole row) {
        ProjectApprovalRoleDto dto = new ProjectApprovalRoleDto();
        dto.setId(row.getId());
        dto.setProjectId(row.getProjectId());
        dto.setRoleCode(row.getRoleCode());
        dto.setEmployeeId(row.getEmployeeId());
        dto.setStatus(row.getStatus());
        projectRepository.findById(row.getProjectId()).ifPresent(project -> dto.setProjectCode(project.getCode()));
        employeeRepository.findById(row.getEmployeeId()).ifPresent(employee -> {
            dto.setEmployeeNumber(employee.getEmployeeNumber());
            dto.setEmployeeName(name(employee));
        });
        return dto;
    }

    private String name(Employee employee) {
        return (nullSafe(employee.getFirstName()) + " " + nullSafe(employee.getLastName())).trim();
    }

    private String normalizeRole(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
