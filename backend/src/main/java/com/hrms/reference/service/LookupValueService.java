package com.hrms.reference.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.reference.domain.LookupValue;
import com.hrms.reference.dto.LookupValueDto;
import com.hrms.reference.repository.LookupValueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read/CRUD for configurable code lists (dropdown sources). Categories such as
 * GENDER, MARITAL_STATUS, CONTRACT_TYPE, EMPLOYEE_STATUS, DOCUMENT_TYPE are
 * configuration, not code (FTDD configuration-first principle).
 */
@Service
@Transactional
public class LookupValueService {

    private final LookupValueRepository repository;

    public LookupValueService(LookupValueRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<LookupValueDto> findByCategory(String category) {
        return repository.findByCategoryOrderBySortOrderAscLabelAsc(category)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LookupValueDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LookupValueDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public LookupValueDto create(LookupValueDto dto) {
        LookupValue entity = new LookupValue();
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public LookupValueDto update(UUID id, LookupValueDto dto) {
        LookupValue entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private LookupValue getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lookup value not found: " + id));
    }

    private void apply(LookupValueDto dto, LookupValue entity) {
        entity.setCompanyId(dto.getCompanyId());
        entity.setCategory(dto.getCategory());
        entity.setCode(dto.getCode());
        entity.setLabel(dto.getLabel());
        entity.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private LookupValueDto toDto(LookupValue entity) {
        LookupValueDto dto = new LookupValueDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setCategory(entity.getCategory());
        dto.setCode(entity.getCode());
        dto.setLabel(entity.getLabel());
        dto.setSortOrder(entity.getSortOrder());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
