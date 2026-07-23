package com.hrms.project.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.project.domain.CostCode;
import com.hrms.project.dto.CostCodeDto;
import com.hrms.project.repository.CostCodeRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CostCodeService {

    private final CostCodeRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public CostCodeService(CostCodeRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
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
        if (isReferenced(id) && changesProtectedFields(entity, dto)) {
            throw new BusinessRuleException("COST_CODE_IN_USE",
                    "Cost code cannot be edited because it is already used in assignments or timesheets. Create a new cost code, or update/remove the related records first. You can still set this cost code to Inactive.");
        }
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        CostCode entity = getEntity(id);
        if (isReferenced(id)) {
            throw new BusinessRuleException("COST_CODE_IN_USE",
                    "Cost code cannot be deleted because it is already used in assignments or timesheets. Remove those references first, or set the cost code to Inactive.");
        }
        repository.delete(entity);
        repository.flush();
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

    private boolean isReferenced(UUID id) {
        return countReferences("assignment", id)
                + countReferences("timesheet_day", id)
                + countReferences("timesheet_day_cost", id) > 0;
    }

    private long countReferences(String table, UUID id) {
        Long count = jdbcTemplate.queryForObject("select count(*) from " + table + " where cost_code_id = ?", Long.class, id);
        return count == null ? 0 : count;
    }

    private boolean changesProtectedFields(CostCode entity, CostCodeDto dto) {
        String description = blankToNull(dto.getDescription());
        String name = description != null ? description : dto.getName();
        return !Objects.equals(entity.getProjectId(), dto.getProjectId())
                || !Objects.equals(entity.getPrjcode(), normalizeNullable(dto.getPrjcode()))
                || !Objects.equals(entity.getCode(), normalize(dto.getCode()))
                || !Objects.equals(entity.getName(), name)
                || !Objects.equals(entity.getDescription(), description)
                || !Objects.equals(entity.getCurrencyCode(), normalizeCurrency(dto.getCurrencyCode()));
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
