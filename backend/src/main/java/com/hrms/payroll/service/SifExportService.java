package com.hrms.payroll.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.domain.EmployeeBankAccount;
import com.hrms.employee.domain.EmployeeDocument;
import com.hrms.employee.repository.AssignmentRepository;
import com.hrms.employee.repository.EmployeeBankAccountRepository;
import com.hrms.employee.repository.EmployeeDocumentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.domain.PayrollResult;
import com.hrms.payroll.domain.PayrollResultLine;
import com.hrms.payroll.domain.PayrollRun;
import com.hrms.payroll.dto.SifExportDtos.SifExclusion;
import com.hrms.payroll.dto.SifExportDtos.SifExportResult;
import com.hrms.payroll.dto.SifExportDtos.SifFile;
import com.hrms.payroll.dto.SifExportDtos.SifRow;
import com.hrms.payroll.repository.PayrollResultLineRepository;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.payroll.repository.PayrollRunRepository;
import com.hrms.project.domain.Project;
import com.hrms.project.domain.Sponsor;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.project.repository.SponsorRepository;
import com.hrms.reference.domain.Bank;
import com.hrms.reference.repository.BankRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Generates Qatar WPS Salary Information Files (SIF) — one CSV per
 * Sponsor (Employer Establishment), grouping whichever projects the
 * caller selected. Cash-paid employees are excluded automatically; a
 * BANK-paid employee missing a bank account or QID/Visa is excluded too,
 * but reported as an exclusion so it can be fixed rather than silently
 * dropped.
 */
