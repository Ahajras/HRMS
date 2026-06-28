package com.hrms.timesheet.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.domain.PublicHoliday;
import com.hrms.timesheet.dto.PublicHolidayDto;
import com.hrms.timesheet.repository.PublicHolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** CRUD for the company public-holiday calendar (FTDD Vol.1 Ch.4). */
@Service
@Transactional
public class PublicHolidayService {

    private final PublicHolidayRepository repository;

    public PublicHolidayService(PublicHolidayRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PublicHolidayDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderByHolidayDate(companyId).stream().map(this::toDto).toList();
    }

    public PublicHolidayDto create(PublicHolidayDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        PublicHoliday entity = new PublicHoliday();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public PublicHolidayDto update(UUID id, PublicHolidayDto dto) {
        PublicHoliday entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private PublicHoliday getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public holiday not found: " + id));
    }

    private void apply(PublicHolidayDto dto, PublicHoliday e) {
        e.setHolidayDate(dto.getHolidayDate());
        e.setName(dto.getName());
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
    }

    private PublicHolidayDto toDto(PublicHoliday e) {
        PublicHolidayDto dto = new PublicHolidayDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setHolidayDate(e.getHolidayDate());
        dto.setName(e.getName());
        dto.setStatus(e.getStatus());
        return dto;
    }
}
