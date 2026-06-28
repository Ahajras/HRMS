package com.hrms.timesheet.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.domain.PayrollCalendar;
import com.hrms.timesheet.dto.PayrollCalendarDto;
import com.hrms.timesheet.repository.PayrollCalendarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** CRUD for payroll calendars (FTDD Vol.1 Ch.4). Company-scoped. */
@Service
@Transactional
public class PayrollCalendarService {

    private final PayrollCalendarRepository repository;

    public PayrollCalendarService(PayrollCalendarRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PayrollCalendarDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderByCode(companyId).stream().map(this::toDto).toList();
    }

    public PayrollCalendarDto create(PayrollCalendarDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        if (repository.findByCompanyIdAndCode(companyId, dto.getCode()).isPresent()) {
            throw new BusinessRuleException("calendar.code.duplicate", "Calendar code already exists: " + dto.getCode());
        }
        PayrollCalendar entity = new PayrollCalendar();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public PayrollCalendarDto update(UUID id, PayrollCalendarDto dto) {
        PayrollCalendar entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    /** Resolve a usable calendar: the requested one, else the company default/first. */
    public PayrollCalendar resolve(UUID companyId, UUID calendarId) {
        if (calendarId != null) {
            return getEntity(calendarId);
        }
        return repository.findByCompanyIdAndCode(companyId, "DEFAULT")
                .or(() -> repository.findByCompanyIdOrderByCode(companyId).stream().findFirst())
                .orElseThrow(() -> new BusinessRuleException("calendar.none",
                        "No payroll calendar configured for this company. Create one first."));
    }

    private PayrollCalendar getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar not found: " + id));
    }

    private void apply(PayrollCalendarDto dto, PayrollCalendar e) {
        e.setCode(dto.getCode());
        e.setName(dto.getName());
        if (dto.getFrequency() != null) {
            e.setFrequency(dto.getFrequency());
        }
        if (dto.getWeekStart() != null) {
            e.setWeekStart(dto.getWeekStart());
        }
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
    }

    private PayrollCalendarDto toDto(PayrollCalendar e) {
        PayrollCalendarDto dto = new PayrollCalendarDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setFrequency(e.getFrequency());
        dto.setWeekStart(e.getWeekStart());
        dto.setStatus(e.getStatus());
        return dto;
    }
}
