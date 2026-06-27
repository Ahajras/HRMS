package com.hrms.project.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.project.domain.Project;
import com.hrms.project.dto.ProjectDto;
import com.hrms.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository repository;

    public ProjectService(ProjectRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAll() {
        return repository.findByCompanyIdOrderByName(TenantContext.requireCompanyId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public ProjectDto create(ProjectDto dto) {
        Project entity = new Project();
        entity.setCompanyId(TenantContext.requireCompanyId());
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public ProjectDto update(UUID id, ProjectDto dto) {
        Project entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Project getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
    }

    private void apply(ProjectDto dto, Project entity) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setManagerEmployeeId(dto.getManagerEmployeeId());
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
        dto.setStatus(e.getStatus());
        return dto;
    }
}
