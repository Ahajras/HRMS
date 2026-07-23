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
        entity.setPrjcode(normalizeNullable(dto.getPrjcode()));
        entity.setCode(normalize(dto.getCode()));
        String description = blankToNull(dto.getDescription());
        entity.setName(description != null ? description : dto.getName());
        entity.setCurrencyCode(normalizeCurrency(dto.getCurrencyCode()));
        entity.setDescription(description);

        String status = normalize(dto.getStatus());
        Boolean active = dto.getActive();
        if (status == null && active == null) {
            status = "ACTIVE";
            active = true;
        } else if (status == null) {
            status = active ? "ACTIVE" : "INACTIVE";
        } else if (active == null) {
            active = "ACTIVE".equalsIgnoreCase(status);
        }
        entity.setStatus(status);
        entity.setActive(active);
    }

    private CostCodeDto toDto(CostCode e) {
        CostCodeDto dto = new CostCodeDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setProjectId(e.getProjectId());
        dto.setPrjcode(e.getPrjcode());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setCurrencyCode(e.getCurrencyCode());
        dto.setDescription(e.getDescription());
        dto.setActive(e.getActive());
        dto.setStatus(e.getStatus());
        return dto;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String normalizeCurrency(String value) {
        String normalized = normalize(value);
        return normalized == null || normalized.isBlank() ? "QAR" : normalized;
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }
}
