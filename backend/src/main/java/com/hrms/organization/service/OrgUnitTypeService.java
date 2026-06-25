package com.hrms.organization.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.organization.domain.OrgUnitType;
import com.hrms.organization.dto.OrgUnitTypeDto;
import com.hrms.organization.repository.OrgUnitTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Manages the configurable set of organisation levels (FTDD Vol.2 Ch.32.7).
 * When no company is in context the global default level set is returned.
 */
@Service
@Transactional
public class OrgUnitTypeService {

    private final OrgUnitTypeRepository repository;

    public OrgUnitTypeService(OrgUnitTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<OrgUnitTypeDto> findAll() {
        UUID companyId = TenantContext.getCompanyId().orElse(null);
        List<OrgUnitType> levels = companyId == null
                ? repository.findByCompanyIdIsNullOrderByLevelOrder()
                : repository.findByCompanyIdIsNullOrCompanyIdOrderByLevelOrder(companyId);
        return levels.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public OrgUnitTypeDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public OrgUnitTypeDto create(OrgUnitTypeDto dto) {
        OrgUnitType entity = new OrgUnitType();
        entity.setCompanyId(TenantContext.getCompanyId().orElse(dto.getCompanyId()));
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public OrgUnitTypeDto update(UUID id, OrgUnitTypeDto dto) {
        OrgUnitType entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private OrgUnitType getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Org unit type not found: " + id));
    }

    private void apply(OrgUnitTypeDto dto, OrgUnitType entity) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setLevelOrder(dto.getLevelOrder());
        entity.setMandatory(dto.isMandatory());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private OrgUnitTypeDto toDto(OrgUnitType entity) {
        OrgUnitTypeDto dto = new OrgUnitTypeDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setLevelOrder(entity.getLevelOrder());
        dto.setMandatory(entity.isMandatory());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
