package com.hrms.timesheet.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.employee.repository.AssignmentRepository;
import com.hrms.timesheet.domain.Shift;
import com.hrms.timesheet.domain.EmployeeShift;
import com.hrms.timesheet.dto.BulkAssignRequest;
import com.hrms.timesheet.dto.EmployeeShiftDto;
import com.hrms.timesheet.repository.EmployeeShiftRepository;
import com.hrms.timesheet.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Roster: assign employees to shifts, effective-dated (FTDD Vol.1 Ch.4). */
@Service
@Transactional
public class EmployeeShiftService {

    private final EmployeeShiftRepository repository;
    private final EmployeeRepository employeeRepository;
    private final AssignmentRepository assignmentRepository;
    private final ShiftRepository shiftRepository;

    public EmployeeShiftService(EmployeeShiftRepository repository,
                                EmployeeRepository employeeRepository,
                                AssignmentRepository assignmentRepository,
                                ShiftRepository shiftRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.assignmentRepository = assignmentRepository;
        this.shiftRepository = shiftRepository;
    }

    @Transactional(readOnly = true)
    public List<EmployeeShiftDto> findAll(UUID employeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        List<EmployeeShift> rows = employeeId != null
                ? repository.findByCompanyIdAndEmployeeIdOrderByEffectiveFromDesc(companyId, employeeId)
                : repository.findByCompanyIdOrderByEffectiveFromDesc(companyId);
        Map<UUID, String> shiftCodes = shiftRepository.findByCompanyIdOrderByCode(companyId).stream()
                .collect(Collectors.toMap(s -> s.getId(), s -> s.getCode()));
        return rows.stream().map(r -> toDto(r, shiftCodes)).toList();
    }

    public EmployeeShiftDto create(EmployeeShiftDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        EmployeeShift entity = new EmployeeShift();
        entity.setCompanyId(companyId);
        apply(dto, entity);
        return toDto(repository.save(entity), null);
    }

    public EmployeeShiftDto update(UUID id, EmployeeShiftDto dto) {
        EmployeeShift entity = getEntity(id);
        apply(dto, entity);
        return toDto(repository.save(entity), null);
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    /** Assign many employees to one shift at once. Skips employees already on that
     *  shift with an open-ended assignment. Returns the count created. */
    public int bulkAssign(BulkAssignRequest req) {
        UUID companyId = TenantContext.requireCompanyId();
        if (req.getShiftId() == null || req.getEmployeeIds() == null || req.getEmployeeIds().isEmpty()) {
            return 0;
        }
        LocalDate from = req.getEffectiveFrom() != null ? req.getEffectiveFrom() : LocalDate.now();
        int created = 0;
        for (UUID employeeId : req.getEmployeeIds()) {
            boolean already = repository
                    .findByCompanyIdAndEmployeeIdOrderByEffectiveFromDesc(companyId, employeeId).stream()
                    .anyMatch(es -> req.getShiftId().equals(es.getShiftId()) && es.getEffectiveTo() == null);
            if (already) {
                continue;
            }
            EmployeeShift e = new EmployeeShift();
            e.setCompanyId(companyId);
            e.setEmployeeId(employeeId);
            e.setShiftId(req.getShiftId());
            e.setEffectiveFrom(from);
            validateShiftProject(e);
            repository.save(e);
            created++;
        }
        return created;
    }

    private EmployeeShift getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Roster entry not found: " + id));
    }

    private void apply(EmployeeShiftDto dto, EmployeeShift e) {
        e.setEmployeeId(dto.getEmployeeId());
        e.setShiftId(dto.getShiftId());
        e.setEffectiveFrom(dto.getEffectiveFrom() != null ? dto.getEffectiveFrom() : LocalDate.now());
        e.setEffectiveTo(dto.getEffectiveTo());
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
        validateShiftProject(e);
    }

    private void validateShiftProject(EmployeeShift e) {
        Shift shift = shiftRepository.findById(e.getShiftId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + e.getShiftId()));
        if (shift.getProjectId() == null) {
            return;
        }
        UUID employeeProject = assignmentRepository.findByEmployeeIdOrderByEffectiveFromDesc(e.getEmployeeId()).stream()
                .filter(a -> a.getProjectId() != null && "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .findFirst()
                .map(Assignment::getProjectId)
                .orElse(null);
        if (!shift.getProjectId().equals(employeeProject)) {
            throw new BusinessRuleException("employee-shift.project.mismatch",
                    "Shift project must match the employee's active project assignment.");
        }
    }

    private EmployeeShiftDto toDto(EmployeeShift e, Map<UUID, String> shiftCodes) {
        EmployeeShiftDto dto = new EmployeeShiftDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setEmployeeId(e.getEmployeeId());
        dto.setShiftId(e.getShiftId());
        dto.setEffectiveFrom(e.getEffectiveFrom());
        dto.setEffectiveTo(e.getEffectiveTo());
        dto.setStatus(e.getStatus());
        if (shiftCodes != null) {
            dto.setShiftCode(shiftCodes.get(e.getShiftId()));
        } else {
            shiftRepository.findById(e.getShiftId()).ifPresent(s -> dto.setShiftCode(s.getCode()));
        }
        employeeRepository.findById(e.getEmployeeId()).ifPresent(emp -> {
            dto.setEmployeeName((emp.getFirstName() + " " + emp.getLastName()).trim());
            dto.setEmployeeNumber(emp.getEmployeeNumber());
        });
        return dto;
    }
}
