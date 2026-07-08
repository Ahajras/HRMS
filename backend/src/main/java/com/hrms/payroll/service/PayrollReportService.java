package com.hrms.payroll.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.AssignmentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.domain.PayrollResult;
import com.hrms.payroll.domain.PayrollResultLine;
import com.hrms.payroll.domain.PayrollRun;
import com.hrms.payroll.dto.PayrollListingReportDto;
import com.hrms.payroll.dto.PayrollListingSummaryDto;
import com.hrms.payroll.repository.PayrollResultLineRepository;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.payroll.repository.PayrollRunRepository;
import com.hrms.project.domain.CostCode;
import com.hrms.project.domain.Project;
import com.hrms.project.repository.CostCodeRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payroll listing report — split into a fast summary (header + totals,
 * regardless of run size) and a paginated, searchable row list, so opening
 * a large run's report doesn't require loading (and N+1-querying) every
 * employee at once. Every lookup here is a batch query, scoped only to the
 * employees actually needed for the call in question.
 */
@Service
@Transactional(readOnly = true)
public class PayrollReportService {

    private final PayrollRunRepository runRepo;
    private final PayrollResultRepository resultRepo;
    private final PayrollResultLineRepository lineRepo;
    private final PayrollPeriodRepository periodRepo;
    private final EmployeeRepository employeeRepo;
    private final AssignmentRepository assignmentRepo;
    private final ProjectRepository projectRepo;
    private final CostCodeRepository costCodeRepo;

    public PayrollReportService(PayrollRunRepository runRepo,
                                PayrollResultRepository resultRepo,
                                PayrollResultLineRepository lineRepo,
                                PayrollPeriodRepository periodRepo,
                                EmployeeRepository employeeRepo,
                                AssignmentRepository assignmentRepo,
                                ProjectRepository projectRepo,
                                CostCodeRepository costCodeRepo) {
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.lineRepo = lineRepo;
        this.periodRepo = periodRepo;
        this.employeeRepo = employeeRepo;
        this.assignmentRepo = assignmentRepo;
        this.projectRepo = projectRepo;
        this.costCodeRepo = costCodeRepo;
    }

