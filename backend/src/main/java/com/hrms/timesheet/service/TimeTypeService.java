package com.hrms.timesheet.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.domain.TimeType;
import com.hrms.timesheet.dto.TimeTypeDto;
import com.hrms.timesheet.repository.TimeTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** CRUD for configurable time types (FTDD Vol.1 Ch.5). Company-scoped. */
@Service
@Transactional
public class TimeTypeService {

    private final TimeTypeRepository repository;

    public TimeTypeService(TimeTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TimeTypeDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderBySortOrderAscNameAsc(companyId).stream().map(this::toDto).toList();
    }

    public TimeTypeDto create(TimeTypeDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        if (repository.existsByCompanyIdAndCode(companyId, dto.getCode())) {
            throw new BusinessRuleException("timetype.code.duplicate", "Time type code already exists: " + dto.getCode());
        }
        TimeType entity = new TimeType();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public TimeTypeDto update(UUID id, TimeTypeDto dto) {
        TimeType entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private TimeType getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time type not found: " + id));
    }

    private void apply(TimeTypeDto dto, TimeType e) {
        e.setCode(dto.getCode());
        e.setName(dto.getName());
        e.setCategory(dto.getCategory());
        e.setPaid(dto.isPaid());
        e.setCountsAsWorked(dto.isCountsAsWorked());
        e.setAffectsLeave(dto.isAffectsLeave());
        if (dto.getFactor() != null) {
            e.setFactor(dto.getFactor());
        }
        e.setSortOrder(dto.getSortOrder());
        e.setColorHex(normalizeColor(dto.getColorHex()));
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
    }

    private TimeTypeDto toDto(TimeType e) {
        TimeTypeDto dto = new TimeTypeDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setCategory(e.getCategory());
        dto.setPaid(e.isPaid());
        dto.setCountsAsWorked(e.isCountsAsWorked());
        dto.setAffectsLeave(e.isAffectsLeave());
        dto.setFactor(e.getFactor());
        dto.setSortOrder(e.getSortOrder());
        dto.setColorHex(e.getColorHex());
        dto.setStatus(e.getStatus());
        return dto;
    }

    private String normalizeColor(String raw) {
        if (raw == null || raw.isBlank()) {
            return "#64748b";
        }
        String color = raw.trim();
        return color.matches("^#[0-9A-Fa-f]{6}$") ? color : "#64748b";
    }
}
