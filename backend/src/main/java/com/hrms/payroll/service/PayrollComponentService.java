package com.hrms.payroll.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.payroll.domain.PayrollComponent;
import com.hrms.payroll.dto.PayrollComponentDto;
import com.hrms.payroll.repository.PayrollComponentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for salary component master data (FTDD Vol.1 Ch.6). Company-scoped via
 * {@link TenantContext}. Calculation semantics are deferred to the Rule Engine
 * (Phase 3); this service only manages component definitions.
 */
@Service
@Transactional
public class PayrollComponentService {

    private final PayrollComponentRepository repository;

    public PayrollComponentService(PayrollComponentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<PayrollComponentDto> findAll(String category) {
        UUID companyId = TenantContext.requireCompanyId();
        List<PayrollComponent> components = (category == null || category.isBlank())
                ? repository.findByCompanyIdOrderByPriority(companyId)
                : repository.findByCompanyIdAndCategoryOrderByPriority(companyId, category);
        return components.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PayrollComponentDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public PayrollComponentDto create(PayrollComponentDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        if (repository.existsByCompanyIdAndCode(companyId, dto.getCode())) {
            throw new BusinessRuleException("component.code.duplicate",
                    "Payroll component code already exists: " + dto.getCode());
        }
        PayrollComponent entity = new PayrollComponent();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public PayrollComponentDto update(UUID id, PayrollComponentDto dto) {
        PayrollComponent entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private PayrollComponent getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll component not found: " + id));
    }

    private void apply(PayrollComponentDto dto, PayrollComponent entity) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setCategory(dto.getCategory());
        entity.setComponentType(dto.getComponentType());
        entity.setPaymentFrequency(dto.getPaymentFrequency());
        entity.setCalculationMethod(dto.getCalculationMethod());
        entity.setRoundingMethod(dto.getRoundingMethod());
        entity.setRoundingScale(dto.getRoundingScale());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setPriority(dto.getPriority());
        entity.setTaxable(dto.isTaxable());
        entity.setInsurable(dto.isInsurable());
        entity.setWpsIncluded(dto.isWpsIncluded());
        entity.setEosIncluded(dto.isEosIncluded());
        entity.setProvisionIncluded(dto.isProvisionIncluded());
        entity.setLeaveIncluded(dto.isLeaveIncluded());
        entity.setVisibleOnPayslip(dto.isVisibleOnPayslip());
        entity.setVisibleOnReports(dto.isVisibleOnReports());
        entity.setCostAllocationRequired(dto.isCostAllocationRequired());
        entity.setApprovalRequired(dto.isApprovalRequired());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setRemarks(dto.getRemarks());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private PayrollComponentDto toDto(PayrollComponent entity) {
        PayrollComponentDto dto = new PayrollComponentDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setCategory(entity.getCategory());
        dto.setComponentType(entity.getComponentType());
        dto.setPaymentFrequency(entity.getPaymentFrequency());
        dto.setCalculationMethod(entity.getCalculationMethod());
        dto.setRoundingMethod(entity.getRoundingMethod());
        dto.setRoundingScale(entity.getRoundingScale());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setPriority(entity.getPriority());
        dto.setTaxable(entity.isTaxable());
        dto.setInsurable(entity.isInsurable());
        dto.setWpsIncluded(entity.isWpsIncluded());
        dto.setEosIncluded(entity.isEosIncluded());
        dto.setProvisionIncluded(entity.isProvisionIncluded());
        dto.setLeaveIncluded(entity.isLeaveIncluded());
        dto.setVisibleOnPayslip(entity.isVisibleOnPayslip());
        dto.setVisibleOnReports(entity.isVisibleOnReports());
        dto.setCostAllocationRequired(entity.isCostAllocationRequired());
        dto.setApprovalRequired(entity.isApprovalRequired());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setStatus(entity.getStatus());
        dto.setRemarks(entity.getRemarks());
        return dto;
    }
}
