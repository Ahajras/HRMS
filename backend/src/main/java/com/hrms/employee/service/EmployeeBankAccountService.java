package com.hrms.employee.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.domain.EmployeeBankAccount;
import com.hrms.employee.dto.EmployeeBankAccountDto;
import com.hrms.employee.repository.EmployeeBankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for employee bank accounts (salary disbursement / WPS).
 */
@Service
@Transactional
public class EmployeeBankAccountService {

    private final EmployeeBankAccountRepository repository;

    public EmployeeBankAccountService(EmployeeBankAccountRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeBankAccountDto> findByEmployee(UUID employeeId) {
        return repository.findByEmployeeIdOrderByPrimaryDesc(employeeId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeBankAccountDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public EmployeeBankAccountDto create(EmployeeBankAccountDto dto) {
        EmployeeBankAccount entity = new EmployeeBankAccount();
        entity.setEmployeeId(dto.getEmployeeId());
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public EmployeeBankAccountDto update(UUID id, EmployeeBankAccountDto dto) {
        EmployeeBankAccount entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private EmployeeBankAccount getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found: " + id));
    }

    private void apply(EmployeeBankAccountDto dto, EmployeeBankAccount entity) {
        entity.setBankId(dto.getBankId());
        entity.setAccountHolderName(dto.getAccountHolderName());
        entity.setIban(dto.getIban());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setPrimary(dto.isPrimary());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private EmployeeBankAccountDto toDto(EmployeeBankAccount entity) {
        EmployeeBankAccountDto dto = new EmployeeBankAccountDto();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setBankId(entity.getBankId());
        dto.setAccountHolderName(entity.getAccountHolderName());
        dto.setIban(entity.getIban());
        dto.setAccountNumber(entity.getAccountNumber());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setPrimary(entity.isPrimary());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
