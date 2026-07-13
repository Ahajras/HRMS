package com.hrms.project.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.crew.service.TimekeeperService;
import com.hrms.payroll.service.PayrollRuleService;
import com.hrms.project.domain.Project;
import com.hrms.project.dto.ProjectDto;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.security.AuthenticatedUser;
import com.hrms.security.domain.AppUser;
import com.hrms.security.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository repository;
    private final TimekeeperService timekeeperService;
    private final AppUserRepository appUserRepo;
    private final PayrollRuleService payrollRuleService;

    public ProjectService(ProjectRepository repository, TimekeeperService timekeeperService,
                          AppUserRepository appUserRepo,
                          PayrollRuleService payrollRuleService) {
        this.repository = repository;
        this.timekeeperService = timekeeperService;
        this.appUserRepo = appUserRepo;
        this.payrollRuleService = payrollRuleService;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        Set<UUID> allowed = restrictedProjects();
        return (allowed == null
                ? repository.findByCompanyIdOrderByName(companyId)
                : repository.findByCompanyIdAndIdInOrderByName(companyId, allowed))
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto findById(UUID id) {
        assertProjectAllowed(id);
        return toDto(getEntity(id));
    }

    public ProjectDto create(ProjectDto dto) {
        Project entity = new Project();
        UUID companyId = TenantContext.requireCompanyId();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        Project saved = repository.save(entity);
        payrollRuleService.initializeDefaultsForProject(companyId, saved.getId());
        return toDto(saved);
    }

    public ProjectDto update(UUID id, ProjectDto dto) {
        assertProjectAllowed(id);
        Project entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        assertProjectAllowed(id);
        repository.delete(getEntity(id));
    }

    private Project getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    private Set<UUID> restrictedProjects() {
        UUID empId = currentEmployeeId();
        if (empId == null) {
            return null;
        }
        List<UUID> projs = timekeeperService.allowedProjectIds(empId);
        return projs.isEmpty() ? null : new HashSet<>(projs);
    }

    private UUID currentEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth != null ? auth.getPrincipal() : null;
        if (principal instanceof AuthenticatedUser user && user.userId() != null) {
            return appUserRepo.findById(user.userId()).map(AppUser::getEmployeeId).orElse(null);
        }
        return null;
    }

    private void assertProjectAllowed(UUID projectId) {
        Set<UUID> allowed = restrictedProjects();
        if (allowed != null && !allowed.contains(projectId)) {
            throw new ResourceNotFoundException("Project not found: " + projectId);
        }
    }

    private void apply(ProjectDto dto, Project entity) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setManagerEmployeeId(dto.getManagerEmployeeId());
        entity.setSponsorId(dto.getSponsorId());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private ProjectDto toDto(Project e) {
        ProjectDto dto = new ProjectDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setManagerEmployeeId(e.getManagerEmployeeId());
        dto.setSponsorId(e.getSponsorId());
        dto.setStatus(e.getStatus());
        return dto;
    }
}
