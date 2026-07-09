package com.hrms.company.service;

import com.hrms.common.tenant.TenantContext;
import com.hrms.company.domain.CompanyProfile;
import com.hrms.company.dto.CompanyProfileDto;
import com.hrms.company.repository.CompanyProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@Transactional
public class CompanyProfileService {

    private final CompanyProfileRepository repository;

    public CompanyProfileService(CompanyProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CompanyProfileDto get() {
        UUID companyId = TenantContext.requireCompanyId();
        return getByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public CompanyProfileDto getPublic() {
        return TenantContext.getCompanyId()
                .map(this::getByCompanyId)
                .orElseGet(() -> {
                    CompanyProfileDto dto = new CompanyProfileDto();
                    dto.setCompanyName("HRMS");
                    return dto;
                });
    }

    private CompanyProfileDto getByCompanyId(UUID companyId) {
        return repository.findByCompanyId(companyId).map(this::toDto).orElseGet(() -> {
            CompanyProfileDto dto = new CompanyProfileDto();
            dto.setCompanyId(companyId);
            dto.setCompanyName("Company");
            return dto;
        });
    }

    public CompanyProfileDto save(CompanyProfileDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        CompanyProfile entity = repository.findByCompanyId(companyId).orElseGet(CompanyProfile::new);
        entity.setCompanyId(companyId);
        entity.setCompanyName(StringUtils.hasText(dto.getCompanyName()) ? dto.getCompanyName() : "Company");
        entity.setLegalName(dto.getLegalName());
        entity.setTaxNumber(dto.getTaxNumber());
        entity.setRegistrationNo(dto.getRegistrationNo());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setWebsite(dto.getWebsite());
        entity.setAddressLine(dto.getAddressLine());
        entity.setLogoUrl(dto.getLogoUrl());
        return toDto(repository.save(entity));
    }

    private CompanyProfileDto toDto(CompanyProfile entity) {
        CompanyProfileDto dto = new CompanyProfileDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setCompanyName(entity.getCompanyName());
        dto.setLegalName(entity.getLegalName());
        dto.setTaxNumber(entity.getTaxNumber());
        dto.setRegistrationNo(entity.getRegistrationNo());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setWebsite(entity.getWebsite());
        dto.setAddressLine(entity.getAddressLine());
        dto.setLogoUrl(entity.getLogoUrl());
        return dto;
    }
}
