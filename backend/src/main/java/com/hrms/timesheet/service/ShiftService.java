package com.hrms.timesheet.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.domain.Shift;
import com.hrms.timesheet.dto.ShiftDto;
import com.hrms.timesheet.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** CRUD for working shifts (FTDD Vol.1 Ch.4). Company-scoped. */
@Service
@Transactional
public class ShiftService {

    private final ShiftRepository repository;

    public ShiftService(ShiftRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderByCode(companyId).stream().map(this::toDto).toList();
    }

    public ShiftDto create(ShiftDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        if (repository.existsByCompanyIdAndCode(companyId, dto.getCode())) {
            throw new BusinessRuleException("shift.code.duplicate", "Shift code already exists: " + dto.getCode());
        }
        Shift entity = new Shift();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public ShiftDto update(UUID id, ShiftDto dto) {
        Shift entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Shift getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + id));
    }

    private void apply(ShiftDto dto, Shift e) {
        e.setCode(dto.getCode());
        e.setName(dto.getName());
        e.setStartTime(dto.getStartTime());
        e.setEndTime(dto.getEndTime());
        e.setBreakMinutes(dto.getBreakMinutes());
        e.setStandardHours(dto.getStandardHours());
        e.setCrossesMidnight(dto.isCrossesMidnight());
        e.setWeeklyOff(dto.getWeeklyOff());
        e.setEffectiveFrom(dto.getEffectiveFrom() != null ? dto.getEffectiveFrom() : LocalDate.of(2020, 1, 1));
        e.setEffectiveTo(dto.getEffectiveTo());
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
    }

    private ShiftDto toDto(Shift e) {
        ShiftDto dto = new ShiftDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setStartTime(e.getStartTime());
        dto.setEndTime(e.getEndTime());
        dto.setBreakMinutes(e.getBreakMinutes());
        dto.setStandardHours(e.getStandardHours());
        dto.setCrossesMidnight(e.isCrossesMidnight());
        dto.setWeeklyOff(e.getWeeklyOff());
        dto.setEffectiveFrom(e.getEffectiveFrom());
        dto.setEffectiveTo(e.getEffectiveTo());
        dto.setStatus(e.getStatus());
        return dto;
    }
}
