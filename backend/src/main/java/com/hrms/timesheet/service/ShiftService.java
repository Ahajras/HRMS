package com.hrms.timesheet.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.domain.Shift;
import com.hrms.timesheet.domain.ShiftDay;
import com.hrms.timesheet.dto.ShiftDayDto;
import com.hrms.timesheet.dto.ShiftDto;
import com.hrms.timesheet.repository.ShiftDayRepository;
import com.hrms.timesheet.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** CRUD for working shifts + their sample week (FTDD Vol.1 Ch.4). Company-scoped. */
@Service
@Transactional
public class ShiftService {

    private static final List<String> DOW = List.of("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI");

    private final ShiftRepository repository;
    private final ShiftDayRepository dayRepository;

    public ShiftService(ShiftRepository repository, ShiftDayRepository dayRepository) {
        this.repository = repository;
        this.dayRepository = dayRepository;
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
        Shift saved = repository.save(entity);
        saveDays(saved, dto.getDays());
        return toDto(saved);
    }

    public ShiftDto update(UUID id, ShiftDto dto) {
        Shift entity = getEntity(id);
        apply(dto, entity);
        Shift saved = repository.save(entity);
        saveDays(saved, dto.getDays());
        return toDto(saved);
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id)); // shift_day cascades in DB
    }

    /** Persist the sample week (replace-all) and keep the weekly_off string in sync. */
    private void saveDays(Shift shift, List<ShiftDayDto> days) {
        if (days == null) {
            return;
        }
        dayRepository.deleteByShiftId(shift.getId());
        List<String> off = new ArrayList<>();
        for (ShiftDayDto d : days) {
            if (d.getDayOfWeek() == null) {
                continue;
            }
            ShiftDay e = new ShiftDay();
            e.setCompanyId(shift.getCompanyId());
            e.setShiftId(shift.getId());
            e.setDayOfWeek(d.getDayOfWeek());
            e.setNormalHours(d.getNormalHours() != null ? d.getNormalHours() : BigDecimal.ZERO);
            e.setDeclaredOt(d.getDeclaredOt() != null ? d.getDeclaredOt() : BigDecimal.ZERO);
            e.setWeeklyOff(d.isWeeklyOff());
            dayRepository.save(e);
            if (d.isWeeklyOff()) {
                off.add(d.getDayOfWeek());
            }
        }
        // Keep the legacy weekly_off CSV in sync so day classification still works.
        shift.setWeeklyOff(String.join(",", off));
        repository.save(shift);
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
        dto.setDays(loadDays(e.getId()));
        return dto;
    }

    /** Sample-week rows ordered Sat..Fri. */
    private List<ShiftDayDto> loadDays(UUID shiftId) {
        var byDow = dayRepository.findByShiftId(shiftId).stream()
                .collect(Collectors.toMap(ShiftDay::getDayOfWeek, d -> d));
        List<ShiftDayDto> out = new ArrayList<>();
        for (String dow : DOW) {
            ShiftDay d = byDow.get(dow);
            if (d == null) {
                continue;
            }
            ShiftDayDto dd = new ShiftDayDto();
            dd.setId(d.getId());
            dd.setDayOfWeek(d.getDayOfWeek());
            dd.setNormalHours(d.getNormalHours());
            dd.setDeclaredOt(d.getDeclaredOt());
            dd.setWeeklyOff(d.isWeeklyOff());
            out.add(dd);
        }
        return out;
    }
}
