package com.hrms.payroll.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.domain.ContractPayItem;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.AssignmentRepository;
import com.hrms.employee.repository.ContractPayItemRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.domain.PayrollComponent;
import com.hrms.payroll.domain.ProvisionResult;
import com.hrms.payroll.domain.ProvisionRun;
import com.hrms.payroll.dto.ProvisionDtos;
import com.hrms.payroll.repository.PayrollComponentRepository;
import com.hrms.payroll.repository.ProvisionResultRepository;
import com.hrms.payroll.repository.ProvisionRunRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProvisionService {

    private final ProvisionRunRepository runRepo;
    private final ProvisionResultRepository resultRepo;
    private final PayrollPeriodRepository periodRepo;
    private final ProjectRepository projectRepo;
    private final EmployeeRepository employeeRepo;
    private final AssignmentRepository assignmentRepo;
    private final ContractPayItemRepository payItemRepo;
    private final PayrollComponentRepository componentRepo;

    public ProvisionService(ProvisionRunRepository runRepo,
                            ProvisionResultRepository resultRepo,
                            PayrollPeriodRepository periodRepo,
                            ProjectRepository projectRepo,
                            EmployeeRepository employeeRepo,
                            AssignmentRepository assignmentRepo,
                            ContractPayItemRepository payItemRepo,
                            PayrollComponentRepository componentRepo) {
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.periodRepo = periodRepo;
        this.projectRepo = projectRepo;
        this.employeeRepo = employeeRepo;
        this.assignmentRepo = assignmentRepo;
        this.payItemRepo = payItemRepo;
        this.componentRepo = componentRepo;
    }

    @Transactional(readOnly = true)
    public List<ProvisionDtos.RunDto> list(UUID periodId) {
        UUID companyId = TenantContext.requireCompanyId();
        List<ProvisionRun> runs = periodId == null
                ? runRepo.findByCompanyIdOrderByCreatedAtDesc(companyId)
                : runRepo.findByCompanyIdAndPeriodIdOrderByCreatedAtDesc(companyId, periodId);
        Map<UUID, PayrollPeriod> periods = periodRepo.findByCompanyIdOrderByPeriodYearDescPeriodMonthDesc(companyId)
                .stream().collect(Collectors.toMap(PayrollPeriod::getId, p -> p, (a, b) -> a));
        return runs.stream().map(r -> toRunDto(r, periods.get(r.getPeriodId()), List.of())).toList();
    }

    @Transactional(readOnly = true)
    public ProvisionDtos.RunDto get(UUID id) {
        UUID companyId = TenantContext.requireCompanyId();
        ProvisionRun run = runRepo.findById(id).filter(r -> companyId.equals(r.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException("Provision run", id));
        PayrollPeriod period = periodRepo.findById(run.getPeriodId()).orElse(null);
        List<ProvisionDtos.ResultDto> results = resultRepo.findByRunIdOrderByEmployeeNumberAsc(run.getId())
                .stream().map(this::toResultDto).toList();
        return toRunDto(run, period, results);
    }

    public ProvisionDtos.RunDto calculate(ProvisionDtos.CreateRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        if (request.getPeriodId() == null) {
            throw new BusinessRuleException("provision.period.required", "Period is required.");
        }
        PayrollPeriod period = periodRepo.findById(request.getPeriodId())
                .filter(p -> companyId.equals(p.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period", request.getPeriodId()));
        if (request.getProjectId() != null) {
            projectRepo.findById(request.getProjectId())
                    .filter(p -> companyId.equals(p.getCompanyId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));
        }

        String payGroup = normalizePayGroup(request.getPayGroup());
        String provisionType = normalizeProvisionType(request.getProvisionType());
        Set<UUID> eligibleComponentIds = eligibleComponentIds(companyId, provisionType);
        if (eligibleComponentIds.isEmpty()) {
            throw new BusinessRuleException("provision.components.required",
                    "No payroll components are marked for " + provisionType + " provision.");
        }

        List<Employee> employees = employeeRepo.findProvisionScope(
                companyId, period.getStartDate(), period.getEndDate(), request.getProjectId(), payGroup);
        List<UUID> employeeIds = employees.stream().map(Employee::getId).toList();
        Map<UUID, List<ContractPayItem>> itemsByEmployee = employeeIds.isEmpty()
                ? Map.of()
                : payItemRepo.findByEmployeeIdInOrderByEmployeeIdAscEffectiveFromDesc(employeeIds)
                    .stream().collect(Collectors.groupingBy(ContractPayItem::getEmployeeId));
        Map<UUID, UUID> projectByEmployee = activeProjectByEmployee(companyId, employeeIds, period.getStartDate(), period.getEndDate());

        ProvisionRun run = new ProvisionRun();
        run.setCompanyId(companyId);
        run.setPeriodId(period.getId());
        run.setProjectId(request.getProjectId());
        run.setPayGroup(payGroup);
        run.setProvisionType(provisionType);
        run.setStatus("CALCULATED");
        run.setCalculatedAt(Instant.now());
        run.setNotes("Monthly accrual preview. Formula: eligible monthly amount / 12.");
        run = runRepo.save(run);

        BigDecimal totalEligible = BigDecimal.ZERO;
        BigDecimal totalProvision = BigDecimal.ZERO;
        for (Employee employee : employees) {
            BigDecimal eligible = activeEligibleAmount(itemsByEmployee.getOrDefault(employee.getId(), List.of()),
                    eligibleComponentIds, period.getEndDate());
            BigDecimal provision = eligible.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
            totalEligible = totalEligible.add(eligible);
            totalProvision = totalProvision.add(provision);

            ProvisionResult result = new ProvisionResult();
            result.setCompanyId(companyId);
            result.setRunId(run.getId());
            result.setEmployeeId(employee.getId());
            result.setEmployeeNumber(employee.getEmployeeNumber());
            result.setEmployeeName(employeeName(employee));
            result.setProjectId(request.getProjectId() != null ? request.getProjectId() : projectByEmployee.get(employee.getId()));
            result.setPayGroup(employee.getPayStatus());
            result.setEligibleAmount(scale(eligible));
            result.setProvisionAmount(scale(provision));
            result.setFormulaNote(provisionType + ": eligible monthly amount / 12");
            result.setStatus("OK");
            if (eligible.compareTo(BigDecimal.ZERO) == 0) {
                result.setMessage("No active eligible pay components found.");
            }
            resultRepo.save(result);
        }

        run.setEmployeeCount(employees.size());
        run.setTotalEligibleAmount(scale(totalEligible));
        run.setTotalProvisionAmount(scale(totalProvision));
        run = runRepo.save(run);
        return get(run.getId());
    }

    public void delete(UUID id) {
        UUID companyId = TenantContext.requireCompanyId();
        ProvisionRun run = runRepo.findById(id).filter(r -> companyId.equals(r.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException("Provision run", id));
        runRepo.delete(run);
    }

    private Set<UUID> eligibleComponentIds(UUID companyId, String type) {
        Set<UUID> ids = new HashSet<>();
        for (PayrollComponent component : componentRepo.findByCompanyIdOrderByPriority(companyId)) {
            if (!"ACTIVE".equalsIgnoreCase(component.getStatus())) continue;
            boolean included = switch (type) {
                case "LEAVE" -> component.isLeaveIncluded() || component.isProvisionIncluded();
                case "EOS" -> component.isEosIncluded() || component.isProvisionIncluded();
                default -> component.isProvisionIncluded();
            };
            if (included) ids.add(component.getId());
        }
        return ids;
    }

    private Map<UUID, UUID> activeProjectByEmployee(UUID companyId, List<UUID> employeeIds, LocalDate periodStart, LocalDate periodEnd) {
        Map<UUID, UUID> result = new HashMap<>();
        if (employeeIds.isEmpty()) return result;
        for (Assignment assignment : assignmentRepo.findActiveWithProjectByCompanyIdAndEmployeeIdIn(companyId, employeeIds)) {
            if (result.containsKey(assignment.getEmployeeId())) continue;
            if (assignment.getEffectiveFrom().isAfter(periodEnd)) continue;
            if (assignment.getEffectiveTo() != null && assignment.getEffectiveTo().isBefore(periodStart)) continue;
            result.put(assignment.getEmployeeId(), assignment.getProjectId());
        }
        return result;
    }

    private BigDecimal activeEligibleAmount(List<ContractPayItem> items, Set<UUID> eligibleComponentIds, LocalDate asOf) {
        BigDecimal total = BigDecimal.ZERO;
        Set<UUID> seenComponentIds = new HashSet<>();
        for (ContractPayItem item : items) {
            if (!eligibleComponentIds.contains(item.getPayComponentId())) continue;
            if (seenComponentIds.contains(item.getPayComponentId())) continue;
            if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) continue;
            if (item.getEffectiveFrom().isAfter(asOf)) continue;
            if (item.getEffectiveTo() != null && item.getEffectiveTo().isBefore(asOf)) continue;
            total = total.add(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
            seenComponentIds.add(item.getPayComponentId());
        }
        return total;
    }

    private ProvisionDtos.RunDto toRunDto(ProvisionRun run, PayrollPeriod period, List<ProvisionDtos.ResultDto> results) {
        ProvisionDtos.RunDto dto = new ProvisionDtos.RunDto();
        dto.setId(run.getId());
        dto.setPeriodId(run.getPeriodId());
        if (period != null) {
            dto.setPeriodName(period.getName());
            dto.setPeriodStartDate(period.getStartDate());
            dto.setPeriodEndDate(period.getEndDate());
        }
        dto.setProjectId(run.getProjectId());
        dto.setPayGroup(run.getPayGroup());
        dto.setProvisionType(run.getProvisionType());
        dto.setStatus(run.getStatus());
        dto.setCalculatedAt(run.getCalculatedAt());
        dto.setEmployeeCount(run.getEmployeeCount());
        dto.setTotalEligibleAmount(run.getTotalEligibleAmount());
        dto.setTotalProvisionAmount(run.getTotalProvisionAmount());
        dto.setNotes(run.getNotes());
        dto.setResults(results);
        return dto;
    }

    private ProvisionDtos.ResultDto toResultDto(ProvisionResult result) {
        ProvisionDtos.ResultDto dto = new ProvisionDtos.ResultDto();
        dto.setId(result.getId());
        dto.setEmployeeId(result.getEmployeeId());
        dto.setEmployeeNumber(result.getEmployeeNumber());
        dto.setEmployeeName(result.getEmployeeName());
        dto.setProjectId(result.getProjectId());
        dto.setPayGroup(result.getPayGroup());
        dto.setEligibleAmount(result.getEligibleAmount());
        dto.setProvisionAmount(result.getProvisionAmount());
        dto.setFormulaNote(result.getFormulaNote());
        dto.setStatus(result.getStatus());
        dto.setMessage(result.getMessage());
        return dto;
    }

    private String normalizePayGroup(String value) {
        String v = value == null || value.isBlank() ? "ALL" : value.trim().toUpperCase();
        return v.length() > 30 ? v.substring(0, 30) : v;
    }

    private String normalizeProvisionType(String value) {
        String v = value == null || value.isBlank() ? "LEAVE" : value.trim().toUpperCase();
        return switch (v) {
            case "LEAVE", "EOS", "TICKET", "OTHER" -> v;
            default -> throw new BusinessRuleException("provision.type.invalid", "Unsupported provision type: " + value);
        };
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getMiddleName() == null ? "" : employee.getMiddleName() + " ")
                + employee.getLastName()).trim().replaceAll("\\s+", " ");
    }
}
