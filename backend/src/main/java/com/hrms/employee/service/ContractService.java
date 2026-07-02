package com.hrms.employee.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.domain.Contract;
import com.hrms.employee.dto.ContractDto;
import com.hrms.employee.repository.ContractPayItemRepository;
import com.hrms.employee.repository.ContractRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for employment contracts (FTDD Vol.1 Ch.2).
 */
@Service
@Transactional
public class ContractService {

    private final ContractRepository repository;
    private final ContractPayItemRepository payItemRepository;

    public ContractService(ContractRepository repository, ContractPayItemRepository payItemRepository) {
        this.repository = repository;
        this.payItemRepository = payItemRepository;
    }

    @Transactional(readOnly = true)
    public List<ContractDto> findByEmployee(UUID employeeId) {
        return repository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ContractDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public ContractDto create(ContractDto dto) {
        Contract entity = new Contract();
        entity.setEmployeeId(dto.getEmployeeId());
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public ContractDto update(UUID id, ContractDto dto) {
        Contract entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        Contract contract = getEntity(id);
        payItemRepository.deleteByContractId(contract.getId());
        repository.delete(contract);
    }

    private Contract getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
    }

    private void apply(ContractDto dto, Contract entity) {
        entity.setContractNumber(dto.getContractNumber());
        entity.setContractType(dto.getContractType());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        entity.setBaseCurrencyCode(dto.getBaseCurrencyCode());
        entity.setWorkingHoursPerWeek(dto.getWorkingHoursPerWeek());
        entity.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
        entity.setOvertimeCategory(dto.getOvertimeCategory());
        entity.setOvertimeCategoryDesc(dto.getOvertimeCategoryDesc());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private ContractDto toDto(Contract entity) {
        ContractDto dto = new ContractDto();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setContractNumber(entity.getContractNumber());
        dto.setContractType(entity.getContractType());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setBaseCurrencyCode(entity.getBaseCurrencyCode());
        dto.setWorkingHoursPerWeek(entity.getWorkingHoursPerWeek());
        dto.setWorkingDaysPerWeek(entity.getWorkingDaysPerWeek());
        dto.setOvertimeCategory(entity.getOvertimeCategory());
        dto.setOvertimeCategoryDesc(entity.getOvertimeCategoryDesc());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
