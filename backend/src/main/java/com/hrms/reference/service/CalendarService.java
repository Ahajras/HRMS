package com.hrms.reference.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.reference.domain.Calendar;
import com.hrms.reference.dto.CalendarDto;
import com.hrms.reference.repository.CalendarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for yearly calendars. Company-scoped via {@link TenantContext} when a
 * tenant is present; otherwise treated as a global calendar.
 */
@Service
@Transactional
public class CalendarService {

    private final CalendarRepository repository;

    public CalendarService(CalendarRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CalendarDto> findAll() {
        UUID companyId = TenantContext.getCompanyId().orElse(null);
        return repository.findByCompanyId(companyId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CalendarDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public CalendarDto create(CalendarDto dto) {
        Calendar entity = new Calendar();
        entity.setCompanyId(TenantContext.getCompanyId().orElse(dto.getCompanyId()));
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public CalendarDto update(UUID id, CalendarDto dto) {
        Calendar entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Calendar getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar not found: " + id));
    }

    private void apply(CalendarDto dto, Calendar entity) {
        entity.setYear(dto.getYear());
        entity.setName(dto.getName());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private CalendarDto toDto(Calendar entity) {
        CalendarDto dto = new CalendarDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setYear(entity.getYear());
        dto.setName(entity.getName());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
