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
import com.hrms.payroll.repository.PayrollResultLineRepository;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.payroll.repository.PayrollRunRepository;
import com.hrms.project.domain.CostCode;
import com.hrms.project.domain.Project;
import com.hrms.project.repository.CostCodeRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    public PayrollListingReportDto payrollListing(UUID runId) {
        UUID companyId = TenantContext.requireCompanyId();
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + runId));
        if (!companyId.equals(run.getCompanyId())) {
            throw new ResourceNotFoundException("Payroll run not found: " + runId);
        }
        PayrollPeriod period = periodRepo.findById(run.getPeriodId()).orElse(null);
        PayrollListingReportDto report = new PayrollListingReportDto();
        report.setRunId(run.getId());
        report.setPeriodId(run.getPeriodId());
        report.setProjectId(run.getProjectId());
        report.setPayGroup(run.getPayGroup());
        report.setStatus(run.getStatus());
        if (period != null) {
            report.setPeriodName(period.getName());
            report.setPeriodStartDate(period.getStartDate());
            report.setPeriodEndDate(period.getEndDate());
        }
        if (run.getProjectId() != null) {
            projectRepo.findById(run.getProjectId()).ifPresent(project -> {
                report.setProjectCode(project.getCode());
                report.setProjectName(project.getName());
            });
        }

        Set<String> componentCodes = new LinkedHashSet<>();
        List<PayrollResult> results = resultRepo.findByRunIdOrderByEmployeeId(run.getId());
        for (PayrollResult result : results) {
            PayrollListingReportDto.Row row = buildRow(result, period != null ? period.getEndDate() : null, componentCodes);
            report.getRows().add(row);
            report.setTotalBasic(z(report.getTotalBasic()).add(z(row.getBasic())));
            report.setTotalAllowances(z(report.getTotalAllowances()).add(z(row.getAllowances())));
            report.setTotalOvertime(z(report.getTotalOvertime()).add(z(row.getOvertime())));
            report.setTotalDeductions(z(report.getTotalDeductions()).add(z(row.getDeductions())));
            report.setTotalGross(z(report.getTotalGross()).add(z(row.getGross())));
            report.setTotalNet(z(report.getTotalNet()).add(z(row.getNet())));
        }
        report.setEmployeeCount(report.getRows().size());
        report.setComponentCodes(componentCodes.stream().toList());
        return report;
    }

    private PayrollListingReportDto.Row buildRow(PayrollResult result, LocalDate asOf, Set<String> componentCodes) {
        PayrollListingReportDto.Row row = new PayrollListingReportDto.Row();
        row.setEmployeeId(result.getEmployeeId());
        employeeRepo.findById(result.getEmployeeId()).ifPresent(employee -> applyEmployee(row, employee, asOf));
        row.setPayGroup(result.getPayStatus());
        row.setWorkedDays(z(result.getWorkedDays()));
        row.setNormalHours(z(result.getNormalHours()));
        row.setOtHours(z(result.getOtHours()));
        row.setDeductions(z(result.getTotalDeductions()));
        row.setGross(z(result.getGross()));
        row.setNet(z(result.getNet()));
        row.setStatus(result.getStatus());
        row.setMessage(result.getMessage());

        for (PayrollResultLine line : lineRepo.findByResultIdOrderBySortOrderAsc(result.getId())) {
            BigDecimal amount = z(line.getAmount());
            String code = line.getComponentCode() != null && !line.getComponentCode().isBlank()
                    ? line.getComponentCode()
                    : line.getComponentName();
            if (code != null && !code.isBlank()) {
                componentCodes.add(code);
                row.getComponentAmounts().merge(code, amount, BigDecimal::add);
            }
            if ("DEDUCTION".equalsIgnoreCase(line.getComponentType())) {
                continue;
            }
            String category = line.getCategory() != null ? line.getCategory() : "";
            String name = line.getComponentName() != null ? line.getComponentName() : "";
            String source = line.getSource() != null ? line.getSource() : "";
            if ("SALARY".equalsIgnoreCase(category) || name.toUpperCase().contains("BASIC")) {
                row.setBasic(z(row.getBasic()).add(amount));
            } else if ("OVERTIME".equalsIgnoreCase(category) || source.toUpperCase().contains("OT")) {
                row.setOvertime(z(row.getOvertime()).add(amount));
            } else if ("ALLOWANCE".equalsIgnoreCase(category)) {
                row.setAllowances(z(row.getAllowances()).add(amount));
            }
        }
        return row;
    }

    private void applyEmployee(PayrollListingReportDto.Row row, Employee employee, LocalDate asOf) {
        row.setEmployeeNumber(employee.getEmployeeNumber());
        row.setEmployeeName((z(employee.getFirstName()) + " " + z(employee.getLastName())).trim());
        Assignment assignment = assignment(employee.getId(), asOf);
        if (assignment == null) {
            return;
        }
        row.setProjectId(assignment.getProjectId());
        row.setCostCodeId(assignment.getCostCodeId());
        if (assignment.getProjectId() != null) {
            Project project = projectRepo.findById(assignment.getProjectId()).orElse(null);
            if (project != null) {
                row.setProjectCode(project.getCode());
                row.setProjectName(project.getName());
            }
        }
        if (assignment.getCostCodeId() != null) {
            CostCode costCode = costCodeRepo.findById(assignment.getCostCodeId()).orElse(null);
            if (costCode != null) {
                row.setCostCode(costCode.getCode());
                row.setCostCodeName(costCode.getName());
            }
        }
    }

    private Assignment assignment(UUID employeeId, LocalDate asOf) {
        return assignmentRepo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .filter(a -> asOf == null || a.getEffectiveFrom() == null || !a.getEffectiveFrom().isAfter(asOf))
                .filter(a -> asOf == null || a.getEffectiveTo() == null || !a.getEffectiveTo().isBefore(asOf))
                .findFirst().orElse(null);
    }

    private static BigDecimal z(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static String z(String value) {
        return value != null ? value : "";
    }
}
