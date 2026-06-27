package com.hrms.employee.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.dto.AssignmentDto;
import com.hrms.employee.repository.AssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD for employee assignments to organisation units (FTDD Vol.1 Ch.2).
 */
@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository repository;

    public AssignmentService(AssignmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AssignmentDto> findByEmployee(UUID employeeId) {
        return repository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AssignmentDto findById(UUID id) {
        return toDto(getEntity(id));
    }

    public AssignmentDto create(AssignmentDto dto) {
        Assignment entity = new Assignment();
        entity.setEmployeeId(dto.getEmployeeId());
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public AssignmentDto update(UUID id, AssignmentDto dto) {
        Assignment entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    private Assignment getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + id));
    }

    private void apply(AssignmentDto dto, Assignment entity) {
        entity.setOrganizationUnitId(dto.getOrganizationUnitId());
        entity.setPositionTitle(dto.getPositionTitle());
        entity.setSupervisorEmployeeId(dto.getSupervisorEmployeeId());
        entity.setProjectId(dto.getProjectId());
        entity.setCostCodeId(dto.getCostCodeId());
        entity.setPrimaryAssignment(dto.isPrimaryAssignment());
        entity.setEffectiveFrom(dto.getEffectiveFrom());
        entity.setEffectiveTo(dto.getEffectiveTo());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    private AssignmentDto toDto(Assignment entity) {
        AssignmentDto dto = new AssignmentDto();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployeeId());
        dto.setOrganizationUnitId(entity.getOrganizationUnitId());
        dto.setPositionTitle(entity.getPositionTitle());
        dto.setSupervisorEmployeeId(entity.getSupervisorEmployeeId());
        dto.setProjectId(entity.getProjectId());
        dto.setCostCodeId(entity.getCostCodeId());
        dto.setPrimaryAssignment(entity.isPrimaryAssignment());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setEffectiveTo(entity.getEffectiveTo());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}
