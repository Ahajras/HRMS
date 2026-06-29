package com.hrms.reference.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.reference.domain.OvertimeCategory;
import com.hrms.reference.dto.OvertimeCategoryDto;
import com.hrms.reference.repository.OvertimeCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OvertimeCategoryService {

    private final OvertimeCategoryRepository repository;

    public OvertimeCategoryService(OvertimeCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<OvertimeCategoryDto> findAll() {
        return repository.findByCompanyIdOrderByCode(TenantContext.requireCompanyId())
                .stream().map(this::toDto).toList();
    }

    public OvertimeCategoryDto create(OvertimeCategoryDto dto) {
        OvertimeCategory entity = new OvertimeCategory();
        entity.setCompanyId(TenantContext.requireCompanyId());
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public OvertimeCategoryDto update(UUID id, OvertimeCategoryDto dto) {
        OvertimeCategory entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private OvertimeCategory getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime category not found: " + id));
    }

    private void apply(OvertimeCategoryDto dto, OvertimeCategory entity) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setOtEligible(dto.isOtEligible());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private OvertimeCategoryDto toDto(OvertimeCategory e) {
        OvertimeCategoryDto dto = new OvertimeCategoryDto();
        dto.setId(e.getId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setOtEligible(e.isOtEligible());
        dto.setStatus(e.getStatus());
        return dto;
    }
}
