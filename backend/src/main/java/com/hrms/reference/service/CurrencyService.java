package com.hrms.reference.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.reference.domain.Currency;
import com.hrms.reference.dto.CurrencyDto;
import com.hrms.reference.repository.CurrencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for ISO 4217 currencies (reference master data).
 */
@Service
@Transactional
public class CurrencyService {

    private final CurrencyRepository repository;

    public CurrencyService(CurrencyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CurrencyDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CurrencyDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public CurrencyDto create(CurrencyDto dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new BusinessRuleException("currency.code.duplicate",
                    "Currency code already exists: " + dto.getCode());
        }
        Currency entity = new Currency();
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public CurrencyDto update(UUID id, CurrencyDto dto) {
        Currency entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Currency getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Currency not found: " + id));
    }

    private void apply(CurrencyDto dto, Currency entity) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setSymbol(dto.getSymbol());
        entity.setMinorUnits(dto.getMinorUnits());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private CurrencyDto toDto(Currency entity) {
        CurrencyDto dto = new CurrencyDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setSymbol(entity.getSymbol());
        dto.setMinorUnits(entity.getMinorUnits());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