@Service
@Transactional(readOnly = true)
public class SifExportService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    private final AssignmentRepository assignmentRepo;
    private final EmployeeRepository employeeRepo;
    private final EmployeeBankAccountRepository bankAccountRepo;
    private final EmployeeDocumentRepository documentRepo;
    private final SponsorRepository sponsorRepo;
    private final ProjectRepository projectRepo;
    private final BankRepository bankRepo;
    private final PayrollPeriodRepository periodRepo;
    private final PayrollResultRepository resultRepo;
    private final PayrollResultLineRepository lineRepo;
    private final PayrollRunRepository runRepo;

    public SifExportService(AssignmentRepository assignmentRepo, EmployeeRepository employeeRepo,
                            EmployeeBankAccountRepository bankAccountRepo, EmployeeDocumentRepository documentRepo,
                            SponsorRepository sponsorRepo, ProjectRepository projectRepo, BankRepository bankRepo,
                            PayrollPeriodRepository periodRepo, PayrollResultRepository resultRepo,
                            PayrollResultLineRepository lineRepo, PayrollRunRepository runRepo) {
        this.assignmentRepo = assignmentRepo;
        this.employeeRepo = employeeRepo;
        this.bankAccountRepo = bankAccountRepo;
        this.documentRepo = documentRepo;
        this.sponsorRepo = sponsorRepo;
        this.projectRepo = projectRepo;
        this.bankRepo = bankRepo;
        this.periodRepo = periodRepo;
        this.resultRepo = resultRepo;
        this.lineRepo = lineRepo;
        this.runRepo = runRepo;
    }

    /** @param projectIds empty/null means "all projects"
     *  @param allowUnlocked true for the manager-preview permission; false
     *         means every underlying payroll run must be LOCKED, or the
     *         whole export is refused (not partially generated). */
    public SifExportResult generate(UUID periodId, Collection<UUID> projectIds, boolean allowUnlocked) {
        UUID companyId = TenantContext.requireCompanyId();
        PayrollPeriod period = periodRepo.findById(periodId)
                .orElseThrow(() -> new BusinessRuleException("sif.period_not_found", "Period not found: " + periodId));

        Map<UUID, Sponsor> sponsorsById = sponsorRepo.findByCompanyIdOrderByCode(companyId).stream()
                .collect(Collectors.toMap(Sponsor::getId, s -> s));
        Map<UUID, Bank> banksById = bankRepo.findAll().stream()
                .collect(Collectors.toMap(Bank::getId, b -> b, (a, b) -> a));

        boolean allProjects = projectIds == null || projectIds.isEmpty();
        List<Assignment> assignments = assignmentRepo.findActiveWithProjectByCompanyId(companyId).stream()
                .filter(a -> allProjects || projectIds.contains(a.getProjectId()))
                .toList();

        // One assignment per employee (the query already orders most-recent-first).
        Map<UUID, Assignment> assignmentByEmployee = new HashMap<>();
        for (Assignment a : assignments) {
            assignmentByEmployee.putIfAbsent(a.getEmployeeId(), a);
        }

        Set<UUID> projectIdsInScope = assignmentByEmployee.values().stream()
                .map(Assignment::getProjectId).collect(Collectors.toSet());
        Map<UUID, Project> projectsById = projectRepo.findAllById(projectIdsInScope).stream()
                .collect(Collectors.toMap(Project::getId, p -> p));

        Set<UUID> employeeIds = assignmentByEmployee.keySet();
        Map<UUID, Employee> employeesById = employeeRepo.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        SifExportResult result = new SifExportResult();
        Map<String, SifFile> filesBySponsorCode = new HashMap<>();
        Map<String, Integer> sequenceBySponsor = new HashMap<>();

        for (UUID employeeId : employeeIds) {
            Employee employee = employeesById.get(employeeId);
            if (employee == null) {
                continue;
            }
            if (!"BANK".equalsIgnoreCase(employee.getPaymentMethodCode())) {
                continue; // cash-paid — not part of WPS at all, not an error
            }

            List<PayrollResult> resultsForEmployee = resultRepo.findByPeriodIdAndEmployeeIdOrderByCreatedAtDesc(periodId, employeeId);
            if (resultsForEmployee.isEmpty()) {
                continue; // no payroll calculated for them this period
            }
            PayrollResult payrollResult = resultsForEmployee.get(0);
            PayrollRun run = runRepo.findById(payrollResult.getRunId()).orElse(null);
            if (run == null) {
                continue;
            }
            if (!allowUnlocked && !"LOCKED".equalsIgnoreCase(run.getStatus())) {
                throw new BusinessRuleException("sif.run_not_locked",
                        "Payroll for " + period.getPeriodYear() + "-" + period.getPeriodMonth()
                                + " is not fully locked yet — SIF can only be generated once every run for this period is LOCKED.");
            }

            Assignment assignment = assignmentByEmployee.get(employeeId);
            Project project = assignment.getProjectId() != null ? projectsById.get(assignment.getProjectId()) : null;
            Sponsor sponsor = project != null && project.getSponsorId() != null ? sponsorsById.get(project.getSponsorId()) : null;
            if (sponsor == null) {
                result.getExclusions().add(new SifExclusion(employee.getEmployeeNumber(),
                        fullName(employee), "Project has no Sponsor (Employer Establishment) configured"));
                continue;
            }

            List<EmployeeBankAccount> accounts = bankAccountRepo.findByEmployeeIdOrderByPrimaryDesc(employeeId).stream()
                    .filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus())).toList();
            if (accounts.isEmpty()) {
                result.getExclusions().add(new SifExclusion(employee.getEmployeeNumber(), fullName(employee),
                        "Payment method is BANK but no active bank account is on file"));
                continue;
            }
            EmployeeBankAccount account = accounts.get(0);
            Bank employeeBank = account.getBankId() != null ? banksById.get(account.getBankId()) : null;
            if (employeeBank == null) {
                result.getExclusions().add(new SifExclusion(employee.getEmployeeNumber(), fullName(employee),
                        "Bank account on file has no bank selected"));
                continue;
            }

            List<EmployeeDocument> docs = documentRepo.findByEmployeeIdOrderByDocumentType(employeeId);
            String qid = docs.stream().filter(d -> "RESIDENCE_ID".equalsIgnoreCase(d.getDocumentType()))
                    .map(EmployeeDocument::getDocumentNumber).findFirst().orElse(null);
            String visa = qid == null ? docs.stream().filter(d -> "VISA".equalsIgnoreCase(d.getDocumentType()))
                    .map(EmployeeDocument::getDocumentNumber).findFirst().orElse(null) : null;
            if (qid == null && visa == null) {
                result.getExclusions().add(new SifExclusion(employee.getEmployeeNumber(), fullName(employee),
                        "Payment method is BANK but no QID or Visa number is on file"));
                continue;
            }

            List<PayrollResultLine> lines = lineRepo.findByResultIdOrderBySortOrderAsc(payrollResult.getId());
            BigDecimal basic = lines.stream()
                    .filter(l -> "SALARY".equalsIgnoreCase(l.getCategory()) && "EARNING".equalsIgnoreCase(l.getComponentType()))
                    .map(PayrollResultLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gross = payrollResult.getGross() != null ? payrollResult.getGross() : BigDecimal.ZERO;
            BigDecimal deductions = payrollResult.getTotalDeductions() != null ? payrollResult.getTotalDeductions() : BigDecimal.ZERO;
            BigDecimal net = payrollResult.getNet() != null ? payrollResult.getNet() : BigDecimal.ZERO;
            BigDecimal extraIncome = gross.subtract(basic);
            if (extraIncome.signum() < 0) {
                extraIncome = BigDecimal.ZERO;
            }

            SifFile file = filesBySponsorCode.computeIfAbsent(sponsor.getCode(), code -> {
                SifFile f = new SifFile();
                f.setSponsorCode(sponsor.getCode());
                f.setSponsorName(sponsor.getName());
                f.setEstablishmentEid(sponsor.getEstablishmentEid());
                f.setPayerBankCode(sponsor.getPayerBankCode());
                f.setPayerIban(sponsor.getPayerIban());
                f.setPayerQid(sponsor.getPayerQid());
                f.setPeriodYear(period.getPeriodYear());
                f.setPeriodMonth(period.getPeriodMonth());
                Instant now = Instant.now();
                f.setFileName("SIF_" + sponsor.getEstablishmentEid() + "_" + sponsor.getPayerBankCode() + "_"
                        + DATE_FMT.format(now.atZone(ZoneOffset.UTC)) + "_" + TIME_FMT.format(now.atZone(ZoneOffset.UTC)));
                return f;
            });

            int seq = sequenceBySponsor.merge(sponsor.getCode(), 1, Integer::sum);
            SifRow row = new SifRow();
            row.setRecordSequence(seq);
            row.setEmployeeId(employee.getId().toString());
            row.setEmployeeNumber(employee.getEmployeeNumber());
            row.setQid(qid);
            row.setVisaId(visa);
            row.setEmployeeName(fullName(employee));
            row.setBankCode(employeeBank.getCode());
            row.setBankAccount(account.getIban() != null && !account.getIban().isBlank() ? account.getIban() : account.getAccountNumber());
            row.setWorkingDays(payrollResult.getWorkedDays() != null ? payrollResult.getWorkedDays().intValue() : 0);
            row.setNetSalary(net.longValue());
            row.setBasicSalary(basic.longValue());
            row.setExtraHours(payrollResult.getOtHours() != null ? payrollResult.getOtHours() : BigDecimal.ZERO);
            row.setExtraIncome(extraIncome.longValue());
            row.setDeductions(deductions.longValue());
            row.setNotes(autoNote(net, deductions, extraIncome));
            row.setProjectCode(project != null ? project.getCode() : null);
            file.getRows().add(row);
            file.setTotalSalaries(file.getTotalSalaries().add(net));
            file.setTotalRecords(file.getTotalRecords() + 1);
        }

        result.getFiles().addAll(filesBySponsorCode.values());
        return result;
    }

    /** "On Leave" if nothing was earned at all, "Personal Loan" if there
     * were deductions, "Allowances" if there was extra income on top of
     * basic, otherwise blank — matching exactly how this bank has always
     * classified the Notes/Comments column. This is a starting suggestion;
     * it can be edited before the file is finalized. */
    private String autoNote(BigDecimal net, BigDecimal deductions, BigDecimal extraIncome) {
        if (net.signum() == 0) {
            return "On Leave";
        }
        if (deductions.signum() > 0) {
            return "Personal Loan";
        }
        if (extraIncome.signum() > 0) {
            return "Allowances";
        }
        return "";
    }

    private String fullName(Employee e) {
        StringBuilder sb = new StringBuilder(e.getFirstName());
        if (e.getMiddleName() != null && !e.getMiddleName().isBlank()) {
            sb.append(' ').append(e.getMiddleName());
        }
        sb.append(' ').append(e.getLastName());
        return sb.toString();
    }
}
