package com.hrms.reference.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.reference.domain.Bank;
import com.hrms.reference.dto.BankDto;
import com.hrms.reference.repository.BankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for bank reference master data.
 */
@Service
@Transactional
public class BankService {

    private final BankRepository repository;

    public BankService(BankRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<BankDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BankDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public BankDto create(BankDto dto) {
        Bank entity = new Bank();
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public BankDto update(UUID id, BankDto dto) {
        Bank entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Bank getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found: " + id));
    }

    private void apply(BankDto dto, Bank entity) {
        entity.setCompanyId(dto.getCompanyId());
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setSwiftCode(dto.getSwiftCode());
        entity.setCountryCode(dto.getCountryCode());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private BankDto toDto(Bank entity) {
        BankDto dto = new BankDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setSwiftCode(entity.getSwiftCode());
        dto.setCountryCode(entity.getCountryCode());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
