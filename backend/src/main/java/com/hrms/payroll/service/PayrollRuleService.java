package com.hrms.payroll.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.payroll.domain.PayrollRule;
import com.hrms.payroll.dto.PayrollRuleDto;
import com.hrms.payroll.repository.PayrollRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PayrollRuleService {
    private final PayrollRuleRepository repository;

    public PayrollRuleService(PayrollRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PayrollRuleDto> list() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderByPayGroup(companyId).stream().map(this::toDto).toList();
    }

    public PayrollRuleDto create(PayrollRuleDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        String group = dto.getPayGroup() != null ? dto.getPayGroup() : "MONTHLY";
        // If a rule already exists for this (company, project, pay group), reuse it
        // instead of creating a duplicate (avoids the unique-index violation).
        PayrollRule rule = repository
                .findByCompanyIdAndProjectIdAndPayGroupAndStatus(companyId, dto.getProjectId(), group, "ACTIVE")
                .orElseGet(PayrollRule::new);
        rule.setCompanyId(companyId);
        rule.setPayGroup(group);
        rule.setProjectId(dto.getProjectId());
        rule.setPayItemBasis(dto.getPayItemBasis() != null ? dto.getPayItemBasis()
                : ("DAILY".equalsIgnoreCase(group) ? "DAILY_RATE" : "FIXED_AMOUNT"));
        rule.setOtMultiplier(dto.getOtMultiplier() != null ? dto.getOtMultiplier() : new java.math.BigDecimal("1.2500"));
        rule.setRestDayOtMultiplier(dto.getRestDayOtMultiplier() != null ? dto.getRestDayOtMultiplier() : new java.math.BigDecimal("1.5000"));
        rule.setStandardHoursPerDay(dto.getStandardHoursPerDay() != null ? dto.getStandardHoursPerDay() : new java.math.BigDecimal("8.00"));
        rule.setMonthDivisor(dto.getMonthDivisor() != null ? dto.getMonthDivisor() : new java.math.BigDecimal("30.00"));
        rule.setDivisorMode(dto.getDivisorMode() != null ? dto.getDivisorMode() : "FIXED");
        rule.setWeeklyRestPaid(dto.isWeeklyRestPaid());
        rule.setStatus("ACTIVE");
        return toDto(repository.save(rule));
    }

    public PayrollRuleDto update(UUID id, PayrollRuleDto dto) {
        PayrollRule rule = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll rule not found: " + id));
        if (!TenantContext.requireCompanyId().equals(rule.getCompanyId())) {
            throw new ResourceNotFoundException("Payroll rule not found: " + id);
        }
        rule.setPayItemBasis(dto.getPayItemBasis());
        rule.setOtMultiplier(dto.getOtMultiplier());
        rule.setRestDayOtMultiplier(dto.getRestDayOtMultiplier());
        rule.setStandardHoursPerDay(dto.getStandardHoursPerDay());
        rule.setMonthDivisor(dto.getMonthDivisor());
        if (dto.getDivisorMode() != null) rule.setDivisorMode(dto.getDivisorMode());
        rule.setProjectId(dto.getProjectId());
        rule.setWeeklyRestPaid(dto.isWeeklyRestPaid());
        return toDto(repository.save(rule));
    }

    private PayrollRuleDto toDto(PayrollRule rule) {
        PayrollRuleDto dto = new PayrollRuleDto();
        dto.setId(rule.getId());
        dto.setPayGroup(rule.getPayGroup());
        dto.setPayItemBasis(rule.getPayItemBasis());
        dto.setOtMultiplier(rule.getOtMultiplier());
        dto.setRestDayOtMultiplier(rule.getRestDayOtMultiplier());
        dto.setStandardHoursPerDay(rule.getStandardHoursPerDay());
        dto.setMonthDivisor(rule.getMonthDivisor());
        dto.setDivisorMode(rule.getDivisorMode());
        dto.setProjectId(rule.getProjectId());
        dto.setWeeklyRestPaid(rule.isWeeklyRestPaid());
        dto.setStatus(rule.getStatus());
        return dto;
    }
}