    private PayrollRun requireRun(UUID runId) {
        UUID companyId = TenantContext.requireCompanyId();
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + runId));
        if (!companyId.equals(run.getCompanyId())) {
            throw new ResourceNotFoundException("Payroll run not found: " + runId);
        }
        return run;
    }

    /** Fast path — header + totals only. Still scans every result+line once
     * to total correctly, but skips employee/assignment/project lookups
     * entirely (not needed for totals), so this stays quick even for
     * thousands of employees. */
    public PayrollListingSummaryDto summary(UUID runId) {
        PayrollRun run = requireRun(runId);
        PayrollPeriod period = periodRepo.findById(run.getPeriodId()).orElse(null);
        PayrollListingSummaryDto dto = new PayrollListingSummaryDto();
        dto.setRunId(run.getId());
        dto.setPeriodId(run.getPeriodId());
        dto.setProjectId(run.getProjectId());
        dto.setPayGroup(run.getPayGroup());
        dto.setStatus(run.getStatus());
        if (period != null) {
            dto.setPeriodName(period.getName());
            dto.setPeriodStartDate(period.getStartDate());
            dto.setPeriodEndDate(period.getEndDate());
        }
        if (run.getProjectId() != null) {
            projectRepo.findById(run.getProjectId()).ifPresent(project -> {
                dto.setProjectCode(project.getCode());
                dto.setProjectName(project.getName());
            });
        }

        List<PayrollResult> results = resultRepo.findByRunIdOrderByEmployeeId(runId);
        List<UUID> resultIds = results.stream().map(PayrollResult::getId).toList();
        Map<UUID, List<PayrollResultLine>> linesByResult = lineRepo.findByResultIdInOrderBySortOrderAsc(resultIds)
                .stream().collect(Collectors.groupingBy(PayrollResultLine::getResultId));

        Set<String> componentCodes = new LinkedHashSet<>();
        for (PayrollResult result : results) {
            ComponentTotals t = componentTotals(linesByResult.getOrDefault(result.getId(), List.of()), componentCodes);
            dto.setTotalBasic(z(dto.getTotalBasic()).add(t.basic));
            dto.setTotalAllowances(z(dto.getTotalAllowances()).add(t.allowances));
            dto.setTotalOvertime(z(dto.getTotalOvertime()).add(t.overtime));
            dto.setTotalDeductions(z(dto.getTotalDeductions()).add(z(result.getTotalDeductions())));
            dto.setTotalGross(z(dto.getTotalGross()).add(z(result.getGross())));
            dto.setTotalNet(z(dto.getTotalNet()).add(z(result.getNet())));
        }
        dto.setEmployeeCount(results.size());
        dto.setComponentCodes(componentCodes.stream().toList());
        return dto;
    }

    /** Paginated, searchable rows — only fetches employees/lines/assignments
     * for the employees on THIS page (25 by default), not the whole run. */
    public com.hrms.common.web.PageResponse<PayrollListingReportDto.Row> pagedRows(
            UUID runId, int page, int size, String search) {
        PayrollRun run = requireRun(runId);
        PayrollPeriod period = periodRepo.findById(run.getPeriodId()).orElse(null);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));

        Page<PayrollResult> resultsPage;
        if (search != null && !search.isBlank()) {
            List<UUID> matchingIds = employeeRepo
                    .search(run.getCompanyId(), search.trim(), Pageable.unpaged())
                    .stream().map(Employee::getId).toList();
            resultsPage = matchingIds.isEmpty()
                    ? Page.empty(pageable)
                    : resultRepo.findByRunIdAndEmployeeIdIn(runId, matchingIds, pageable);
        } else {
            resultsPage = resultRepo.findByRunId(runId, pageable);
        }

        List<PayrollResult> pageResults = resultsPage.getContent();
        List<UUID> employeeIds = pageResults.stream().map(PayrollResult::getEmployeeId).toList();
        List<UUID> resultIds = pageResults.stream().map(PayrollResult::getId).toList();

        Map<UUID, Employee> employeeById = employeeRepo.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        Map<UUID, List<PayrollResultLine>> linesByResult = lineRepo.findByResultIdInOrderBySortOrderAsc(resultIds)
                .stream().collect(Collectors.groupingBy(PayrollResultLine::getResultId));

        LocalDate asOf = period != null ? period.getEndDate() : null;
        Map<UUID, Assignment> assignmentByEmployee = assignmentRepo
                .findActiveWithProjectByCompanyIdAndEmployeeIdIn(run.getCompanyId(), employeeIds)
                .stream()
                .filter(a -> asOf == null || a.getEffectiveFrom() == null || !a.getEffectiveFrom().isAfter(asOf))
                .filter(a -> asOf == null || a.getEffectiveTo() == null || !a.getEffectiveTo().isBefore(asOf))
                .collect(Collectors.toMap(Assignment::getEmployeeId, a -> a, (a, b) -> a));

        Set<UUID> projectIds = assignmentByEmployee.values().stream().map(Assignment::getProjectId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> costCodeIds = assignmentByEmployee.values().stream().map(Assignment::getCostCodeId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, Project> projectById = projectIds.isEmpty() ? Map.of() : projectRepo.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, p -> p, (a, b) -> a));
        Map<UUID, CostCode> costCodeById = costCodeIds.isEmpty() ? Map.of() : costCodeRepo.findAllById(costCodeIds).stream()
                .collect(Collectors.toMap(CostCode::getId, c -> c, (a, b) -> a));

        List<PayrollListingReportDto.Row> rows = pageResults.stream()
                .map(result -> buildRow(result, linesByResult.getOrDefault(result.getId(), List.of()),
                        employeeById.get(result.getEmployeeId()), assignmentByEmployee.get(result.getEmployeeId()),
                        projectById, costCodeById))
                .toList();

        return new com.hrms.common.web.PageResponse<>(rows, resultsPage.getNumber(), resultsPage.getSize(),
                resultsPage.getTotalElements(), resultsPage.getTotalPages(), resultsPage.isFirst(), resultsPage.isLast());
    }

    private static final class ComponentTotals {
        final BigDecimal basic;
        final BigDecimal allowances;
        final BigDecimal overtime;
        ComponentTotals(BigDecimal basic, BigDecimal allowances, BigDecimal overtime) {
            this.basic = basic; this.allowances = allowances; this.overtime = overtime;
        }
    }

    private ComponentTotals componentTotals(List<PayrollResultLine> lines, Set<String> componentCodes) {
        BigDecimal basic = BigDecimal.ZERO, allowances = BigDecimal.ZERO, overtime = BigDecimal.ZERO;
        for (PayrollResultLine line : lines) {
            BigDecimal amount = z(line.getAmount());
            String code = line.getComponentCode() != null && !line.getComponentCode().isBlank()
                    ? line.getComponentCode() : line.getComponentName();
            if (code != null && !code.isBlank() && componentCodes != null) {
                componentCodes.add(code);
            }
            if ("DEDUCTION".equalsIgnoreCase(line.getComponentType())) {
                continue;
            }
            String category = line.getCategory() != null ? line.getCategory() : "";
            String name = line.getComponentName() != null ? line.getComponentName() : "";
            String source = line.getSource() != null ? line.getSource() : "";
            if ("SALARY".equalsIgnoreCase(category) || name.toUpperCase().contains("BASIC")) {
                basic = basic.add(amount);
            } else if ("OVERTIME".equalsIgnoreCase(category) || source.toUpperCase().contains("OT")) {
                overtime = overtime.add(amount);
            } else if ("ALLOWANCE".equalsIgnoreCase(category)) {
                allowances = allowances.add(amount);
            }
        }
        return new ComponentTotals(basic, allowances, overtime);
    }

    private PayrollListingReportDto.Row buildRow(PayrollResult result, List<PayrollResultLine> lines,
                                                 Employee employee, Assignment assignment,
                                                 Map<UUID, Project> projectById, Map<UUID, CostCode> costCodeById) {
        PayrollListingReportDto.Row row = new PayrollListingReportDto.Row();
        row.setEmployeeId(result.getEmployeeId());
        if (employee != null) {
            row.setEmployeeNumber(employee.getEmployeeNumber());
            row.setEmployeeName((z(employee.getFirstName()) + " " + z(employee.getLastName())).trim());
        }
        row.setPayGroup(result.getPayStatus());
        row.setWorkedDays(z(result.getWorkedDays()));
        row.setNormalHours(z(result.getNormalHours()));
        row.setOtHours(z(result.getOtHours()));
        row.setDeductions(z(result.getTotalDeductions()));
        row.setGross(z(result.getGross()));
        row.setNet(z(result.getNet()));
        row.setStatus(result.getStatus());
        row.setMessage(result.getMessage());

        if (assignment != null) {
            row.setProjectId(assignment.getProjectId());
            row.setCostCodeId(assignment.getCostCodeId());
            Project p = assignment.getProjectId() != null ? projectById.get(assignment.getProjectId()) : null;
            if (p != null) {
                row.setProjectCode(p.getCode());
                row.setProjectName(p.getName());
            }
            CostCode c = assignment.getCostCodeId() != null ? costCodeById.get(assignment.getCostCodeId()) : null;
            if (c != null) {
                row.setCostCode(c.getCode());
                row.setCostCodeName(c.getName());
            }
        }

        Map<String, BigDecimal> componentAmounts = new LinkedHashMap<>();
        for (PayrollResultLine line : lines) {
            BigDecimal amount = z(line.getAmount());
            String code = line.getComponentCode() != null && !line.getComponentCode().isBlank()
                    ? line.getComponentCode() : line.getComponentName();
            if (code != null && !code.isBlank()) {
                componentAmounts.merge(code, amount, BigDecimal::add);
            }
        }
        ComponentTotals t = componentTotals(lines, null);
        row.setBasic(t.basic);
        row.setAllowances(t.allowances);
        row.setOvertime(t.overtime);
        row.setComponentAmounts(componentAmounts);
        return row;
    }

    private static BigDecimal z(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String z(String value) {
        return value != null ? value : "";
    }
}
