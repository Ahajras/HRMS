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
import java.time.LocalDate;
import java.util.ArrayList;
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

    public List<PayrollComponentDto> initializeDefaults() {
        UUID companyId = TenantContext.requireCompanyId();
        LocalDate effectiveFrom = LocalDate.of(2024, 1, 1);
        List<DefaultComponent> defaults = List.of(
                d("00", "Basic Salary", "SALARY", 10, true, true, true, true, "MONTHLY"),
                d("03", "Special Location", "ALLOWANCE", 20, true, false, false, false, "MONTHLY"),
                d("05", "Special Location Asian", "ALLOWANCE", 21, true, false, false, false, "MONTHLY"),
                d("08", "Job Allowance", "ALLOWANCE", 30, true, false, true, true, "MONTHLY"),
                d("10", "Captain Allowance", "ALLOWANCE", 31, true, false, false, false, "MONTHLY"),
                d("15", "Area Allowance", "ALLOWANCE", 32, true, false, false, false, "MONTHLY"),
                d("18", "Subsistence Allowance", "ALLOWANCE", 33, true, false, false, false, "MONTHLY"),
                d("20", "Qualification Allowance", "ALLOWANCE", 34, true, false, false, false, "MONTHLY"),
                d("21", "Home Subsidy", "ALLOWANCE", 35, true, false, false, false, "MONTHLY"),
                d("22", "Messing Subsidy", "ALLOWANCE", 36, true, false, false, false, "MONTHLY"),
                d("23", "Furniture", "ALLOWANCE", 37, true, false, false, false, "MONTHLY"),
                d("24", "Accom. & Furn.", "ALLOWANCE", 38, true, false, false, false, "MONTHLY"),
                d("25", "Accommodation", "ALLOWANCE", 39, true, false, true, true, "MONTHLY"),
                d("27", "Living Allowance", "ALLOWANCE", 40, true, false, true, true, "MONTHLY"),
                d("28", "Cost of Living Allowance", "ALLOWANCE", 41, true, false, false, false, "MONTHLY"),
                d("30", "Special Allowance", "ALLOWANCE", 42, true, false, false, false, "MONTHLY"),
                d("32", "Temporary Allowance", "ALLOWANCE", 43, true, false, false, false, "MONTHLY"),
                d("36", "Hardship & Separation Allowance", "ALLOWANCE", 44, true, false, false, false, "MONTHLY"),
                d("37", "Special Location Allowance", "ALLOWANCE", 45, true, false, false, false, "MONTHLY"),
                d("39", "Technical Allowance", "ALLOWANCE", 46, true, false, false, false, "MONTHLY"),
                d("41", "Transportation", "ALLOWANCE", 47, true, false, true, true, "MONTHLY"),
                d("42", "Car I Allowance", "ALLOWANCE", 48, true, false, false, false, "MONTHLY"),
                d("43", "Car II Allowance", "ALLOWANCE", 49, true, false, false, false, "MONTHLY"),
                d("44", "Car III Allowance", "ALLOWANCE", 50, true, false, false, false, "MONTHLY"),
                d("51", "Overtime Allowance", "OVERTIME", 60, true, false, false, false, "MONTHLY"),
                d("54", "Make-up Subsidy", "ALLOWANCE", 61, true, false, false, false, "MONTHLY"),
                d("55", "Offshore Industrial Allowance", "ALLOWANCE", 62, true, false, false, false, "MONTHLY"),
                d("56", "Temporary Cost-Index Subsidy", "ALLOWANCE", 63, true, false, false, false, "MONTHLY"),
                d("62", "Family Living Subsidy", "ALLOWANCE", 64, true, false, false, false, "MONTHLY"),
                d("63", "Direct Allowance", "ALLOWANCE", 65, true, false, false, false, "MONTHLY"),
                d("64", "In-direct Allowance", "ALLOWANCE", 66, true, false, false, false, "MONTHLY"),
                d("65", "Job Allowance 2", "ALLOWANCE", 67, true, false, false, false, "MONTHLY"),
                d("66", "Marine Allowance", "ALLOWANCE", 68, true, false, false, false, "MONTHLY"),
                d("74", "Incentive Payment", "BONUS", 80, true, false, false, false, "MONTHLY"),
                d("78", "Safety Allowance", "ALLOWANCE", 69, true, false, false, false, "MONTHLY"),
                d("81", "Supplement Allowance", "ALLOWANCE", 70, true, false, false, false, "MONTHLY"),
                d("91", "Special Family Subsidy", "ALLOWANCE", 71, true, false, false, false, "MONTHLY"),
                d("95", "House Boy/Cook Allowance", "ALLOWANCE", 72, true, false, false, false, "MONTHLY"),
                d("96", "Transportation Expenses", "ALLOWANCE", 73, true, false, false, false, "MONTHLY"),
                d("99", "Policy Allowance", "ALLOWANCE", 74, true, false, false, false, "MONTHLY"),
                d("OA", "Other Allowance", "ALLOWANCE", 75, true, false, false, false, "MONTHLY")
        );
        List<PayrollComponentDto> out = new ArrayList<>();
        for (DefaultComponent def : defaults) {
            PayrollComponent entity = repository.findByCompanyIdAndCode(companyId, def.code()).orElseGet(() -> {
                PayrollComponent pc = new PayrollComponent();
                pc.setCompanyId(companyId);
                pc.setCode(def.code());
                return pc;
            });
            entity.setName(def.name());
            entity.setCategory(def.category());
            entity.setComponentType("EARNING");
            entity.setPaymentFrequency(def.paymentFrequency());
            entity.setCalculationMethod("FIXED");
            entity.setRoundingMethod("HALF_UP");
            entity.setRoundingScale(2);
            entity.setPriority(def.priority());
            entity.setTaxable(false);
            entity.setInsurable(def.insurable());
            entity.setWpsIncluded(def.wpsIncluded());
            entity.setEosIncluded(def.eosIncluded());
            entity.setProvisionIncluded(false);
            entity.setLeaveIncluded(def.leaveIncluded());
            entity.setVisibleOnPayslip(true);
            entity.setVisibleOnReports(true);
            entity.setCostAllocationRequired(false);
            entity.setApprovalRequired(false);
            entity.setEffectiveFrom(entity.getEffectiveFrom() != null ? entity.getEffectiveFrom() : effectiveFrom);
            entity.setStatus("ACTIVE");
            entity.setRemarks("Initialized from legacy allowance library.");
            out.add(toDto(repository.save(entity)));
        }
        return out;
    }

    private static DefaultComponent d(String code, String name, String category, int priority,
                                      boolean wpsIncluded, boolean eosIncluded,
                                      boolean leaveIncluded, boolean insurable,
                                      String paymentFrequency) {
        return new DefaultComponent(code, name, category, priority, wpsIncluded, eosIncluded, leaveIncluded, insurable, paymentFrequency);
    }

    private record DefaultComponent(String code, String name, String category, int priority,
                                    boolean wpsIncluded, boolean eosIncluded,
                                    boolean leaveIncluded, boolean insurable,
                                    String paymentFrequency) {}

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
