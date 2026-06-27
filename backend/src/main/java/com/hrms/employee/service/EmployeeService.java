package com.hrms.employee.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.common.web.PageResponse;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.dto.EmployeeDto;
import com.hrms.employee.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * CRUD for employees (FTDD Vol.1 Ch.2). Company-scoped via {@link TenantContext}.
 */
@Service
@Transactional
public class EmployeeService {

    private final EmployeeRepository repository;

    public EmployeeService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeDto> findAll(String q, Pageable pageable) {
        UUID companyId = TenantContext.requireCompanyId();
        Page<EmployeeDto> page = (q == null || q.isBlank())
                ? repository.findByCompanyId(companyId, pageable).map(this::toDto)
                : repository.search(companyId, q.trim(), pageable).map(this::toDto);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public EmployeeDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public EmployeeDto create(EmployeeDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        if (repository.existsByCompanyIdAndEmployeeNumber(companyId, dto.getEmployeeNumber())) {
            throw new BusinessRuleException("employee.number.duplicate",
                    "Employee number already exists: " + dto.getEmployeeNumber());
        }
        Employee entity = new Employee();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public EmployeeDto update(UUID id, EmployeeDto dto) {
        Employee entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Employee getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + id));
    }

    private void apply(EmployeeDto dto, Employee entity) {
        entity.setEmployeeNumber(dto.getEmployeeNumber());
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setMiddleName(dto.getMiddleName());
        entity.setNationalityCountryCode(dto.getNationalityCountryCode());
        entity.setMaritalStatus(dto.getMaritalStatus());
        entity.setAddressLine(dto.getAddressLine());
        entity.setCity(dto.getCity());
        entity.setCountryOfResidenceCode(dto.getCountryOfResidenceCode());
        entity.setDateOfBirth(dto.getDateOfBirth());
        entity.setGender(dto.getGender());
        entity.setHireDate(dto.getHireDate());
        entity.setTerminationDate(dto.getTerminationDate());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setJobTitle(dto.getJobTitle());
        entity.setJobTitleCode(dto.getJobTitleCode());
        entity.setPayStatus(dto.getPayStatus());
        entity.setArabicName(dto.getArabicName());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private EmployeeDto toDto(Employee entity) {
        EmployeeDto dto = new EmployeeDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setEmployeeNumber(entity.getEmployeeNumber());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setMiddleName(entity.getMiddleName());
        dto.setNationalityCountryCode(entity.getNationalityCountryCode());
        dto.setMaritalStatus(entity.getMaritalStatus());
        dto.setAddressLine(entity.getAddressLine());
        dto.setCity(entity.getCity());
        dto.setCountryOfResidenceCode(entity.getCountryOfResidenceCode());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setGender(entity.getGender());
        dto.setHireDate(entity.getHireDate());
        dto.setTerminationDate(entity.getTerminationDate());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setJobTitle(entity.getJobTitle());
        dto.setJobTitleCode(entity.getJobTitleCode());
        dto.setPayStatus(entity.getPayStatus());
        dto.setArabicName(entity.getArabicName());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
