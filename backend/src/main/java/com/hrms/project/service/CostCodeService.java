package com.hrms.project.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.project.domain.CostCode;
import com.hrms.project.dto.CostCodeDto;
import com.hrms.project.repository.CostCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CostCodeService {

    private final CostCodeRepository repository;

    public CostCodeService(CostCodeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CostCodeDto> findByProject(UUID projectId) {
        return repository.findByProjectIdOrderByCode(projectId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CostCodeDto> findAll() {
        return repository.findByCompanyIdOrderByCode(TenantContext.requireCompanyId())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CostCodeDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public CostCodeDto create(CostCodeDto dto) {
        CostCode entity = new CostCode();
        entity.setCompanyId(TenantContext.requireCompanyId());
        entity.setProjectId(dto.getProjectId());
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public CostCodeDto update(UUID id, CostCodeDto dto) {
        CostCode entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private CostCode getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cost code not found: " + id));
    }

    private void apply(CostCodeDto dto, CostCode entity) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private CostCodeDto toDto(CostCode e) {
        CostCodeDto dto = new CostCodeDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setProjectId(e.getProjectId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setStatus(e.getStatus());
        return dto;
    }
}
