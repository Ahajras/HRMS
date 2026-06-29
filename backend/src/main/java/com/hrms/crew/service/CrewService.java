package com.hrms.crew.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.crew.domain.Crew;
import com.hrms.crew.domain.CrewMember;
import com.hrms.crew.dto.BulkCrewMemberRequest;
import com.hrms.crew.dto.CrewDto;
import com.hrms.crew.dto.CrewMemberDto;
import com.hrms.crew.repository.CrewMemberRepository;
import com.hrms.crew.repository.CrewRepository;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.EmployeeShift;
import com.hrms.timesheet.repository.EmployeeShiftRepository;
import com.hrms.timesheet.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Crew master data + membership (FTDD Vol.1 Ch.4; legacy PAYREF). Company-scoped. */
@Service
@Transactional
public class CrewService {

    private final CrewRepository repository;
    private final CrewMemberRepository memberRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeShiftRepository employeeShiftRepository;

    public CrewService(CrewRepository repository, CrewMemberRepository memberRepository,
                       EmployeeRepository employeeRepository, ProjectRepository projectRepository,
                       ShiftRepository shiftRepository, EmployeeShiftRepository employeeShiftRepository) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.shiftRepository = shiftRepository;
        this.employeeShiftRepository = employeeShiftRepository;
    }

    /** Mirror a crew member's shift into the Shift Roster so it reflects there too. */
    private void ensureRoster(UUID companyId, UUID employeeId, UUID shiftId, LocalDate from) {
        if (shiftId == null) {
            return;
        }
        boolean already = employeeShiftRepository
                .findByCompanyIdAndEmployeeIdOrderByEffectiveFromDesc(companyId, employeeId).stream()
                .anyMatch(es -> shiftId.equals(es.getShiftId()) && es.getEffectiveTo() == null);
        if (already) {
            return;
        }
        EmployeeShift es = new EmployeeShift();
        es.setCompanyId(companyId);
        es.setEmployeeId(employeeId);
        es.setShiftId(shiftId);
        es.setEffectiveFrom(from);
        employeeShiftRepository.save(es);
    }

    @Transactional(readOnly = true)
    public List<CrewDto> findAll() {
        UUID companyId = TenantContext.requireCompanyId();
        return repository.findByCompanyIdOrderByCode(companyId).stream().map(this::toDto).toList();
    }

    public CrewDto create(CrewDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        // Code is unique per project (the same code may be reused in another project).
        boolean dup = dto.getProjectId() != null
                ? repository.existsByCompanyIdAndProjectIdAndCode(companyId, dto.getProjectId(), dto.getCode())
                : repository.existsByCompanyIdAndCode(companyId, dto.getCode());
        if (dup) {
            throw new BusinessRuleException("crew.code.duplicate",
                    "Crew code already exists in this project: " + dto.getCode());
        }
        Crew e = new Crew();
        e.setCompanyId(companyId);
        apply(dto, e);
        return toDto(repository.save(e));
    }

    public CrewDto update(UUID id, CrewDto dto) {
        Crew e = getEntity(id);
        apply(dto, e);
        return toDto(repository.save(e));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id)); // crew_member cascades in DB
    }

    /** The crew an employee currently belongs to (most recent open membership), or null. */
    @Transactional(readOnly = true)
    public CrewDto findByEmployee(UUID employeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        return memberRepository.findByCompanyIdAndEmployeeId(companyId, employeeId).stream()
                .filter(m -> m.getEffectiveTo() == null)
                .findFirst()
                .or(() -> memberRepository.findByCompanyIdAndEmployeeId(companyId, employeeId).stream().findFirst())
                .flatMap(m -> repository.findById(m.getCrewId()))
                .map(this::toDto)
                .orElse(null);
    }

    // --- members -----------------------------------------------------

    @Transactional(readOnly = true)
    public List<CrewMemberDto> listMembers(UUID crewId) {
        return memberRepository.findByCrewIdOrderByEffectiveFromDesc(crewId).stream()
                .map(this::toMemberDto).toList();
    }

    public CrewMemberDto addMember(UUID crewId, CrewMemberDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        getEntity(crewId);
        CrewMember m = new CrewMember();
        m.setCompanyId(companyId);
        m.setCrewId(crewId);
        m.setEmployeeId(dto.getEmployeeId());
        m.setShiftId(dto.getShiftId());
        m.setEffectiveFrom(dto.getEffectiveFrom() != null ? dto.getEffectiveFrom() : LocalDate.now());
        m.setEffectiveTo(dto.getEffectiveTo());
        CrewMember saved = memberRepository.save(m);
        ensureRoster(companyId, saved.getEmployeeId(), saved.getShiftId(), saved.getEffectiveFrom());
        return toMemberDto(saved);
    }

    /** Add many employees to a crew at once, all on one shift. Skips current members. */
    public int bulkAddMembers(UUID crewId, BulkCrewMemberRequest req) {
        UUID companyId = TenantContext.requireCompanyId();
        getEntity(crewId);
        if (req.getEmployeeIds() == null || req.getEmployeeIds().isEmpty()) {
            return 0;
        }
        LocalDate from = req.getEffectiveFrom() != null ? req.getEffectiveFrom() : LocalDate.now();
        List<CrewMember> existing = memberRepository.findByCrewIdOrderByEffectiveFromDesc(crewId);
        int created = 0;
        for (UUID employeeId : req.getEmployeeIds()) {
            boolean already = existing.stream()
                    .anyMatch(m -> employeeId.equals(m.getEmployeeId()) && m.getEffectiveTo() == null);
            if (already) {
                continue;
            }
            CrewMember m = new CrewMember();
            m.setCompanyId(companyId);
            m.setCrewId(crewId);
            m.setEmployeeId(employeeId);
            m.setShiftId(req.getShiftId());
            m.setEffectiveFrom(from);
            memberRepository.save(m);
            ensureRoster(companyId, employeeId, req.getShiftId(), from);
            created++;
        }
        return created;
    }

    public void removeMember(UUID memberId) {
        memberRepository.findById(memberId).ifPresent(memberRepository::delete);
    }

    // --- mapping -----------------------------------------------------

    private Crew getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Crew not found: " + id));
    }

    private void apply(CrewDto dto, Crew e) {
        e.setCode(dto.getCode());
        e.setName(dto.getName());
        e.setProjectId(dto.getProjectId());
        e.setForemanEmployeeId(dto.getForemanEmployeeId());
        e.setParentCrewId(dto.getParentCrewId());
        if (dto.getStatus() != null) {
            e.setStatus(dto.getStatus());
        }
    }

    private CrewDto toDto(Crew e) {
        CrewDto dto = new CrewDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setCode(e.getCode());
        dto.setName(e.getName());
        dto.setProjectId(e.getProjectId());
        dto.setForemanEmployeeId(e.getForemanEmployeeId());
        dto.setParentCrewId(e.getParentCrewId());
        dto.setStatus(e.getStatus());
        if (e.getProjectId() != null) {
            projectRepository.findById(e.getProjectId()).ifPresent(p -> dto.setProjectCode(p.getCode()));
        }
        if (e.getForemanEmployeeId() != null) {
            employeeRepository.findById(e.getForemanEmployeeId())
                    .ifPresent(emp -> dto.setForemanName((emp.getFirstName() + " " + emp.getLastName()).trim()));
        }
        dto.setMemberCount(memberRepository.findByCrewIdOrderByEffectiveFromDesc(e.getId()).size());
        return dto;
    }

    private CrewMemberDto toMemberDto(CrewMember m) {
        CrewMemberDto dto = new CrewMemberDto();
        dto.setId(m.getId());
        dto.setCrewId(m.getCrewId());
        dto.setEmployeeId(m.getEmployeeId());
        dto.setShiftId(m.getShiftId());
        dto.setEffectiveFrom(m.getEffectiveFrom());
        dto.setEffectiveTo(m.getEffectiveTo());
        dto.setStatus(m.getStatus());
        Employee emp = employeeRepository.findById(m.getEmployeeId()).orElse(null);
        if (emp != null) {
            dto.setEmployeeName((emp.getFirstName() + " " + emp.getLastName()).trim());
            dto.setEmployeeNumber(emp.getEmployeeNumber());
        }
        if (m.getShiftId() != null) {
            shiftRepository.findById(m.getShiftId()).ifPresent(s -> dto.setShiftCode(s.getCode()));
        }
        return dto;
    }
}
