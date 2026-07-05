package com.hrms.employee.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.common.web.PageResponse;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.dto.EmployeeDto;
import com.hrms.employee.dto.EmployeeSummaryDto;
import com.hrms.employee.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

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
    public PageResponse<EmployeeDto> findAll(String q, String payStatus, UUID projectId,
                                             boolean activeOnly, boolean assignedOnly, boolean unassigned,
                                             Pageable pageable) {
        UUID companyId = TenantContext.requireCompanyId();
        Page<EmployeeDto> page = repository.searchFiltered(companyId, q, payStatus, projectId,
                        activeOnly, assignedOnly, unassigned, pageable)
                .map(this::toDto);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public EmployeeSummaryDto summary(String q, UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        Object[] row = repository.summaryCounts(companyId, q, projectId);
        // Single-row aggregate; some JPA providers wrap it as Object[]{Object[]}.
        if (row != null && row.length == 1 && row[0] instanceof Object[]) {
            row = (Object[]) row[0];
        }
        long total = asLong(row, 0);
        long active = asLong(row, 1);
        long monthly = asLong(row, 2);
        long daily = asLong(row, 3);
        long withoutProject = asLong(row, 4);
        return new EmployeeSummaryDto(total, active, total - active, monthly, daily, withoutProject);
    }

    private static long asLong(Object[] row, int idx) {
        if (row == null || idx >= row.length || row[idx] == null) {
            return 0L;
        }
        return ((Number) row[idx]).longValue();
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
        if (repository.existsByCompanyIdAndEmployeeNumberAndIdNot(
                entity.getCompanyId(), dto.getEmployeeNumber(), entity.getId())) {
            throw new BusinessRuleException("employee.number.duplicate",
                    "Employee number already exists: " + dto.getEmployeeNumber());
        }
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    public int assignTimekeeperForProject(UUID projectId, UUID timekeeperEmployeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        Employee timekeeper = repository.findById(timekeeperEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Timekeeper employee not found: " + timekeeperEmployeeId));
        if (!companyId.equals(timekeeper.getCompanyId())) {
            throw new ResourceNotFoundException("Timekeeper employee not found: " + timekeeperEmployeeId);
        }
        return repository.assignTimekeeperForActiveProjectEmployees(companyId, projectId, timekeeperEmployeeId);
    }

    public int assignTimekeeperForEmployees(List<UUID> employeeIds, UUID timekeeperEmployeeId, UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        if (employeeIds == null || employeeIds.isEmpty()) {
            return 0;
        }
        Employee timekeeper = repository.findById(timekeeperEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Timekeeper employee not found: " + timekeeperEmployeeId));
        if (!companyId.equals(timekeeper.getCompanyId())) {
            throw new ResourceNotFoundException("Timekeeper employee not found: " + timekeeperEmployeeId);
        }
        return repository.assignTimekeeperForEmployees(companyId, employeeIds, timekeeperEmployeeId, projectId);
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
        entity.setOvertimeCategoryCode(dto.getOvertimeCategoryCode());
        entity.setBand(dto.getBand());
        entity.setArabicName(dto.getArabicName());
        entity.setSupervisorEmployeeId(dto.getSupervisorEmployeeId());
        entity.setTimekeeperEmployeeId(dto.getTimekeeperEmployeeId());
        entity.setPhotoUrl(dto.getPhotoUrl());
        if (dto.getStatus() != null) {
            if (requiresTerminationDate(dto.getStatus()) && dto.getTerminationDate() == null) {
                throw new BusinessRuleException("employee.termination-date.required",
                        "Termination date is required when employee status is TERMINATED or SUSPENDED.");
            }
            entity.setStatus(dto.getStatus());
        }
    }

    private static boolean requiresTerminationDate(String status) {
        return "TERMINATED".equalsIgnoreCase(status) || "SUSPENDED".equalsIgnoreCase(status);
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
        dto.setOvertimeCategoryCode(entity.getOvertimeCategoryCode());
        dto.setBand(entity.getBand());
        dto.setArabicName(entity.getArabicName());
        dto.setSupervisorEmployeeId(entity.getSupervisorEmployeeId());
        dto.setTimekeeperEmployeeId(entity.getTimekeeperEmployeeId());
        dto.setPhotoUrl(entity.getPhotoUrl());
        if (entity.getSupervisorEmployeeId() != null) {
            repository.findById(entity.getSupervisorEmployeeId()).ifPresent(sup ->
                    dto.setSupervisorName((sup.getFirstName() + " " + sup.getLastName()).trim()));
        }
        if (entity.getTimekeeperEmployeeId() != null) {
            repository.findById(entity.getTimekeeperEmployeeId()).ifPresent(tk ->
                    dto.setTimekeeperName((tk.getFirstName() + " " + tk.getLastName()).trim()));
        }
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
