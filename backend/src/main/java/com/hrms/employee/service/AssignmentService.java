package com.hrms.employee.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.dto.AssignmentDto;
import com.hrms.employee.repository.AssignmentRepository;
import com.hrms.timesheet.domain.EmployeeShift;
import com.hrms.timesheet.domain.Shift;
import com.hrms.timesheet.repository.EmployeeShiftRepository;
import com.hrms.timesheet.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * CRUD for employee assignments to organisation units (FTDD Vol.1 Ch.2).
 */
@Service
@Transactional
public class AssignmentService {

    private final AssignmentRepository repository;
    private final EmployeeShiftRepository employeeShiftRepository;
    private final ShiftRepository shiftRepository;

    public AssignmentService(AssignmentRepository repository,
                             EmployeeShiftRepository employeeShiftRepository,
                             ShiftRepository shiftRepository) {
        this.repository = repository;
        this.employeeShiftRepository = employeeShiftRepository;
        this.shiftRepository = shiftRepository;
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
        entity = repository.save(entity);
        closeOldProjectShifts(entity);
        return toDto(entity);
    }

    public AssignmentDto update(UUID id, AssignmentDto dto) {
        Assignment entity = getEntity(id);
        apply(dto, entity);
        entity = repository.save(entity);
        closeOldProjectShifts(entity);
        return toDto(entity);
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

    private void closeOldProjectShifts(Assignment assignment) {
        if (assignment.getEmployeeId() == null || assignment.getEffectiveFrom() == null
                || !"ACTIVE".equalsIgnoreCase(assignment.getStatus())) {
            return;
        }
        LocalDate changeDate = assignment.getEffectiveFrom();
        LocalDate closeDate = changeDate.minusDays(1);
        for (EmployeeShift employeeShift : employeeShiftRepository
                .findByEmployeeIdOrderByEffectiveFromDesc(assignment.getEmployeeId())) {
            if (!"ACTIVE".equalsIgnoreCase(employeeShift.getStatus())) {
                continue;
            }
            if (!activeOn(employeeShift.getEffectiveFrom(), employeeShift.getEffectiveTo(), changeDate)) {
                continue;
            }
            Shift shift = shiftRepository.findById(employeeShift.getShiftId()).orElse(null);
            UUID shiftProject = shift != null ? shift.getProjectId() : null;
            if (shiftProject == null || shiftProject.equals(assignment.getProjectId())) {
                continue;
            }
            if (employeeShift.getEffectiveFrom().isAfter(closeDate)) {
                employeeShift.setStatus("INACTIVE");
            } else {
                employeeShift.setEffectiveTo(closeDate);
            }
            employeeShiftRepository.save(employeeShift);
        }
    }

    private static boolean activeOn(LocalDate from, LocalDate to, LocalDate date) {
        return from != null && !from.isAfter(date) && (to == null || !to.isBefore(date));
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
