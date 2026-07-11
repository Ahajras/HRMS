package com.hrms.payroll.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.payroll.domain.ProvisionRule;
import com.hrms.payroll.dto.ProvisionRuleDto;
import com.hrms.payroll.repository.ProvisionRuleRepository;
import com.hrms.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProvisionRuleService {

    private final ProvisionRuleRepository repo;
    private final ProjectRepository projectRepo;

    public ProvisionRuleService(ProvisionRuleRepository repo, ProjectRepository projectRepo) {
        this.repo = repo;
        this.projectRepo = projectRepo;
    }

    @Transactional(readOnly = true)
    public List<ProvisionRuleDto> list() {
        UUID companyId = TenantContext.requireCompanyId();
        return repo.findByCompanyIdOrderByProvisionTypeAscPayGroupAscNameAsc(companyId).stream().map(this::toDto).toList();
    }

    public ProvisionRuleDto save(ProvisionRuleDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        if (dto.getProjectId() != null) {
            projectRepo.findById(dto.getProjectId()).filter(p -> companyId.equals(p.getCompanyId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Project", dto.getProjectId()));
        }
        ProvisionRule rule = dto.getId() == null ? new ProvisionRule()
                : repo.findById(dto.getId()).filter(r -> companyId.equals(r.getCompanyId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Provision rule", dto.getId()));
        rule.setCompanyId(companyId);
        apply(dto, rule);
        return toDto(repo.save(rule));
    }

    public List<ProvisionRuleDto> initializeDefaults() {
        UUID companyId = TenantContext.requireCompanyId();
        createIfMissing(companyId, "LEAVE", "Annual leave provision", "COMPONENT_FLAGS",
                null, null, null, "basis_amount / divisor * entitlement_days / 12",
                BigDecimal.valueOf(365), BigDecimal.ZERO, BigDecimal.valueOf(21), BigDecimal.valueOf(28),
                12, BigDecimal.ONE, 0, "ON_CYCLE_DATE",
                "Default template. Edit the basis, divisor, days, or formula before relying on it.");
        createIfMissing(companyId, "EOS", "End of service provision", "COMPONENT_FLAGS",
                null, null, null, "basis_amount / divisor * entitlement_days / 12",
                BigDecimal.valueOf(365), BigDecimal.ZERO, BigDecimal.valueOf(21), BigDecimal.valueOf(21),
                12, BigDecimal.ONE, 0, "ON_CYCLE_DATE",
                "Default template. Qatar minimum is commonly configured as 21 basic-wage days per service year.");
        createIfMissing(companyId, "TICKET", "Ticket provision", "FIXED_AMOUNT", "MONTHLY",
                null, null, "ticket_amount * ticket_quantity / ticket_cycle_months",
                BigDecimal.valueOf(365), BigDecimal.ZERO, BigDecimal.valueOf(21), BigDecimal.valueOf(28),
                12, BigDecimal.ONE, 0, "ON_CYCLE_DATE",
                "Default template. Ticket amount is read from the employee work/home airport fare table.");
        return list();
    }

    public void delete(UUID id) {
        UUID companyId = TenantContext.requireCompanyId();
        ProvisionRule rule = repo.findById(id).filter(r -> companyId.equals(r.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException("Provision rule", id));
        repo.delete(rule);
    }

    private void createIfMissing(UUID companyId, String type, String name, String basisMode, String payGroup, String categories, String componentCodes,
                                 String formula, BigDecimal divisor, BigDecimal fixedAmount, BigDecimal underFive,
                                 BigDecimal fiveOrMore, int ticketCycleMonths, BigDecimal ticketQuantity,
                                 int ticketExpiryMonths, String ticketEntitlementMode, String notes) {
        String group = blank(payGroup) ? "ALL" : payGroup.trim().toUpperCase();
        if (repo.findByCompanyIdAndProvisionTypeAndProjectIdIsNullAndPayGroup(companyId, type, group).isPresent()) {
            return;
        }
        ProvisionRule rule = new ProvisionRule();
        rule.setCompanyId(companyId);
        rule.setProvisionType(type);
        rule.setPayGroup(group);
        rule.setName(name);
        rule.setBasisMode(basisMode);
        rule.setBasisCategories(categories);
        rule.setBasisComponentCodes(componentCodes);
        rule.setFormulaExpression(formula);
        rule.setDivisor(divisor);
        rule.setFixedAmount(fixedAmount);
        rule.setEntitlementDaysUnderFive(underFive);
        rule.setEntitlementDaysFiveOrMore(fiveOrMore);
        rule.setTicketCycleMonths(ticketCycleMonths);
        rule.setTicketQuantity(ticketQuantity == null ? BigDecimal.ONE : ticketQuantity);
        rule.setTicketExpiryMonths(Math.max(0, ticketExpiryMonths));
        rule.setTicketEntitlementMode(blank(ticketEntitlementMode) ? "ON_CYCLE_DATE" : ticketEntitlementMode.trim().toUpperCase());
        rule.setEffectiveFrom(LocalDate.of(2000, 1, 1));
        rule.setStatus("ACTIVE");
        rule.setNotes(notes);
        repo.save(rule);
    }

    private void apply(ProvisionRuleDto dto, ProvisionRule rule) {
        String type = norm(dto.getProvisionType(), "provisionType");
        String formula = requiredRaw(dto.getFormulaExpression(), "formulaExpression");
        rule.setProjectId(dto.getProjectId());
        rule.setPayGroup(blank(dto.getPayGroup()) ? "ALL" : dto.getPayGroup().trim().toUpperCase());
        rule.setProvisionType(type);
        rule.setName(blank(dto.getName()) ? type + " provision" : dto.getName().trim());
        rule.setBasisMode(blank(dto.getBasisMode()) ? "COMPONENT_FLAGS" : dto.getBasisMode().trim().toUpperCase());
        rule.setBasisCategories(dto.getBasisCategories());
        rule.setBasisComponentCodes(dto.getBasisComponentCodes());
        rule.setFormulaExpression(formula);
        rule.setDivisor(nz(dto.getDivisor(), BigDecimal.valueOf(365)));
        rule.setFixedAmount(nz(dto.getFixedAmount(), BigDecimal.ZERO));
        rule.setEntitlementDaysUnderFive(nz(dto.getEntitlementDaysUnderFive(), BigDecimal.valueOf(21)));
        rule.setEntitlementDaysFiveOrMore(nz(dto.getEntitlementDaysFiveOrMore(), BigDecimal.valueOf(28)));
        rule.setTicketCycleMonths(dto.getTicketCycleMonths() <= 0 ? 12 : dto.getTicketCycleMonths());
        rule.setTicketQuantity(nz(dto.getTicketQuantity(), BigDecimal.ONE));
        rule.setTicketExpiryMonths(Math.max(0, dto.getTicketExpiryMonths()));
        rule.setTicketEntitlementMode(blank(dto.getTicketEntitlementMode()) ? "ON_CYCLE_DATE" : dto.getTicketEntitlementMode().trim().toUpperCase());
        rule.setEffectiveFrom(dto.getEffectiveFrom() == null ? LocalDate.now() : dto.getEffectiveFrom());
        rule.setEffectiveTo(dto.getEffectiveTo());
        rule.setStatus(blank(dto.getStatus()) ? "ACTIVE" : dto.getStatus().trim().toUpperCase());
        rule.setNotes(dto.getNotes());
    }

    private ProvisionRuleDto toDto(ProvisionRule rule) {
        ProvisionRuleDto dto = new ProvisionRuleDto();
        dto.setId(rule.getId());
        dto.setProjectId(rule.getProjectId());
        dto.setPayGroup(rule.getPayGroup());
        dto.setProvisionType(rule.getProvisionType());
        dto.setName(rule.getName());
        dto.setBasisMode(rule.getBasisMode());
        dto.setBasisCategories(rule.getBasisCategories());
        dto.setBasisComponentCodes(rule.getBasisComponentCodes());
        dto.setFormulaExpression(rule.getFormulaExpression());
        dto.setDivisor(rule.getDivisor());
        dto.setFixedAmount(rule.getFixedAmount());
        dto.setEntitlementDaysUnderFive(rule.getEntitlementDaysUnderFive());
        dto.setEntitlementDaysFiveOrMore(rule.getEntitlementDaysFiveOrMore());
        dto.setTicketCycleMonths(rule.getTicketCycleMonths());
        dto.setTicketQuantity(rule.getTicketQuantity());
        dto.setTicketExpiryMonths(rule.getTicketExpiryMonths());
        dto.setTicketEntitlementMode(rule.getTicketEntitlementMode());
        dto.setEffectiveFrom(rule.getEffectiveFrom());
        dto.setEffectiveTo(rule.getEffectiveTo());
        dto.setStatus(rule.getStatus());
        dto.setNotes(rule.getNotes());
        return dto;
    }

    private String norm(String value, String field) {
        if (blank(value)) throw new BusinessRuleException("provision.rule.required", field + " is required.");
        return value.trim().toUpperCase();
    }

    private String requiredRaw(String value, String field) {
        if (blank(value)) throw new BusinessRuleException("provision.rule.required", field + " is required.");
        return value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private BigDecimal nz(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }
}
