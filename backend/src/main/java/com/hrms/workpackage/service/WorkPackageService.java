package com.hrms.workpackage.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.crew.domain.Crew;
import com.hrms.crew.domain.CrewMember;
import com.hrms.crew.repository.CrewMemberRepository;
import com.hrms.crew.repository.CrewRepository;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.project.domain.CostCode;
import com.hrms.project.repository.CostCodeRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.workpackage.domain.WorkPackage;
import com.hrms.workpackage.domain.WorkPackageCrew;
import com.hrms.workpackage.domain.WorkPackageRequirement;
import com.hrms.workpackage.dto.WorkPackageCrewDto;
import com.hrms.workpackage.dto.WorkPackageDto;
import com.hrms.workpackage.dto.WorkPackageRequirementDto;
import com.hrms.workpackage.repository.WorkPackageCrewRepository;
import com.hrms.workpackage.repository.WorkPackageRepository;
import com.hrms.workpackage.repository.WorkPackageRequirementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class WorkPackageService {

    private final WorkPackageRepository repository;
    private final WorkPackageRequirementRepository requirementRepository;
    private final WorkPackageCrewRepository crewLinkRepository;
    private final ProjectRepository projectRepository;
    private final CostCodeRepository costCodeRepository;
    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final EmployeeRepository employeeRepository;

    public WorkPackageService(WorkPackageRepository repository,
                              WorkPackageRequirementRepository requirementRepository,
                              WorkPackageCrewRepository crewLinkRepository,
                              ProjectRepository projectRepository,
                              CostCodeRepository costCodeRepository,
                              CrewRepository crewRepository,
                              CrewMemberRepository crewMemberRepository,
                              EmployeeRepository employeeRepository) {
        this.repository = repository;
        this.requirementRepository = requirementRepository;
        this.crewLinkRepository = crewLinkRepository;
        this.projectRepository = projectRepository;
        this.costCodeRepository = costCodeRepository;
        this.crewRepository = crewRepository;
        this.crewMemberRepository = crewMemberRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkPackageDto> list(UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        List<WorkPackage> rows = projectId == null
                ? repository.findByCompanyIdOrderByCode(companyId)
                : repository.findByCompanyIdAndProjectIdOrderByCode(companyId, projectId);
        return rows.stream().map(this::toDto).toList();
    }

    public WorkPackageDto create(WorkPackageDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        require(dto.getProjectId() != null, "Project is required.");
        require(dto.getName() != null && !dto.getName().isBlank(), "Work package name is required.");
        String code = generateCode(companyId, dto.getProjectId(), dto.getCode());
        if (repository.existsByCompanyIdAndProjectIdAndCode(companyId, dto.getProjectId(), code)) {
            throw new BusinessRuleException("workpackage.code.duplicate",
                    "Work package code already exists in this project: " + code);
        }
        WorkPackage wp = new WorkPackage();
        wp.setCompanyId(companyId);
        dto.setCode(code);
        apply(dto, wp);
        return toDto(repository.save(wp));
    }

    public WorkPackageDto update(UUID id, WorkPackageDto dto) {
        WorkPackage wp = getEntity(id);
        apply(dto, wp);
        return toDto(repository.save(wp));
    }

    public void delete(UUID id) {
        repository.delete(getEntity(id));
    }

    @Transactional(readOnly = true)
    public List<WorkPackageRequirementDto> requirements(UUID workPackageId) {
        getEntity(workPackageId);
        Map<String, Integer> assignedByJob = assignedByJobTitle(workPackageId);
        return requirementRepository.findByWorkPackageIdOrderByJobTitleCode(workPackageId).stream()
                .map(r -> toRequirementDto(r, assignedByJob.getOrDefault(r.getJobTitleCode(), 0)))
                .toList();
    }

    public WorkPackageRequirementDto addRequirement(UUID workPackageId, WorkPackageRequirementDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        getEntity(workPackageId);
        String code = normalizeCode(dto.getJobTitleCode());
        require(code != null && !code.isBlank(), "Job title is required.");
        require(dto.getRequiredCount() > 0, "Required count must be greater than zero.");
        if (requirementRepository.existsByWorkPackageIdAndJobTitleCode(workPackageId, code)) {
            throw new BusinessRuleException("workpackage.requirement.duplicate",
                    "This job title is already required on the work package.");
        }
        WorkPackageRequirement r = new WorkPackageRequirement();
        r.setCompanyId(companyId);
        r.setWorkPackageId(workPackageId);
        r.setJobTitleCode(code);
        r.setJobTitleName(dto.getJobTitleName());
        r.setRequiredCount(dto.getRequiredCount());
        return toRequirementDto(requirementRepository.save(r), 0);
    }

    public void removeRequirement(UUID id) {
        requirementRepository.findById(id).ifPresent(requirementRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<WorkPackageCrewDto> crews(UUID workPackageId) {
        getEntity(workPackageId);
        return crewLinkRepository.findByWorkPackageIdOrderByPlannedStartAsc(workPackageId).stream()
                .map(this::toCrewDto)
                .toList();
    }

    public WorkPackageCrewDto addCrew(UUID workPackageId, WorkPackageCrewDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        WorkPackage wp = getEntity(workPackageId);
        require(dto.getCrewId() != null, "Crew is required.");
        Crew crew = crewRepository.findById(dto.getCrewId())
                .orElseThrow(() -> new ResourceNotFoundException("Crew not found: " + dto.getCrewId()));
        if (crew.getProjectId() != null && !crew.getProjectId().equals(wp.getProjectId())) {
            throw new BusinessRuleException("workpackage.crew.project",
                    "Crew must belong to the same project as the work package.");
        }
        if (crewLinkRepository.existsByWorkPackageIdAndCrewId(workPackageId, dto.getCrewId())) {
            throw new BusinessRuleException("workpackage.crew.duplicate",
                    "This crew is already assigned to the work package.");
        }
        WorkPackageCrew link = new WorkPackageCrew();
        link.setCompanyId(companyId);
        link.setWorkPackageId(workPackageId);
        link.setCrewId(dto.getCrewId());
        link.setPlannedStart(dto.getPlannedStart());
        link.setPlannedEnd(dto.getPlannedEnd());
        link.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        return toCrewDto(crewLinkRepository.save(link));
    }

    public void removeCrew(UUID id) {
        crewLinkRepository.findById(id).ifPresent(crewLinkRepository::delete);
    }

    private WorkPackage getEntity(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work package not found: " + id));
    }

    private void apply(WorkPackageDto dto, WorkPackage wp) {
        if (dto.getProjectId() != null) {
            wp.setProjectId(dto.getProjectId());
        }
        wp.setCostCodeId(dto.getCostCodeId());
        if (dto.getCode() != null && !dto.getCode().isBlank()) {
            wp.setCode(normalizeCode(dto.getCode()));
        }
        wp.setName(dto.getName());
        wp.setDescription(dto.getDescription());
        wp.setPlannedStart(dto.getPlannedStart());
        wp.setPlannedEnd(dto.getPlannedEnd());
        if (dto.getStatus() != null) {
            wp.setStatus(dto.getStatus());
        }
    }

    private WorkPackageDto toDto(WorkPackage wp) {
        WorkPackageDto dto = new WorkPackageDto();
        dto.setId(wp.getId());
        dto.setCompanyId(wp.getCompanyId());
        dto.setProjectId(wp.getProjectId());
        dto.setCostCodeId(wp.getCostCodeId());
        dto.setCode(wp.getCode());
        dto.setName(wp.getName());
        dto.setDescription(wp.getDescription());
        dto.setPlannedStart(wp.getPlannedStart());
        dto.setPlannedEnd(wp.getPlannedEnd());
        dto.setStatus(wp.getStatus());
        projectRepository.findById(wp.getProjectId()).ifPresent(p -> {
            dto.setProjectCode(p.getCode());
            dto.setProjectName(p.getName());
        });
        if (wp.getCostCodeId() != null) {
            costCodeRepository.findById(wp.getCostCodeId()).ifPresent(c -> {
                dto.setCostCode(c.getCode());
                dto.setCostCodeName(c.getDescription() != null && !c.getDescription().isBlank()
                        ? c.getDescription() : c.getName());
            });
        }
        dto.setRequirementCount(requirementRepository.countByWorkPackageId(wp.getId()));
        dto.setCrewCount(crewLinkRepository.countByWorkPackageId(wp.getId()));
        return dto;
    }

    private WorkPackageRequirementDto toRequirementDto(WorkPackageRequirement r, int assignedCount) {
        WorkPackageRequirementDto dto = new WorkPackageRequirementDto();
        dto.setId(r.getId());
        dto.setWorkPackageId(r.getWorkPackageId());
        dto.setJobTitleCode(r.getJobTitleCode());
        dto.setJobTitleName(r.getJobTitleName());
        dto.setRequiredCount(r.getRequiredCount());
        dto.setAssignedCount(assignedCount);
        return dto;
    }

    private WorkPackageCrewDto toCrewDto(WorkPackageCrew link) {
        WorkPackageCrewDto dto = new WorkPackageCrewDto();
        dto.setId(link.getId());
        dto.setWorkPackageId(link.getWorkPackageId());
        dto.setCrewId(link.getCrewId());
        dto.setPlannedStart(link.getPlannedStart());
        dto.setPlannedEnd(link.getPlannedEnd());
        dto.setStatus(link.getStatus());
        crewRepository.findById(link.getCrewId()).ifPresent(c -> {
            dto.setCrewCode(c.getCode());
            dto.setCrewName(c.getName());
        });
        return dto;
    }

    private Map<String, Integer> assignedByJobTitle(UUID workPackageId) {
        Map<String, Integer> counts = new HashMap<>();
        for (WorkPackageCrew link : crewLinkRepository.findByWorkPackageIdOrderByPlannedStartAsc(workPackageId)) {
            for (CrewMember member : crewMemberRepository.findByCrewIdOrderByEffectiveFromDesc(link.getCrewId())) {
                if (member.getEffectiveTo() != null) {
                    continue;
                }
                employeeRepository.findById(member.getEmployeeId()).ifPresent(emp -> {
                    String code = employeeJobCode(emp);
                    if (code != null && !code.isBlank()) {
                        counts.merge(code, 1, Integer::sum);
                    }
                });
            }
        }
        return counts;
    }

    private String employeeJobCode(Employee emp) {
        String code = emp.getJobTitleCode() != null ? emp.getJobTitleCode() : emp.getJobTitle();
        return normalizeCode(code);
    }

    private String generateCode(UUID companyId, UUID projectId, String raw) {
        String code = normalizeCode(raw);
        require(code != null && !code.isBlank(), "Work package code prefix is required.");
        if (code.matches(".*-\\d{3,}$")) {
            return code;
        }
        String prefix = code.startsWith("WP-") ? code : "WP-" + code;
        prefix = prefix.endsWith("-") ? prefix.substring(0, prefix.length() - 1) : prefix;
        String startsWith = prefix + "-";
        int max = repository.findByCompanyIdAndProjectIdAndCodeStartingWithOrderByCode(companyId, projectId, startsWith)
                .stream()
                .map(WorkPackage::getCode)
                .mapToInt(existing -> parseSuffix(existing, startsWith))
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

    private void require(boolean ok, String message) {
        if (!ok) {
            throw new BusinessRuleException("workpackage.invalid", message);
        }
    }
}
