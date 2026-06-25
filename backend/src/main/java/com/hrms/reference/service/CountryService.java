package com.hrms.reference.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.reference.domain.Country;
import com.hrms.reference.dto.CountryDto;
import com.hrms.reference.repository.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for ISO 3166 countries (reference master data).
 */
@Service
@Transactional
public class CountryService {

    private final CountryRepository repository;

    public CountryService(CountryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CountryDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CountryDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public CountryDto create(CountryDto dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new BusinessRuleException("country.code.duplicate",
                    "Country code already exists: " + dto.getCode());
        }
        Country entity = new Country();
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public CountryDto update(UUID id, CountryDto dto) {
        Country entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Country getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Country not found: " + id));
    }

    private void apply(CountryDto dto, Country entity) {
        entity.setCode(dto.getCode());
        entity.setName(dto.getName());
        entity.setDefaultCurrencyCode(dto.getDefaultCurrencyCode());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private CountryDto toDto(Country entity) {
        CountryDto dto = new CountryDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setName(entity.getName());
        dto.setDefaultCurrencyCode(entity.getDefaultCurrencyCode());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
