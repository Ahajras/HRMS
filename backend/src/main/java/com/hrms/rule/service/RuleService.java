package com.hrms.rule.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.rule.domain.CompanyRulePackage;
import com.hrms.rule.domain.Rule;
import com.hrms.rule.domain.RulePackage;
import com.hrms.rule.dto.RuleDto;
import com.hrms.rule.dto.RulePackageDto;
import com.hrms.rule.repository.CompanyRulePackageRepository;
import com.hrms.rule.repository.RulePackageRepository;
import com.hrms.rule.repository.RuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Country-law rule store: lists packages, lets a company pick its active country
 * law, edits values forward in time (supersede), and resolves a value as of a
 * date. The resolver is the seam every later engine reads from instead of
 * hardcoding business logic (FTDD Vol.1 Ch.15 / Vol.2 Ch.23).
 */
@Service
@Transactional
public class RuleService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";
    private static final String DEFAULT_PACKAGE = "QATAR";

    private final RulePackageRepository packageRepo;
    private final RuleRepository ruleRepo;
    private final CompanyRulePackageRepository companyPackageRepo;

    public RuleService(RulePackageRepository packageRepo, RuleRepository ruleRepo,
                       CompanyRulePackageRepository companyPackageRepo) {
        this.packageRepo = packageRepo;
        this.ruleRepo = ruleRepo;
        this.companyPackageRepo = companyPackageRepo;
    }

    // --- packages ----------------------------------------------------

    @Transactional(readOnly = true)
    public List<RulePackageDto> listPackages() {
        return packageRepo.findByCompanyIdIsNullOrderByName().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public String getActivePackageCode() {
        UUID companyId = TenantContext.requireCompanyId();
        return companyPackageRepo.findById(companyId)
                .map(CompanyRulePackage::getPackageCode)
                .orElse(DEFAULT_PACKAGE);
    }

    public String setActivePackage(String packageCode) {
        UUID companyId = TenantContext.requireCompanyId();
        packageRepo.findByCompanyIdIsNullAndCode(packageCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rule package not found: " + packageCode));
        CompanyRulePackage link = companyPackageRepo.findById(companyId).orElseGet(() -> {
            CompanyRulePackage c = new CompanyRulePackage();
            c.setCompanyId(companyId);
            return c;
        });
        link.setPackageCode(packageCode);
        link.setUpdatedAt(Instant.now());
        companyPackageRepo.save(link);
        return packageCode;
    }

    // --- rules -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<RuleDto> listRules(String packageCode) {
        RulePackage pkg = requirePackage(packageCode);
        return ruleRepo.findByPackageIdOrderByCategoryAscNameAscEffectiveFromDesc(pkg.getId())
                .stream().map(this::toDto).toList();
    }

    /** Create or change a rule value; supersedes the current active value forward. */
    public RuleDto save(RuleDto dto) {
        List<Rule> current = ruleRepo.findByPackageIdAndCodeAndStatus(dto.getPackageId(), dto.getCode(), ACTIVE);
        for (Rule existing : current) {
            if (!dto.getEffectiveFrom().isAfter(existing.getEffectiveFrom())) {
                throw new BusinessRuleException("rule.effective.order",
                        "New effective date must be after the current value's effective date ("
                                + existing.getEffectiveFrom() + ").");
            }
            existing.setEffectiveTo(dto.getEffectiveFrom().minusDays(1));
            existing.setStatus(INACTIVE);
            ruleRepo.save(existing);
        }
        Rule entity = new Rule();
        entity.setPackageId(dto.getPackageId());
        apply(dto, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(ACTIVE);
        }
        return toDto(ruleRepo.save(entity));
    }

    /** In-place correction (no supersession). */
    public RuleDto update(UUID id, RuleDto dto) {
        Rule entity = getRule(id);
        apply(dto, entity);
        return toDto(ruleRepo.save(entity));
    }

    public void delete(UUID id) {
        ruleRepo.delete(getRule(id));
    }

    // --- resolver (used by later engines) ----------------------------

    /** Resolve a numeric rule value for the company's active package as of a date. */
    @Transactional(readOnly = true)
    public BigDecimal number(String ruleCode, LocalDate asOf) {
        return resolve(ruleCode, asOf).getValueNumber();
    }

    /** Resolve a text rule value for the company's active package as of a date. */
    @Transactional(readOnly = true)
    public String text(String ruleCode, LocalDate asOf) {
        return resolve(ruleCode, asOf).getValueText();
    }

    private Rule resolve(String ruleCode, LocalDate asOf) {
        RulePackage pkg = requirePackage(getActivePackageCode());
        return ruleRepo.findByPackageIdAndCodeAndStatus(pkg.getId(), ruleCode, ACTIVE).stream()
                .filter(r -> !asOf.isBefore(r.getEffectiveFrom())
                        && (r.getEffectiveTo() == null || !asOf.isAfter(r.getEffectiveTo())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active rule '" + ruleCode + "' as of " + asOf + " in package " + pkg.getCode()));
    }

    // --- helpers -----------------------------------------------------

    private RulePackage requirePackage(String code) {
        return packageRepo.findByCompanyIdIsNullAndCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Rule package not found: " + code));
    }

    private Rule getRule(UUID id) {
        return ruleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
    }

    private void apply(RuleDto dto, Rule entity) {
        entity.setCompanyId(dto.getCompanyId());
        entity.setCode(dto.getCode());
        entity.setCategory(dto.getCategory());
        entity.setName(dto.getName());
        entity.setValueType(dto.getValueType());
        entity.setValueNumber(dto.getValueNumber());
        entity.setValueText(dto.getValueText());
        entity.setUnit(dto.getUnit());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private RuleDto toDto(Rule e) {
        RuleDto dto = new RuleDto();
        dto.setId(e.getId());
        dto.setPackageId(e.getPackageId());
        dto.setCompanyId(e.getCompanyId());
        dto.setCode(e.getCode());
        dto.setCategory(e.getCategory());
        dto.setName(e.getName());
        dto.setValueType(e.getValueType());
        dto.setValueNumber(e.getValueNumber());
        dto.setValueText(e.getValueText());
        dto.setUnit(e.getUnit());
        dto.setEffectiveFrom(e.getEffectiveFrom());
        dto.setEffectiveTo(e.getEffectiveTo());
        dto.setStatus(e.getStatus());
        dto.setDescription(e.getDescription());
        return dto;
    }

    private RulePackageDto toDto(RulePackage e) {
        RulePackageDto dto = new RulePackageDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setCountryCode(e.getCountryCode());
        dto.setStatus(e.getStatus());
        return dto;
    }
}
