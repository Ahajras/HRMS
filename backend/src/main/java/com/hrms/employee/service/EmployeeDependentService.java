package com.hrms.employee.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.domain.EmployeeDependent;
import com.hrms.employee.dto.EmployeeDependentDto;
import com.hrms.employee.repository.EmployeeDependentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for employee dependents (family members / المعالين).
 */
@Service
@Transactional
public class EmployeeDependentService {

    private final EmployeeDependentRepository repository;

    public EmployeeDependentService(EmployeeDependentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeDependentDto> findByEmployee(UUID employeeId) {
        return repository.findByEmployeeIdOrderByFullName(employeeId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeDependentDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public EmployeeDependentDto create(EmployeeDependentDto dto) {
        EmployeeDependent entity = new EmployeeDependent();
        entity.setEmployeeId(dto.getEmployeeId());
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public EmployeeDependentDto update(UUID id, EmployeeDependentDto dto) {
        EmployeeDependent entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private EmployeeDependent getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dependent not found: " + id));
    }

    private void apply(EmployeeDependentDto dto, EmployeeDependent entity) {
        entity.setFullName(dto.getFullName());
        entity.setRelationship(dto.getRelationship());
        entity.setGender(dto.getGender());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setNationalityCountryCode(dto.getNationalityCountryCode());
        entity.setBeneficiary(dto.isBeneficiary());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private EmployeeDependentDto toDto(EmployeeDependent entity) {
        EmployeeDependentDto dto = new EmployeeDependentDto();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setFullName(entity.getFullName());
        dto.setRelationship(entity.getRelationship());
        dto.setGender(entity.getGender());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setNationalityCountryCode(entity.getNationalityCountryCode());
        dto.setBeneficiary(entity.isBeneficiary());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
