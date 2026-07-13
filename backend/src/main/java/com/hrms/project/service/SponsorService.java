package com.hrms.project.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.project.domain.Sponsor;
import com.hrms.project.dto.SponsorDto;
import com.hrms.project.repository.SponsorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SponsorService {

    private final SponsorRepository repository;

    public SponsorService(SponsorRepository repository) {
        this.repository = repository;
    }

    public List<SponsorDto> list() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderByCode(companyId).stream().map(this::toDto).toList();
    }

    @Transactional
    public SponsorDto save(SponsorDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        Sponsor entity = dto.getId() != null
                ? repository.findById(dto.getId()).orElseThrow(() -> new ResourceNotFoundException("Sponsor not found: " + dto.getId()))
                : new Sponsor();
        entity.setCompanyId(companyId);
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setEstablishmentEid(dto.getEstablishmentEid());
        entity.setPayerQid(dto.getPayerQid());
        entity.setPayerBankCode(dto.getPayerBankCode());
        entity.setPayerIban(dto.getPayerIban());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        return toDto(repository.save(entity));
    }

    private SponsorDto toDto(Sponsor s) {
        SponsorDto dto = new SponsorDto();
        dto.setId(s.getId());
        dto.setCode(s.getCode());
        dto.setName(s.getName());
        dto.setEstablishmentEid(s.getEstablishmentEid());
        dto.setPayerQid(s.getPayerQid());
        dto.setPayerBankCode(s.getPayerBankCode());
        dto.setPayerIban(s.getPayerIban());
        dto.setStatus(s.getStatus());
        return dto;
    }
}
