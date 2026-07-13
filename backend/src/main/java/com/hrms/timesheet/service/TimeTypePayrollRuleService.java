package com.hrms.timesheet.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.payroll.domain.PayrollComponent;
import com.hrms.payroll.repository.PayrollComponentRepository;
import com.hrms.timesheet.domain.TimeType;
import com.hrms.timesheet.domain.TimeTypePayrollRule;
import com.hrms.timesheet.dto.TimeTypePayrollRuleDto;
import com.hrms.timesheet.repository.TimeTypePayrollRuleRepository;
import com.hrms.timesheet.repository.TimeTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TimeTypePayrollRuleService {

    private final TimeTypePayrollRuleRepository repository;
    private final TimeTypeRepository timeTypeRepository;
    private final PayrollComponentRepository payrollComponentRepository;

    public TimeTypePayrollRuleService(TimeTypePayrollRuleRepository repository,
                                      TimeTypeRepository timeTypeRepository,
                                      PayrollComponentRepository payrollComponentRepository) {
        this.repository = repository;
        this.timeTypeRepository = timeTypeRepository;
        this.payrollComponentRepository = payrollComponentRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeTypePayrollRuleDto> findByTimeType(UUID timeTypeId) {
        UUID companyId = TenantContext.requireCompanyId();
        requireTimeType(companyId, timeTypeId);
        return repository.findByCompanyIdAndTimeTypeIdOrderBySortOrderAsc(companyId, timeTypeId).stream()
                .map(this::toDto)
                .toList();
    }

    public TimeTypePayrollRuleDto save(UUID timeTypeId, TimeTypePayrollRuleDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        requireTimeType(companyId, timeTypeId);
        PayrollComponent component = requireComponent(companyId, dto.getPayrollComponentId());
        TimeTypePayrollRule entity = repository
                .findByCompanyIdAndTimeTypeIdAndPayrollComponentId(companyId, timeTypeId, component.getId())
                .orElseGet(TimeTypePayrollRule::new);
        entity.setCompanyId(companyId);
        entity.setTimeTypeId(timeTypeId);
        entity.setPayrollComponentId(component.getId());
        entity.setAction(dto.getAction());
        entity.setPercent(dto.getPercent());
        entity.setBasis(dto.getBasis());
        entity.setThresholdDays(dto.getThresholdDays());
        entity.setThresholdScope(normalizeThresholdScope(dto.getThresholdScope(), dto.getThresholdDays()));
        entity.setYearBasis(dto.getYearBasis());
        entity.setAffectsOvertime(dto.isAffectsOvertime());
        entity.setProcessSeparately(dto.isProcessSeparately());
        entity.setSortOrder(dto.getSortOrder());
        entity.setRemarks(dto.getRemarks());
        return toDto(repository.save(entity));
    }

    public List<TimeTypePayrollRuleDto> initializeDefaults(UUID timeTypeId) {
        UUID companyId = TenantContext.requireCompanyId();
        TimeType timeType = requireTimeType(companyId, timeTypeId);
        if ("T".equalsIgnoreCase(timeType.getCode())) {
            initializeLateDefaults(companyId, timeTypeId);
            return findByTimeType(timeTypeId);
        }
        boolean unpaid = "U".equalsIgnoreCase(timeType.getCode());
        String defaultAction = unpaid ? "DEDUCT" : "PAY";
        for (PayrollComponent component : payrollComponentRepository.findByCompanyIdOrderByPriority(companyId)) {
            if (!"ACTIVE".equalsIgnoreCase(component.getStatus())) {
                continue;
            }
            TimeTypePayrollRule entity = repository
                    .findByCompanyIdAndTimeTypeIdAndPayrollComponentId(companyId, timeTypeId, component.getId())
                    .orElseGet(TimeTypePayrollRule::new);
            if (entity.getId() != null && !shouldResetInitializedRule(entity, unpaid)) {
                continue;
            }
            entity.setCompanyId(companyId);
            entity.setTimeTypeId(timeTypeId);
            entity.setPayrollComponentId(component.getId());
            entity.setAction(defaultAction);
            entity.setPercent(new java.math.BigDecimal("100.00"));
            entity.setBasis("HOURS");
            entity.setThresholdDays(0);
            entity.setThresholdScope("NONE");
            entity.setYearBasis("CALENDAR");
            entity.setAffectsOvertime(false);
            entity.setProcessSeparately(false);
            entity.setSortOrder(component.getPriority());
            entity.setRemarks("Initialized rule; review and adjust this component if the time type should behave differently.");
            repository.save(entity);
        }
        return findByTimeType(timeTypeId);
    }

    private void initializeLateDefaults(UUID companyId, UUID timeTypeId) {
        for (PayrollComponent component : payrollComponentRepository.findByCompanyIdOrderByPriority(companyId)) {
            if (!"ACTIVE".equalsIgnoreCase(component.getStatus())) {
                continue;
            }
            TimeTypePayrollRule entity = repository
                    .findByCompanyIdAndTimeTypeIdAndPayrollComponentId(companyId, timeTypeId, component.getId())
                    .orElseGet(TimeTypePayrollRule::new);
            entity.setCompanyId(companyId);
            entity.setTimeTypeId(timeTypeId);
            entity.setPayrollComponentId(component.getId());
            boolean basicSalary = isBasicSalaryComponent(component);
            entity.setAction(basicSalary ? "DEDUCT" : "PAY");
            entity.setPercent(new java.math.BigDecimal("100.00"));
            entity.setBasis(basicSalary ? "SHORTAGE" : "PLANNED_SHIFT");
            entity.setThresholdDays(0);
            entity.setThresholdScope("NONE");
            entity.setYearBasis("CALENDAR");
            entity.setAffectsOvertime(false);
            entity.setProcessSeparately(false);
            entity.setSortOrder(component.getPriority());
            entity.setRemarks(basicSalary
                    ? "Initialized late rule: pay planned hours and deduct shortage hours from basic salary."
                    : "Initialized late rule: pay planned shift hours; late deduction is handled by basic salary.");
            repository.save(entity);
        }
    }

    private static boolean shouldResetInitializedRule(TimeTypePayrollRule entity, boolean unpaid) {
        if (unpaid) {
            return true;
        }
        return isInitializedRule(entity);
    }

    private static boolean isInitializedRule(TimeTypePayrollRule entity) {
        String action = entity.getAction();
        String remarks = entity.getRemarks();
        return "DEFAULT".equalsIgnoreCase(action)
                || (remarks != null && remarks.trim().toUpperCase().startsWith("INITIALIZED"));
    }

    private static boolean isBasicSalaryComponent(PayrollComponent component) {
        String category = component.getCategory() == null ? "" : component.getCategory();
        String name = component.getName() == null ? "" : component.getName();
        return "SALARY".equalsIgnoreCase(category) || name.toUpperCase().contains("BASE");
    }

    public void delete(UUID timeTypeId, UUID componentId) {
        UUID companyId = TenantContext.requireCompanyId();
        requireTimeType(companyId, timeTypeId);
        repository.deleteByCompanyIdAndTimeTypeIdAndPayrollComponentId(companyId, timeTypeId, componentId);
    }

    private TimeType requireTimeType(UUID companyId, UUID timeTypeId) {
        TimeType timeType = timeTypeRepository.findById(timeTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Time type not found: " + timeTypeId));
        if (!companyId.equals(timeType.getCompanyId())) {
            throw new ResourceNotFoundException("Time type not found: " + timeTypeId);
        }
        return timeType;
    }

    private PayrollComponent requireComponent(UUID companyId, UUID componentId) {
        PayrollComponent component = payrollComponentRepository.findById(componentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll component not found: " + componentId));
        if (!companyId.equals(component.getCompanyId())) {
            throw new ResourceNotFoundException("Payroll component not found: " + componentId);
        }
        return component;
    }

    private static String normalizeThresholdScope(String thresholdScope, int thresholdDays) {
        if (thresholdDays <= 0) {
            return "NONE";
        }
        if (thresholdScope != null && "ANNUAL".equalsIgnoreCase(thresholdScope)) {
            return "ANNUAL";
        }
        return "CONSECUTIVE";
    }

    private TimeTypePayrollRuleDto toDto(TimeTypePayrollRule entity) {
        TimeTypePayrollRuleDto dto = new TimeTypePayrollRuleDto();
        dto.setId(entity.getId());
        dto.setTimeTypeId(entity.getTimeTypeId());
        dto.setPayrollComponentId(entity.getPayrollComponentId());
        dto.setAction(entity.getAction());
        dto.setPercent(entity.getPercent());
        dto.setBasis(entity.getBasis());
        dto.setThresholdDays(entity.getThresholdDays());
        dto.setThresholdScope(entity.getThresholdScope());
        dto.setYearBasis(entity.getYearBasis());
        dto.setAffectsOvertime(entity.isAffectsOvertime());
        dto.setProcessSeparately(entity.isProcessSeparately());
        dto.setSortOrder(entity.getSortOrder());
        dto.setRemarks(entity.getRemarks());
        payrollComponentRepository.findById(entity.getPayrollComponentId()).ifPresent(component -> {
            dto.setPayrollComponentCode(component.getCode());
            dto.setPayrollComponentName(component.getName());
        });
        return dto;
    }
}
