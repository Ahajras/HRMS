package com.hrms.crew.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.crew.domain.TimekeeperProject;
import com.hrms.crew.dto.TimekeeperProjectDto;
import com.hrms.crew.repository.TimekeeperProjectRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Links timekeepers (employees) to the projects they may enter attendance for. */
@Service
@Transactional
public class TimekeeperService {

    private final TimekeeperProjectRepository repository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    public TimekeeperService(TimekeeperProjectRepository repository,
                             EmployeeRepository employeeRepository,
                             ProjectRepository projectRepository) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<TimekeeperProjectDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderById(companyId).stream().map(this::toDto).toList();
    }

    public TimekeeperProjectDto create(TimekeeperProjectDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        boolean dup = repository.findByCompanyIdAndEmployeeId(companyId, dto.getEmployeeId()).stream()
                .anyMatch(tp -> tp.getProjectId().equals(dto.getProjectId()));
        if (dup) {
            throw new BusinessRuleException("timekeeper.project.duplicate",
                    "This timekeeper is already assigned to that project.");
        }
        TimekeeperProject e = new TimekeeperProject();
        e.setCompanyId(companyId);
        e.setEmployeeId(dto.getEmployeeId());
        e.setProjectId(dto.getProjectId());
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
        return toDto(repository.save(e));
    }

    public void delete(UUID id) {
        repository.findById(id).ifPresent(repository::delete);
    }

    /** The project IDs a timekeeper (by employee id) is allowed to work on. */
    @Transactional(readOnly = true)
    public List<UUID> allowedProjectIds(UUID employeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdAndEmployeeId(companyId, employeeId).stream()
                .map(TimekeeperProject::getProjectId).toList();
    }

    private TimekeeperProjectDto toDto(TimekeeperProject e) {
        TimekeeperProjectDto dto = new TimekeeperProjectDto();
        dto.setId(e.getId());
        dto.setEmployeeId(e.getEmployeeId());
        dto.setProjectId(e.getProjectId());
        dto.setStatus(e.getStatus());
        employeeRepository.findById(e.getEmployeeId()).ifPresent(emp -> {
            dto.setEmployeeName((emp.getFirstName() + " " + emp.getLastName()).trim());
            dto.setEmployeeNumber(emp.getEmployeeNumber());
        });
        projectRepository.findById(e.getProjectId()).ifPresent(p -> dto.setProjectCode(p.getCode()));
        return dto;
    }
}
