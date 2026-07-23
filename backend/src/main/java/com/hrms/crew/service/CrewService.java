package com.hrms.crew.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.crew.domain.Crew;
import com.hrms.crew.domain.CrewMember;
import com.hrms.crew.domain.CrewTrade;
import com.hrms.crew.dto.BulkCrewMemberRequest;
import com.hrms.crew.dto.CrewDto;
import com.hrms.crew.dto.CrewMemberDto;
import com.hrms.crew.dto.CrewTradeDto;
import com.hrms.crew.repository.CrewMemberRepository;
import com.hrms.crew.repository.CrewRepository;
import com.hrms.crew.repository.CrewTradeRepository;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.EmployeeShift;
import com.hrms.timesheet.repository.EmployeeShiftRepository;
import com.hrms.timesheet.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final CrewTradeRepository tradeRepository;

    public CrewService(CrewRepository repository, CrewMemberRepository memberRepository,
                       EmployeeRepository employeeRepository, ProjectRepository projectRepository,
                       ShiftRepository shiftRepository, EmployeeShiftRepository employeeShiftRepository,
                       CrewTradeRepository tradeRepository) {
        this.repository = repository;
        this.memberRepository = memberRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.shiftRepository = shiftRepository;
        this.employeeShiftRepository = employeeShiftRepository;
        this.tradeRepository = tradeRepository;
    }

    // --- trades (planned vs assigned head-count) ---------------------

    @Transactional(readOnly = true)
    public List<CrewTradeDto> listTrades(UUID crewId) {
        Map<String, Integer> assignedByCode = new HashMap<>();
        for (CrewMember m : memberRepository.findByCrewIdOrderByEffectiveFromDesc(crewId)) {
            if (m.getEffectiveTo() != null) {
                continue;
            }
            employeeRepository.findById(m.getEmployeeId()).ifPresent(emp -> {
                String code = emp.getJobTitleCode() != null ? emp.getJobTitleCode() : emp.getJobTitle();
                if (code != null) {
                    assignedByCode.merge(code, 1, Integer::sum);
                }
            });
        }
        return tradeRepository.findByCrewIdOrderByTradeCode(crewId).stream().map(t -> {
            CrewTradeDto dto = new CrewTradeDto();
            dto.setId(t.getId());
            dto.setCrewId(t.getCrewId());
            dto.setTradeCode(t.getTradeCode());
            dto.setTradeName(t.getTradeName());
            dto.setPlannedCount(t.getPlannedCount());
            dto.setAssignedCount(assignedByCode.getOrDefault(t.getTradeCode(), 0));
            return dto;
        }).toList();
    }

    public CrewTradeDto addTrade(UUID crewId, CrewTradeDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        getEntity(crewId);
        CrewTrade t = new CrewTrade();
        t.setCompanyId(companyId);
        t.setCrewId(crewId);
        t.setTradeCode(dto.getTradeCode());
        t.setTradeName(dto.getTradeName());
        t.setPlannedCount(dto.getPlannedCount());
        tradeRepository.save(t);
        return dto;
    }

    public void removeTrade(UUID tradeId) {
        tradeRepository.findById(tradeId).ifPresent(tradeRepository::delete);
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
        String code = generateOrNormalizeCode(companyId, dto.getProjectId(), dto.getCode());
        dto.setCode(code);
        // Code is unique per project (the same code may be reused in another project).
        boolean dup = dto.getProjectId() != null
                ? repository.existsByCompanyIdAndProjectIdAndCode(companyId, dto.getProjectId(), code)
                : repository.existsByCompanyIdAndCode(companyId, code);
        if (dup) {
            throw new BusinessRuleException("crew.code.duplicate",
                    "Crew code already exists in this project: " + code);
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
        e.setCode(normalizeCode(dto.getCode()));
        e.setName(dto.getName());
        e.setProjectId(dto.getProjectId());
        e.setForemanEmployeeId(dto.getForemanEmployeeId());
        e.setSupervisorEmployeeId(dto.getSupervisorEmployeeId());
        e.setTimekeeperEmployeeId(dto.getTimekeeperEmployeeId());
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
        dto.setSupervisorEmployeeId(e.getSupervisorEmployeeId());
        dto.setTimekeeperEmployeeId(e.getTimekeeperEmployeeId());
        dto.setParentCrewId(e.getParentCrewId());
        dto.setStatus(e.getStatus());
        if (e.getProjectId() != null) {
            projectRepository.findById(e.getProjectId()).ifPresent(p -> dto.setProjectCode(p.getCode()));
        }
        if (e.getForemanEmployeeId() != null) {
            employeeRepository.findById(e.getForemanEmployeeId())
                    .ifPresent(emp -> dto.setForemanName((emp.getFirstName() + " " + emp.getLastName()).trim()));
        }
        if (e.getSupervisorEmployeeId() != null) {
            employeeRepository.findById(e.getSupervisorEmployeeId())
                    .ifPresent(emp -> dto.setSupervisorName((emp.getFirstName() + " " + emp.getLastName()).trim()));
        }
        if (e.getTimekeeperEmployeeId() != null) {
            employeeRepository.findById(e.getTimekeeperEmployeeId())
                    .ifPresent(emp -> dto.setTimekeeperName((emp.getFirstName() + " " + emp.getLastName()).trim()));
        }
        dto.setMemberCount(memberRepository.findByCrewIdOrderByEffectiveFromDesc(e.getId()).size());
        return dto;
    }

    private String generateOrNormalizeCode(UUID companyId, UUID projectId, String raw) {
        String code = normalizeCode(raw);
        if (code == null || code.isBlank()) {
            throw new BusinessRuleException("crew.code.required", "Crew code prefix is required.");
        }
        if (code.matches(".*-\\d{3,}$")) {
            return code;
        }
        String prefix = code.endsWith("-") ? code.substring(0, code.length() - 1) : code;
        String startsWith = prefix + "-";
        List<Crew> existing = projectId != null
                ? repository.findByCompanyIdAndProjectIdAndCodeStartingWithOrderByCode(companyId, projectId, startsWith)
                : repository.findByCompanyIdAndCodeStartingWithOrderByCode(companyId, startsWith);
        int max = existing.stream()
                .map(Crew::getCode)
                .mapToInt(existingCode -> parseSuffix(existingCode, startsWith))
                .max()
                .orElse(0);
        return "%s-%03d".formatted(prefix, max + 1);
    }

    private String normalizeCode(String raw) {
        return raw == null ? null : raw.trim().toUpperCase().replaceAll("\\s+", "-");
    }

    private int parseSuffix(String code, String prefix) {
        if (code == null || !code.startsWith(prefix)) {
            return 0;
        }
        try {
            return Integer.parseInt(code.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
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
