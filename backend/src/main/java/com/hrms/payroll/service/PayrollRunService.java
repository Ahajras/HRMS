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
import com.hrms.payroll.domain.PayrollCategoryRule;
import com.hrms.payroll.domain.PayrollComponent;
import com.hrms.payroll.domain.PayrollResult;
import com.hrms.payroll.domain.PayrollResultLine;
import com.hrms.payroll.domain.PayrollRule;
import com.hrms.payroll.domain.PayrollRun;
import com.hrms.payroll.dto.PayrollResultDto;
import com.hrms.payroll.dto.PayrollResultLineDto;
import com.hrms.payroll.dto.PayrollRunDto;
import com.hrms.payroll.repository.PayrollComponentRepository;
import com.hrms.payroll.repository.PayrollCategoryRuleRepository;
import com.hrms.payroll.repository.PayrollResultLineRepository;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.payroll.repository.PayrollRuleRepository;
import com.hrms.payroll.repository.PayrollRunRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.domain.PayrollPeriodProject;
import com.hrms.timesheet.domain.Shift;
import com.hrms.timesheet.domain.TimeType;
import com.hrms.timesheet.domain.Timesheet;
import com.hrms.timesheet.domain.TimesheetDay;
import com.hrms.timesheet.domain.TimeTypePayrollRule;
import com.hrms.timesheet.repository.PayrollPeriodProjectRepository;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import com.hrms.timesheet.repository.ShiftRepository;
import com.hrms.timesheet.repository.TimeTypeRepository;
import com.hrms.timesheet.repository.TimeTypePayrollRuleRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import com.hrms.timesheet.repository.TimesheetRepository;
import com.hrms.timesheet.service.TimesheetService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PayrollRunService {

    private static final String DRAFT = "DRAFT";
    private static final String CALCULATED = "CALCULATED";

    private final PayrollRunRepository runRepo;
    private final PayrollResultRepository resultRepo;
    private final PayrollResultLineRepository lineRepo;
    private final PayrollPeriodRepository periodRepo;
    private final PayrollPeriodProjectRepository periodProjectRepo;
    private final TimesheetRepository timesheetRepo;
    private final TimesheetDayRepository dayRepo;
    private final ShiftRepository shiftRepo;
    private final TimeTypeRepository timeTypeRepo;
    private final TimeTypePayrollRuleRepository timeTypePayrollRuleRepo;
    private final EmployeeRepository employeeRepo;
    private final AssignmentRepository assignmentRepo;
    private final ContractPayItemRepository payItemRepo;
    private final PayrollComponentRepository componentRepo;
    private final PayrollCategoryRuleRepository categoryRuleRepo;
    private final PayrollRuleRepository ruleRepo;

    public PayrollRunService(PayrollRunRepository runRepo,
                             PayrollResultRepository resultRepo,
                             PayrollResultLineRepository lineRepo,
                             PayrollPeriodRepository periodRepo,
                             PayrollPeriodProjectRepository periodProjectRepo,
                             TimesheetRepository timesheetRepo,
                             TimesheetDayRepository dayRepo,
                             ShiftRepository shiftRepo,
                             TimeTypeRepository timeTypeRepo,
                             TimeTypePayrollRuleRepository timeTypePayrollRuleRepo,
                             EmployeeRepository employeeRepo,
                             AssignmentRepository assignmentRepo,
                             ContractPayItemRepository payItemRepo,
                             PayrollComponentRepository componentRepo,
                             PayrollCategoryRuleRepository categoryRuleRepo,
                             PayrollRuleRepository ruleRepo) {
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.lineRepo = lineRepo;
        this.periodRepo = periodRepo;
        this.periodProjectRepo = periodProjectRepo;
        this.timesheetRepo = timesheetRepo;
        this.dayRepo = dayRepo;
        this.shiftRepo = shiftRepo;
        this.timeTypeRepo = timeTypeRepo;
        this.timeTypePayrollRuleRepo = timeTypePayrollRuleRepo;
        this.employeeRepo = employeeRepo;
        this.assignmentRepo = assignmentRepo;
        this.payItemRepo = payItemRepo;
        this.componentRepo = componentRepo;
        this.categoryRuleRepo = categoryRuleRepo;
        this.ruleRepo = ruleRepo;
    }

    private PayrollCalculationContext payrollCalculationContext(PayrollRun run, PayrollPeriod period, List<Timesheet> timesheets) {
        Set<UUID> employeeIds = new HashSet<>();
        Set<UUID> timesheetIds = new HashSet<>();
        Set<UUID> shiftIds = new HashSet<>();
        for (Timesheet ts : timesheets) {
            employeeIds.add(ts.getEmployeeId());
            timesheetIds.add(ts.getId());
            if (ts.getShiftId() != null) {
                shiftIds.add(ts.getShiftId());
            }
        }

        Map<UUID, Employee> employees = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            employeeRepo.findAllById(employeeIds).forEach(e -> {
                if (run.getCompanyId().equals(e.getCompanyId())) {
                    employees.put(e.getId(), e);
                }
            });
        }

        Map<UUID, UUID> employeeProjects = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            for (Assignment a : assignmentRepo.findActiveWithProjectByCompanyIdAndEmployeeIdIn(run.getCompanyId(), employeeIds)) {
                employeeProjects.putIfAbsent(a.getEmployeeId(), a.getProjectId());
            }
        }

        Map<UUID, List<ContractPayItem>> payItemsByEmployee = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            for (ContractPayItem item : payItemRepo.findByEmployeeIdInOrderByEmployeeIdAscEffectiveFromDesc(employeeIds)) {
                payItemsByEmployee.computeIfAbsent(item.getEmployeeId(), ignored -> new ArrayList<>()).add(item);
            }
        }

        Map<UUID, List<TimesheetDay>> daysByTimesheet = new HashMap<>();
        Set<UUID> timeTypeIds = new HashSet<>();
        if (!timesheetIds.isEmpty()) {
            for (TimesheetDay day : dayRepo.findByTimesheetIdInOrderByTimesheetIdAscWorkDateAsc(timesheetIds)) {
                daysByTimesheet.computeIfAbsent(day.getTimesheetId(), ignored -> new ArrayList<>()).add(day);
                if (day.getTimeTypeId() != null) {
                    timeTypeIds.add(day.getTimeTypeId());
                }
                if (day.getShiftId() != null) {
                    shiftIds.add(day.getShiftId());
                }
            }
        }

        Map<UUID, Shift> shifts = new HashMap<>();
        if (!shiftIds.isEmpty()) {
            shiftRepo.findAllById(shiftIds).forEach(s -> shifts.put(s.getId(), s));
        }

        Map<UUID, TimeType> timeTypes = new HashMap<>();
        if (!timeTypeIds.isEmpty()) {
            timeTypeRepo.findAllById(timeTypeIds).forEach(t -> timeTypes.put(t.getId(), t));
        }

        Map<UUID, Map<UUID, TimeTypePayrollRule>> rulesByTimeType = new HashMap<>();
        Set<UUID> ruleIds = new HashSet<>();
        if (!timeTypeIds.isEmpty()) {
            for (TimeTypePayrollRule ruleRow : timeTypePayrollRuleRepo.findByCompanyIdAndTimeTypeIdIn(run.getCompanyId(), timeTypeIds)) {
                rulesByTimeType.computeIfAbsent(ruleRow.getTimeTypeId(), ignored -> new HashMap<>())
                        .put(ruleRow.getPayrollComponentId(), ruleRow);
            }
        }

        Map<PayrollRuleKey, PayrollRule> payrollRules = new HashMap<>();
        for (PayrollRule rule : ruleRepo.findByCompanyIdOrderByPayGroup(run.getCompanyId())) {
            if (!"ACTIVE".equalsIgnoreCase(rule.getStatus())) {
                continue;
            }
            payrollRules.put(new PayrollRuleKey(rule.getProjectId(), normalizePayGroup(rule.getPayGroup())), rule);
            if (rule.getId() != null) {
                ruleIds.add(rule.getId());
            }
        }

        Map<UUID, Map<String, CategoryPolicy>> categoryPolicies = new HashMap<>();
        if (!ruleIds.isEmpty()) {
            for (PayrollCategoryRule row : categoryRuleRepo.findByPayrollRuleIdInAndStatus(ruleIds, "ACTIVE")) {
                categoryPolicies.computeIfAbsent(row.getPayrollRuleId(), ignored -> new HashMap<>())
                        .put(normalizeCategory(row.getCategory()),
                                new CategoryPolicy(normalizeCategory(row.getCategory()), normalizeCategoryBasis(row.getBasis()),
                                        normalizeCategoryDivisorMode(row.getDivisorMode()), row.getMonthDivisor()));
            }
        }

        Map<UUID, PayrollComponent> components = new HashMap<>();
        componentRepo.findByCompanyIdOrderByPriority(run.getCompanyId()).forEach(c -> components.put(c.getId(), c));

        Map<AnnualUsageKey, Integer> priorAnnualCounts = priorAnnualCounts(run.getCompanyId(), period, employees, timeTypeIds, rulesByTimeType);

        return new PayrollCalculationContext(employees, employeeProjects, payItemsByEmployee, daysByTimesheet,
                shifts, timeTypes, rulesByTimeType, payrollRules, categoryPolicies, components, priorAnnualCounts);
    }

    @Transactional(readOnly = true)
    public List<PayrollRunDto> list(UUID periodId) {
        UUID companyId = TenantContext.requireCompanyId();
        List<PayrollRun> runs = periodId != null
                ? runRepo.findByCompanyIdAndPeriodIdOrderByCreatedAtDesc(companyId, periodId)
                : runRepo.findByCompanyIdOrderByCreatedAtDesc(companyId);
        return runs.stream().map(r -> toDto(r, false)).toList();
    }

    @Transactional(readOnly = true)
    public PayrollRunDto get(UUID id) {
        return toDto(getEntity(id), false);
    }

    /** Paginated, searchable results for the run detail screen — avoids
     * loading thousands of rows at once (the whole run's results used to
     * come back in a single response, which made large runs very slow to
     * open). */
    @Transactional(readOnly = true)
    public com.hrms.common.web.PageResponse<PayrollResultDto> pagedResults(UUID id, int page, int size, String search) {
        PayrollRun run = getEntity(id);
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        org.springframework.data.domain.Page<PayrollResult> resultsPage;
        if (search != null && !search.isBlank()) {
            List<UUID> matchingEmployeeIds = employeeRepo
                    .search(run.getCompanyId(), search.trim(), org.springframework.data.domain.Pageable.unpaged())
                    .stream().map(Employee::getId).toList();
            resultsPage = matchingEmployeeIds.isEmpty()
                    ? org.springframework.data.domain.Page.empty(pageable)
                    : resultRepo.findByRunIdAndEmployeeIdIn(id, matchingEmployeeIds, pageable);
        } else {
            resultsPage = resultRepo.findByRunId(id, pageable);
        }
        // Batch-fetch employees for THIS PAGE only (25-200 rows), not one lookup per row.
        List<UUID> pageEmployeeIds = resultsPage.getContent().stream().map(PayrollResult::getEmployeeId).toList();
        Map<UUID, Employee> employeeById = employeeRepo.findAllById(pageEmployeeIds).stream()
                .collect(java.util.stream.Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        List<PayrollResultDto> dtos = resultsPage.getContent().stream()
                .map(r -> toDto(r, employeeById.get(r.getEmployeeId())))
                .toList();
        return new com.hrms.common.web.PageResponse<>(dtos, resultsPage.getNumber(), resultsPage.getSize(),
                resultsPage.getTotalElements(), resultsPage.getTotalPages(), resultsPage.isFirst(), resultsPage.isLast());
    }

    public PayrollRunDto create(UUID periodId, UUID projectId, String payGroup) {
        UUID companyId = TenantContext.requireCompanyId();
        periodRepo.findById(periodId).orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodId));
        String normalizedPayGroup = normalizePayGroup(payGroup);
        List<PayrollRun> existing = runRepo.findExistingScope(companyId, periodId, projectId, normalizedPayGroup);
        if (!existing.isEmpty()) {
            return toDto(existing.get(0), false);
        }
        PayrollRun run = new PayrollRun();
        run.setCompanyId(companyId);
        run.setPeriodId(periodId);
        run.setProjectId(projectId);
        run.setPayGroup(normalizedPayGroup);
        run.setRunType("REGULAR");
        run.setStatus(DRAFT);
        return toDto(runRepo.save(run), false);
    }

    public PayrollRunDto calculate(UUID id) {
        Map<String, Object> ignored = calculate(id, null);
        return get(id);
    }

    /** Day Zero — recomputes what an employee's NET pay would have been for
     * an already-calculated period if the given days had different time
     * types, using the exact same building blocks (payableBreakdown,
     * buildLines, buildOvertimeLines, buildMonthlyUnpaidDeductionLine) the
     * real calculation uses. Nothing is persisted — this is a pure "what-if"
     * used only to compute the size of a prior-period correction. Returns
     * null if there is nothing to compare against (no original run, or no
     * timesheet for that period). */
    /** Day Zero — the actual, already-paid result for an employee in a
     * prior run, used as the "before" side of the correction diff. */
    @Transactional(readOnly = true)
    public Optional<PayrollResult> findResultForEmployee(UUID runId, UUID employeeId) {
        return resultRepo.findByRunIdAndEmployeeId(runId, employeeId);
    }

    public BigDecimal simulateNetWithOverride(UUID originalRunId, UUID employeeId, Map<UUID, UUID> dayTimeTypeOverrides) {
        PayrollRun run = runRepo.findById(originalRunId).orElse(null);
        if (run == null) {
            return null;
        }
        PayrollPeriod period = periodRepo.findById(run.getPeriodId()).orElse(null);
        if (period == null) {
            return null;
        }
        Timesheet ts = timesheetRepo.findByCompanyIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                run.getCompanyId(), employeeId, period.getPeriodYear(), period.getPeriodMonth()).orElse(null);
        if (ts == null) {
            return null;
        }
        Employee emp = employeeRepo.findById(employeeId).orElse(null);
        if (emp == null) {
            return null;
        }

        List<TimesheetDay> realDays = dayRepo.findByTimesheetIdOrderByWorkDate(ts.getId());
        List<TimesheetDay> simulatedDays = new ArrayList<>();
        for (TimesheetDay d : realDays) {
            UUID overrideType = dayTimeTypeOverrides.get(d.getId());
            if (overrideType == null) {
                simulatedDays.add(d);
                continue;
            }
            TimesheetDay copy = new TimesheetDay();
            copy.setTimesheetId(d.getTimesheetId());
            copy.setWorkDate(d.getWorkDate());
            copy.setShiftId(d.getShiftId());
            copy.setTimeTypeId(overrideType);
            copy.setPlannedHours(d.getPlannedHours());
            copy.setWorkedHours(BigDecimal.ZERO);
            copy.setOtHours(BigDecimal.ZERO);
            copy.setNormalHours(BigDecimal.ZERO);
            copy.setProjectId(d.getProjectId());
            copy.setCostCodeId(d.getCostCodeId());
            simulatedDays.add(copy);
        }

        PayrollCalculationContext ctx = payrollCalculationContext(run, period, List.of(ts));
        // The context's time-type lookup only contains types that actually
        // appeared in the ORIGINAL month's real days. If we're overriding a
        // day to a type that was never used that month (e.g. the employee
        // never had a sick day, and we're now testing "what if this was
        // sick"), that type would be missing here — and a missing type
        // silently falls back to "paid/normal", making every override look
        // like it changed nothing. Fetch and add any missing override types.
        java.util.Set<UUID> missingTypeIds = dayTimeTypeOverrides.values().stream()
                .filter(typeId -> typeId != null && !ctx.timeTypes().containsKey(typeId))
                .collect(java.util.stream.Collectors.toSet());
        if (!missingTypeIds.isEmpty()) {
            timeTypeRepo.findAllById(missingTypeIds).forEach(t -> ctx.timeTypes().put(t.getId(), t));
        }
        UUID empProject = run.getProjectId() != null ? run.getProjectId() : ctx.employeeProjects().get(emp.getId());
        PayrollRule rule = payrollRule(ctx.rules(), run.getCompanyId(), empProject, emp.getPayStatus());
        BigDecimal shiftHours = shiftHours(ts, rule, ctx.shifts());
        int periodDays = period.getEndDate() != null ? period.getEndDate().getDayOfMonth() : 30;

        PayableBreakdown breakdown = payableBreakdown(simulatedDays, rule, shiftHours, ctx.timeTypes());
        PayrollPolicyContext policy = payrollPolicyContext(ctx, ts, emp, rule, shiftHours, simulatedDays);
        PayrollResult simResult = buildResult(run, ts, emp, rule, breakdown);
        UUID dummyResultId = UUID.randomUUID();

        BigDecimal earnings = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        for (ContractPayItem item : activePayItems(ctx.payItemsByEmployee().getOrDefault(emp.getId(), List.of()), period.getEndDate())) {
            PayrollComponent component = ctx.components().get(item.getPayComponentId());
            if (component == null) {
                continue;
            }
            for (PayrollResultLine line : buildLines(run.getCompanyId(), dummyResultId, item, component, simResult, rule, breakdown, policy, shiftHours, periodDays, ctx.categoryPolicies())) {
                if ("DEDUCTION".equalsIgnoreCase(line.getComponentType())) {
                    deductions = deductions.add(line.getAmount());
                } else {
                    earnings = earnings.add(line.getAmount());
                }
            }
        }
        for (PayrollResultLine otLine : buildOvertimeLines(run.getCompanyId(), dummyResultId, simResult, rule, breakdown)) {
            earnings = earnings.add(otLine.getAmount());
        }
        PayrollResultLine unpaidDeduction = buildMonthlyUnpaidDeductionLine(run.getCompanyId(), dummyResultId, earnings, rule, breakdown, policy);
        if (unpaidDeduction != null) {
            deductions = deductions.add(unpaidDeduction.getAmount());
        }
        return earnings.subtract(deductions);
    }

    public Map<String, Object> calculate(UUID id, TimesheetService.BulkStatusProgressListener progress) {
        PayrollRun run = getEntity(id);
        if (!DRAFT.equals(run.getStatus()) && !CALCULATED.equals(run.getStatus())) {
            throw new BusinessRuleException("payroll.run.state", "Only a DRAFT/CALCULATED payroll run can be recalculated.");
        }
        PayrollPeriod period = periodRepo.findById(run.getPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + run.getPeriodId()));
        assertPayrollScopeLocked(run);
        resultRepo.deleteByRunId(run.getId());
        resultRepo.flush();

        String payGroup = normalizePayGroup(run.getPayGroup());
        List<Timesheet> timesheets = timesheetRepo.findPayrollScope(
                run.getCompanyId(), period.getPeriodYear(), period.getPeriodMonth(), run.getProjectId(), payGroup);
        if (progress != null) {
            progress.onProgress(0, timesheets.size());
        }

        PayrollCalculationContext ctx = payrollCalculationContext(run, period, timesheets);
        List<PayrollResult> results = new ArrayList<>();
        List<PayrollResultLine> lines = new ArrayList<>();
        int processed = 0;

        for (Timesheet ts : timesheets) {
            Employee emp = ctx.employees().get(ts.getEmployeeId());
            if (emp == null) {
                continue;
            }
            UUID empProject = run.getProjectId() != null ? run.getProjectId() : ctx.employeeProjects().get(emp.getId());
            PayrollRule rule = payrollRule(ctx.rules(), run.getCompanyId(), empProject, emp.getPayStatus());
            BigDecimal shiftHours = shiftHours(ts, rule, ctx.shifts());
            int periodDays = period.getEndDate() != null ? period.getEndDate().getDayOfMonth() : 30;
            List<TimesheetDay> tsDays = ctx.daysByTimesheet().getOrDefault(ts.getId(), List.of());
            PayableBreakdown breakdown = payableBreakdown(tsDays, rule, shiftHours, ctx.timeTypes());
            PayrollPolicyContext policy = payrollPolicyContext(ctx, ts, emp, rule, shiftHours, tsDays);
            PayrollResult result = buildResult(run, ts, emp, rule, breakdown);
            result = resultRepo.save(result);
            BigDecimal earnings = BigDecimal.ZERO;
            BigDecimal deductions = BigDecimal.ZERO;
            for (ContractPayItem item : activePayItems(ctx.payItemsByEmployee().getOrDefault(emp.getId(), List.of()), period.getEndDate())) {
                PayrollComponent component = ctx.components().get(item.getPayComponentId());
                if (component == null) {
                    continue;
                }
                for (PayrollResultLine line : buildLines(run.getCompanyId(), result.getId(), item, component, result, rule, breakdown, policy, shiftHours, periodDays, ctx.categoryPolicies())) {
                    lines.add(line);
                    if ("DEDUCTION".equalsIgnoreCase(line.getComponentType())) {
                        deductions = deductions.add(line.getAmount());
                    } else {
                        earnings = earnings.add(line.getAmount());
                    }
                }
            }
            for (PayrollResultLine otLine : buildOvertimeLines(run.getCompanyId(), result.getId(), result, rule, breakdown)) {
                lines.add(otLine);
                earnings = earnings.add(otLine.getAmount());
            }
            PayrollResultLine unpaidDeduction = buildMonthlyUnpaidDeductionLine(run.getCompanyId(), result.getId(), earnings, rule, breakdown, policy);
            if (unpaidDeduction != null) {
                lines.add(unpaidDeduction);
                deductions = deductions.add(unpaidDeduction.getAmount());
            }
            result.setTotalEarnings(earnings);
            result.setTotalDeductions(deductions);
            result.setGross(earnings);
            result.setNet(earnings.subtract(deductions));
            if (earnings.compareTo(BigDecimal.ZERO) == 0) {
                result.setStatus("FLAGGED");
                result.setMessage("No active contract pay items found.");
            }
            results.add(result);
            processed++;
            if (progress != null && (processed % 250 == 0 || processed == timesheets.size())) {
                progress.onProgress(processed, timesheets.size());
            }
        }
        resultRepo.saveAll(results);
        resultRepo.flush();
        lineRepo.saveAll(lines);
        run.setStatus(CALCULATED);
        run.setCalculatedAt(Instant.now());
        runRepo.save(run);
        if (progress != null) {
            progress.onProgress(processed, timesheets.size());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processed", processed);
        result.put("lines", lines.size());
        result.put("gross", results.stream().map(PayrollResult::getGross).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.put("net", results.stream().map(PayrollResult::getNet).reduce(BigDecimal.ZERO, BigDecimal::add));
        return result;
    }

    public PayrollRunDto approve(UUID id) {
        PayrollRun run = getEntity(id);
        if (!CALCULATED.equals(run.getStatus())) {
            throw new BusinessRuleException("payroll.run.approve.state", "Only a CALCULATED payroll run can be approved.");
        }
        run.setStatus("APPROVED");
        run.setApprovedAt(Instant.now());
        run.setApprovedBy(currentUsername());
        return toDto(runRepo.save(run), false);
    }

    public PayrollRunDto lock(UUID id) {
        PayrollRun run = getEntity(id);
        if (!"APPROVED".equals(run.getStatus())) {
            throw new BusinessRuleException("payroll.run.lock.state", "Only an APPROVED payroll run can be locked.");
        }
        run.setStatus("LOCKED");
        run.setLockedAt(Instant.now());
        return toDto(runRepo.save(run), false);
    }

    public void delete(UUID id) {
        PayrollRun run = getEntity(id);
        if (!DRAFT.equals(run.getStatus())) {
            throw new BusinessRuleException("payroll.run.delete.state", "Only a DRAFT payroll run can be deleted.");
        }
        runRepo.delete(run);
    }

    private PayrollResult buildResult(PayrollRun run, Timesheet ts, Employee emp, PayrollRule rule, PayableBreakdown breakdown) {
        PayrollResult result = new PayrollResult();
        result.setCompanyId(run.getCompanyId());
        result.setRunId(run.getId());
        result.setEmployeeId(emp.getId());
        result.setPayStatus(emp.getPayStatus());
        result.setRateBasis(isDailyRule(rule) ? "ACTUAL_DAYS" : "MONTH_DIVISOR");
        result.setDivisor(safeMonthDivisor(rule));
        result.setNormalHours(breakdown.regularPaidHours());
        result.setOtHours(z(breakdown.normalOtHours()).add(z(breakdown.restOtHours())));
        result.setWorkedDays(breakdown.payableDays());
        return result;
    }

    private List<PayrollResultLine> buildLines(UUID companyId, UUID resultId, ContractPayItem item, PayrollComponent component,
                                               PayrollResult result, PayrollRule rule, PayableBreakdown breakdown,
                                               PayrollPolicyContext policy, BigDecimal shiftHours, int periodDays,
                                               Map<UUID, Map<String, CategoryPolicy>> categoryPolicies) {
        CategoryPolicy categoryPolicy = categoryPolicy(rule, component, categoryPolicies);
        ComponentPolicyBreakdown policyBreakdown = componentPolicyBreakdown(component, item, rule, policy, categoryPolicy, periodDays);
        if ("FIXED_AMOUNT".equals(categoryPolicy.basis()) && isPayItemEarning(component) && !isDailyRule(rule)) {
            BigDecimal amount = z(item.getAmount());
            if (isSalaryComponent(component) && result.getDailyRate().compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal divisor = effectiveDivisor(rule, categoryPolicy, periodDays);
                BigDecimal hourly = amount.divide(divisor.multiply(shiftHours), 4, RoundingMode.HALF_UP);
                result.setHourlyRate(hourly);
                result.setDailyRate(hourly.multiply(shiftHours));
            }
            List<PayrollResultLine> lines = new java.util.ArrayList<>();
            lines.add(payItemLine(companyId, resultId, component, component.getName(),
                    BigDecimal.ONE, amount, amount, "FIXED_AMOUNT", component.getPriority()));
            BigDecimal deductQty = z(policyBreakdown.deductQuantity());
            if (deductQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal deductionRate = unitRate(amount, rule, policyBreakdown.basis(), shiftHours, categoryPolicy, periodDays);
                lines.add(manualLine(companyId, resultId, component.getCode(),
                        component.getName() + " - Time type deduction", "DEDUCTION", component.getCategory(),
                        deductQty, deductionRate, round(deductionRate.multiply(deductQty)),
                        "TIME_TYPE_RULE_DEDUCT", component.getPriority() + 1));
            }
            return lines;
        }
        // Monthly base salary is always paid as the fixed full amount (paid leave counts
        // like a normal day). Time-type rules apply only as DEDUCTIONS on top of it,
        // instead of replacing the whole base calculation.
        boolean monthlyBase = isSalaryComponent(component) && isPayItemEarning(component) && !isDailyRule(rule);
        if (policyBreakdown.hasAny() && !monthlyBase) {
            return buildPolicyLines(companyId, resultId, component, item, result, rule, policyBreakdown, shiftHours, categoryPolicy, periodDays);
        }
        if (isPayItemEarning(component)) {
            BigDecimal rate = z(item.getAmount());
            if (isSalaryComponent(component) && result.getDailyRate().compareTo(BigDecimal.ZERO) == 0) {
                if (isDailyRule(rule)) {
                    result.setDailyRate(rate);
                    result.setHourlyRate(rate.divide(shiftHours, 4, RoundingMode.HALF_UP));
                } else {
                    BigDecimal hourly = rate.divide(effectiveDivisor(rule, categoryPolicy, periodDays).multiply(shiftHours), 4, RoundingMode.HALF_UP);
                    result.setHourlyRate(hourly);
                    result.setDailyRate(hourly.multiply(shiftHours));
                }
            }
            if (!isDailyRule(rule)) {
                BigDecimal baseHours = effectiveDivisor(rule, categoryPolicy, periodDays).multiply(shiftHours);
                BigDecimal hourly = rate.divide(baseHours, 4, RoundingMode.HALF_UP);
                BigDecimal componentAmount = round(rate.multiply(z(breakdown.paidHours()).divide(baseHours, 8, RoundingMode.HALF_UP)));
                BigDecimal normalAmount = round(hourly.multiply(z(breakdown.regularPaidHours())));
                BigDecimal restAmount = componentAmount.subtract(normalAmount);
                List<PayrollResultLine> lines = new java.util.ArrayList<>(java.util.stream.Stream.of(
                        payItemLine(companyId, resultId, component, component.getName() + " - Normal paid hours",
                                breakdown.regularPaidHours(), hourly, normalAmount, "REGULAR_HOURS", component.getPriority()),
                        payItemLine(companyId, resultId, component, component.getName() + " - Weekend/Holiday paid hours",
                                breakdown.restPaidHours(), hourly, restAmount, "WEEKLY_REST", component.getPriority() + 1)
                ).filter(l -> z(l.getQuantity()).compareTo(BigDecimal.ZERO) > 0).toList());
                // Apply time-type deductions (e.g. sick-after-N, unpaid) on top of the fixed base.
                if (monthlyBase) {
                    BigDecimal deductQty = z(policyBreakdown.deductQuantity());
                    if (deductQty.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal deductionRate = unitRate(rate, rule, policyBreakdown.basis(), shiftHours, categoryPolicy, periodDays);
                        lines.add(manualLine(companyId, resultId, component.getCode(),
                                component.getName() + " - Time type deduction", "DEDUCTION", component.getCategory(),
                                deductQty, deductionRate, round(deductionRate.multiply(deductQty)),
                                "TIME_TYPE_RULE_DEDUCT", component.getPriority() + 2));
                    }
                }
                return lines;
            }
            return List.of(
                    payItemLine(companyId, resultId, component, component.getName() + " - Regular paid days", breakdown.regularPaidDays(), rate, "REGULAR_DAYS", component.getPriority()),
                    payItemLine(companyId, resultId, component, component.getName() + " - Weekly rest paid days", breakdown.restPaidDays(), rate, "WEEKLY_REST", component.getPriority() + 1)
            ).stream().filter(l -> z(l.getQuantity()).compareTo(BigDecimal.ZERO) > 0).toList();
        }
        PayrollResultLine line = payItemLine(companyId, resultId, component, component.getName(), BigDecimal.ONE,
                z(item.getAmount()), "PAY_ITEM", component.getPriority());
        if (isSalaryComponent(component) && result.getDailyRate().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal dailyRate = z(item.getAmount()).divide(new BigDecimal("30"), 4, RoundingMode.HALF_UP);
            result.setDailyRate(dailyRate);
            result.setHourlyRate(dailyRate.divide(shiftHours, 4, RoundingMode.HALF_UP));
        }
        return List.of(line);
    }

    private List<PayrollResultLine> buildPolicyLines(UUID companyId, UUID resultId, PayrollComponent component,
                                                     ContractPayItem item, PayrollResult result, PayrollRule rule,
                                                     ComponentPolicyBreakdown breakdown, BigDecimal shiftHours,
                                                     CategoryPolicy categoryPolicy, int periodDays) {
        BigDecimal amount = z(item.getAmount());
        BigDecimal unitRate = unitRate(amount, rule, breakdown.basis(), shiftHours, categoryPolicy, periodDays);
        if (isSalaryComponent(component) && result.getDailyRate().compareTo(BigDecimal.ZERO) == 0) {
            if (isDailyRule(rule)) {
                result.setDailyRate(amount);
                result.setHourlyRate(amount.divide(shiftHours, 4, RoundingMode.HALF_UP));
            } else {
                BigDecimal hourly = amount.divide(effectiveDivisor(rule, categoryPolicy, periodDays).multiply(shiftHours), 4, RoundingMode.HALF_UP);
                result.setHourlyRate(hourly);
                result.setDailyRate(amount.divide(effectiveDivisor(rule, categoryPolicy, periodDays), 4, RoundingMode.HALF_UP));
            }
        }
        BigDecimal payQty = z(breakdown.payQuantity());
        BigDecimal deductQty = z(breakdown.deductQuantity());
        BigDecimal payAmount = round(unitRate.multiply(payQty));
        BigDecimal deductAmount = round(unitRate.multiply(deductQty));
        List<PayrollResultLine> lines = new java.util.ArrayList<>();
        if (payQty.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(payItemLine(companyId, resultId, component,
                    component.getName() + " - Time type pay",
                    payQty, unitRate, payAmount, "TIME_TYPE_RULE_PAY", component.getPriority()));
        }
        if (deductQty.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(manualLine(companyId, resultId, component.getCode(),
                    component.getName() + " - Time type deduction",
                    "DEDUCTION", component.getCategory(),
                    deductQty, unitRate, deductAmount, "TIME_TYPE_RULE_DEDUCT", component.getPriority() + 1));
        }
        return lines;
    }

    private PayrollResultLine payItemLine(UUID companyId, UUID resultId, PayrollComponent component, String name,
                                          BigDecimal quantity, BigDecimal rate, String source, int sortOrder) {
        return payItemLine(companyId, resultId, component, name, quantity, rate,
                round(z(rate).multiply(z(quantity))), source, sortOrder);
    }

    private PayrollResultLine payItemLine(UUID companyId, UUID resultId, PayrollComponent component, String name,
                                          BigDecimal quantity, BigDecimal rate, BigDecimal amount,
                                          String source, int sortOrder) {
        PayrollResultLine line = new PayrollResultLine();
        line.setCompanyId(companyId);
        line.setResultId(resultId);
        line.setComponentCode(component.getCode());
        line.setComponentName(name);
        line.setComponentType(component.getComponentType());
        line.setCategory(component.getCategory());
        line.setQuantity(z(quantity));
        line.setRate(z(rate));
        line.setAmount(round(amount));
        line.setSource(source);
        line.setSortOrder(sortOrder);
        return line;
    }

    private List<PayrollResultLine> buildOvertimeLines(UUID companyId, UUID resultId, PayrollResult result, PayrollRule rule,
                                                       PayableBreakdown breakdown) {
        BigDecimal hourlyRate = z(result.getHourlyRate());
        if (hourlyRate.compareTo(BigDecimal.ZERO) == 0) return List.of();
        PayrollResultLine normal = overtimeLine(companyId, resultId, "Normal overtime", breakdown.normalOtHours(), hourlyRate, rule.getOtMultiplier(), 900);
        PayrollResultLine rest = overtimeLine(companyId, resultId, "Weekly rest overtime", breakdown.restOtHours(), hourlyRate, rule.getRestDayOtMultiplier(), 901);
        return List.of(normal, rest).stream().filter(l -> z(l.getQuantity()).compareTo(BigDecimal.ZERO) > 0).toList();
    }

    private PayrollResultLine overtimeLine(UUID companyId, UUID resultId, String name, BigDecimal hours,
                                           BigDecimal hourlyRate, BigDecimal multiplier, int sortOrder) {
        PayrollResultLine line = new PayrollResultLine();
        line.setCompanyId(companyId);
        line.setResultId(resultId);
        line.setComponentCode("OT");
        line.setComponentName(name);
        line.setComponentType("EARNING");
        line.setCategory("OVERTIME");
        line.setQuantity(z(hours));
        line.setRate(z(hourlyRate).multiply(z(multiplier)));
        line.setAmount(round(z(hours).multiply(z(hourlyRate)).multiply(z(multiplier))));
        line.setSource("OVERTIME");
        line.setSortOrder(sortOrder);
        return line;
    }

    private PayrollResultLine buildMonthlyUnpaidDeductionLine(UUID companyId, UUID resultId, BigDecimal earnings,
                                                              PayrollRule rule, PayableBreakdown breakdown,
                                                              PayrollPolicyContext policy) {
        if (policy.hasConfiguredRules()) {
            return null;
        }
        if (isDailyRule(rule) || z(breakdown.unpaidHours()).compareTo(BigDecimal.ZERO) <= 0
                || z(earnings).compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal hourlyGross = z(earnings).divide(z(breakdown.paidHours()).max(BigDecimal.ONE), 4, RoundingMode.HALF_UP);
        PayrollResultLine line = new PayrollResultLine();
        line.setCompanyId(companyId);
        line.setResultId(resultId);
        line.setComponentCode("UNPAID");
        line.setComponentName("Unpaid absence / not employed days");
        line.setComponentType("DEDUCTION");
        line.setCategory("UNPAID");
        line.setQuantity(breakdown.unpaidHours());
        line.setRate(hourlyGross);
        line.setAmount(round(hourlyGross.multiply(breakdown.unpaidHours())));
        line.setSource("AUTO_DEDUCTION");
        line.setSortOrder(950);
        return line;
    }

    private PayrollPolicyContext payrollPolicyContext(PayrollCalculationContext ctx, Timesheet ts, Employee emp,
                                                      PayrollRule rule, BigDecimal shiftHours, List<TimesheetDay> days) {
        Map<UUID, TimeType> types = ctx.timeTypes();
        Map<UUID, Map<UUID, TimeTypePayrollRule>> rulesByTimeType = ctx.rulesByTimeType();
        Map<UUID, Integer> consecutivePos = new HashMap<>();
        Map<UUID, Integer> annualPos = new HashMap<>();
        computeThresholdPositions(days, types, rulesByTimeType, ctx.priorAnnualCounts(), emp, consecutivePos, annualPos);
        return new PayrollPolicyContext(days, types, rulesByTimeType, rule, shiftHours, consecutivePos, annualPos);
    }

    /**
     * For each day, record its position in the running count of its own time type:
     *  - consecutivePos: length of the current unbroken spell (REST/HOLIDAY days
     *    bridge the spell and are not counted; any other time type breaks it).
     *  - annualPos: cumulative count of that time type within the configured rule year.
     * These feed the "apply effect only after N days" thresholds.
     */
    private void computeThresholdPositions(List<TimesheetDay> days, Map<UUID, TimeType> types,
                                           Map<UUID, Map<UUID, TimeTypePayrollRule>> rulesByTimeType,
                                           Map<AnnualUsageKey, Integer> priorAnnualCounts,
                                           Employee employee,
                                           Map<UUID, Integer> consecutivePos, Map<UUID, Integer> annualPos) {
        Map<UUID, Integer> spell = new HashMap<>();
        Map<UUID, Integer> annual = new HashMap<>();
        for (TimesheetDay d : days) {
            UUID tt = d.getTimeTypeId();
            if (tt == null) {
                continue;
            }
            TimeType type = types.get(tt);
            String cat = type != null ? type.getCategory() : "REGULAR";
            int prior = priorAnnualCountForDay(priorAnnualCounts, employee, d, rulesByTimeType.get(tt));
            annualPos.put(d.getId(), prior + annual.merge(tt, 1, Integer::sum));
            boolean bridge = "REST".equalsIgnoreCase(cat) || "HOLIDAY".equalsIgnoreCase(cat);
            if (bridge) {
                continue; // keep all spells alive, do not count
            }
            int c = spell.getOrDefault(tt, 0) + 1;
            for (UUID k : new java.util.ArrayList<>(spell.keySet())) {
                if (!k.equals(tt)) {
                    spell.put(k, 0);
                }
            }
            spell.put(tt, c);
            consecutivePos.put(d.getId(), c);
        }
    }

    private ComponentPolicyBreakdown componentPolicyBreakdown(PayrollComponent component, ContractPayItem item,
                                                              PayrollRule rule, PayrollPolicyContext policy,
                                                              CategoryPolicy categoryPolicy, int periodDays) {
        BigDecimal payQty = BigDecimal.ZERO;
        BigDecimal deductQty = BigDecimal.ZERO;
        BigDecimal legacyPayHours = BigDecimal.ZERO;
        String basis = "HOURS";
        boolean touched = false;
        for (TimesheetDay day : policy.days()) {
            TimeType type = day.getTimeTypeId() != null ? policy.types().get(day.getTimeTypeId()) : null;
            Map<UUID, TimeTypePayrollRule> byComponent = day.getTimeTypeId() != null ? policy.rulesByTimeType().get(day.getTimeTypeId()) : null;
            TimeTypePayrollRule explicit = byComponent != null ? byComponent.get(item.getPayComponentId()) : null;
            if (day.getTimeTypeId() != null && explicit == null) {
                throw new BusinessRuleException("payroll.time_type_rule.missing",
                        "Payroll setup is incomplete. Configure payroll rules for time type "
                                + timeTypeLabel(type, day.getTimeTypeId()) + " and pay component "
                                + component.getCode() + " - " + component.getName() + ".");
            }
            DayEffect effect = explicitEffect(explicit, day, type, rule, policy.shiftHours());
            if (!effect.hasAny()) {
                continue;
            }
            // Threshold gate: for a rule with a threshold, the first N qualifying
            // days are protected — the effect applies only from day N+1 onward.
            if (explicit != null && explicit.getThresholdDays() > 0) {
                String thresholdScope = normalizeThresholdScope(explicit.getThresholdScope(), explicit.getThresholdDays());
                Integer pos = "ANNUAL".equalsIgnoreCase(thresholdScope)
                        ? policy.annualPos().get(day.getId())
                        : policy.consecutivePos().get(day.getId());
                if (pos == null || pos <= explicit.getThresholdDays()) {
                    continue; // still within the protected window
                }
            }
            touched = true;
            basis = effect.basis();
            payQty = payQty.add(effect.payQuantity());
            deductQty = deductQty.add(effect.deductQuantity());
            if (explicit == null) {
                legacyPayHours = legacyPayHours.add(effect.payQuantity());
            }
        }
        if ("FULL_MONTH".equals(categoryPolicy.basis()) && !isDailyRule(rule) && legacyPayHours.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisorFull = effectiveDivisor(rule, categoryPolicy, periodDays).multiply(policy.shiftHours());
            payQty = payQty.subtract(legacyPayHours).add(divisorFull);
        }
        return new ComponentPolicyBreakdown(payQty, deductQty, basis, touched);
    }

    private DayEffect explicitEffect(TimeTypePayrollRule explicit, TimesheetDay day, TimeType type, PayrollRule rule, BigDecimal shiftHours) {
        if (explicit == null) {
            return legacyEffect(type, day, rule, shiftHours);
        }
        BigDecimal baseQuantity = quantityForBasis(day, explicit.getBasis(), shiftHours, isDailyRule(rule));
        BigDecimal scaled = baseQuantity.multiply(z(explicit.getPercent()).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        if ("DEDUCT".equalsIgnoreCase(explicit.getAction())) {
            DayEffect legacy = legacyEffect(type, day, rule, shiftHours);
            // A daily-rate employee's pay is simply the sum of days actually
            // worked — an unpaid/absence day already contributes nothing to
            // that sum. Deducting it again here would subtract the same
            // absence twice. This does NOT apply to monthly employees, whose
            // base pay starts full and genuinely needs this deduction.
            String category = type != null ? type.getCategory() : "REGULAR";
            boolean unpaidDay = isUnpaidCategory(category) || (type != null && !type.isPaid());
            if (isDailyRule(rule) && unpaidDay) {
                return DayEffect.none();
            }
            return new DayEffect(legacy.payQuantity(), scaled, explicit.getBasis());
        }
        if ("PAY".equalsIgnoreCase(explicit.getAction())) {
            return new DayEffect(scaled, BigDecimal.ZERO, explicit.getBasis());
        }
        if ("IGNORE".equalsIgnoreCase(explicit.getAction()) || "SUSPEND".equalsIgnoreCase(explicit.getAction())) {
            return DayEffect.none();
        }
        throw new BusinessRuleException("payroll.time_type_rule.action",
                "Unsupported payroll action " + explicit.getAction() + " for time type "
                        + timeTypeLabel(type, day.getTimeTypeId()) + ".");
    }

    private static String timeTypeLabel(TimeType type, UUID timeTypeId) {
        if (type == null) {
            return String.valueOf(timeTypeId);
        }
        return type.getCode() + " - " + type.getName();
    }

    private DayEffect legacyEffect(TimeType type, TimesheetDay day, PayrollRule rule, BigDecimal shiftHours) {
        String category = type != null ? type.getCategory() : "REGULAR";
        boolean rest = "REST".equalsIgnoreCase(category) || "HOLIDAY".equalsIgnoreCase(category);
        boolean daily = isDailyRule(rule);
        BigDecimal standardHours = shiftHours;
        if (rest) {
            if (!daily && (type == null || type.isPaid() || rule.isWeeklyRestPaid())) {
                return new DayEffect(restHours(day, standardHours), BigDecimal.ZERO, "HOURS");
            }
            return DayEffect.none();
        }
        if (isUnpaidCategory(category) || (type != null && !type.isPaid())) {
            if (daily) {
                // Daily pay = days actually worked x daily rate. An absent/
                // unpaid day already contributes nothing to that sum, so
                // there is nothing left here to deduct on top of it.
                return DayEffect.none();
            }
            return new DayEffect(BigDecimal.ZERO, payrollHours(day, standardHours), "HOURS");
        }
        if (type == null || type.isPaid()) {
            return new DayEffect(payrollHours(day, standardHours), BigDecimal.ZERO, "HOURS");
        }
        return DayEffect.none();
    }

    private BigDecimal quantityForBasis(TimesheetDay day, String basis, BigDecimal shiftHours, boolean daily) {
        String normalized = basis == null ? "HOURS" : basis.toUpperCase();
        BigDecimal standardHours = shiftHours;
        if ("DAYS".equals(normalized) || "FIXED".equals(normalized)) {
            BigDecimal hours = daily ? actualPayHours(day) : payrollHours(day, standardHours);
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            return hours.divide(standardHours, 4, RoundingMode.HALF_UP);
        }
        return daily ? actualPayHours(day) : payrollHours(day, standardHours);
    }

    private BigDecimal unitRate(BigDecimal amount, PayrollRule rule, String basis, BigDecimal shiftHours,
                                CategoryPolicy categoryPolicy, int periodDays) {
        String normalized = basis == null ? "HOURS" : basis.toUpperCase();
        if ("DAYS".equals(normalized) || "FIXED".equals(normalized)) {
            return isDailyRule(rule)
                    ? amount
                    : amount.divide(effectiveDivisor(rule, categoryPolicy, periodDays), 4, RoundingMode.HALF_UP);
        }
        return isDailyRule(rule)
                ? amount.divide(shiftHours, 4, RoundingMode.HALF_UP)
                : amount.divide(effectiveDivisor(rule, categoryPolicy, periodDays).multiply(shiftHours), 4, RoundingMode.HALF_UP);
    }

    private PayrollResultLine manualLine(UUID companyId, UUID resultId, String code, String name, String componentType,
                                         String category, BigDecimal quantity, BigDecimal rate, BigDecimal amount,
                                         String source, int sortOrder) {
        PayrollResultLine line = new PayrollResultLine();
        line.setCompanyId(companyId);
        line.setResultId(resultId);
        line.setComponentCode(code);
        line.setComponentName(name);
        line.setComponentType(componentType);
        line.setCategory(category);
        line.setQuantity(z(quantity));
        line.setRate(z(rate));
        line.setAmount(round(amount));
        line.setSource(source);
        line.setSortOrder(sortOrder);
        return line;
    }

    private PayrollRule payrollRule(UUID companyId, UUID projectId, String payStatus) {
        String group = normalizePayGroup(payStatus);
        if (projectId != null) {
            Optional<PayrollRule> projectRule =
                    ruleRepo.findByCompanyIdAndProjectIdAndPayGroupAndStatus(companyId, projectId, group, "ACTIVE");
            if (projectRule.isPresent()) {
                return projectRule.get();
            }
        }
        return ruleRepo.findDefaultByCompanyIdAndPayGroupAndStatus(companyId, group, "ACTIVE")
                .orElseThrow(() -> new BusinessRuleException("payroll.rule.missing",
                        "No active payroll rule is configured for pay group " + group + "."));
    }

    private PayrollRule payrollRule(Map<PayrollRuleKey, PayrollRule> rules, UUID companyId, UUID projectId, String payStatus) {
        String group = normalizePayGroup(payStatus);
        PayrollRule projectRule = projectId != null ? rules.get(new PayrollRuleKey(projectId, group)) : null;
        if (projectRule != null) {
            return projectRule;
        }
        PayrollRule defaultRule = rules.get(new PayrollRuleKey(null, group));
        if (defaultRule != null) {
            return defaultRule;
        }
        throw new BusinessRuleException("payroll.rule.missing",
                "No active payroll rule is configured for pay group " + group + ".");
    }

    private PayableBreakdown payableBreakdown(Timesheet ts, PayrollRule rule, BigDecimal shiftHours) {
        return payableBreakdown(dayRepo.findByTimesheetIdOrderByWorkDate(ts.getId()), rule, shiftHours, new HashMap<>());
    }

    private PayableBreakdown payableBreakdown(List<TimesheetDay> days, PayrollRule rule, BigDecimal shiftHours,
                                              Map<UUID, TimeType> types) {
        BigDecimal standardHours = shiftHours;
        BigDecimal regularPaidDays = BigDecimal.ZERO;
        BigDecimal restPaidDays = BigDecimal.ZERO;
        BigDecimal unpaidDays = BigDecimal.ZERO;
        BigDecimal regularPaidHours = BigDecimal.ZERO;
        BigDecimal restPaidHours = BigDecimal.ZERO;
        BigDecimal paidHours = BigDecimal.ZERO;
        BigDecimal unpaidHours = BigDecimal.ZERO;
        BigDecimal normalOtHours = BigDecimal.ZERO;
        BigDecimal restOtHours = BigDecimal.ZERO;
        boolean daily = isDailyRule(rule);
        for (TimesheetDay day : days) {
            TimeType type = day.getTimeTypeId() == null ? null
                    : types.get(day.getTimeTypeId());
            String category = type != null ? type.getCategory() : "REGULAR";
            boolean rest = "REST".equalsIgnoreCase(category) || "HOLIDAY".equalsIgnoreCase(category);
            if (rest) {
                if (!daily && (type == null || type.isPaid() || rule.isWeeklyRestPaid())) {
                    restPaidDays = restPaidDays.add(BigDecimal.ONE);
                    BigDecimal hours = restHours(day, standardHours);
                    restPaidHours = restPaidHours.add(hours);
                    paidHours = paidHours.add(hours);
                }
                restOtHours = restOtHours.add(z(day.getOtHours()));
            } else if (isUnpaidCategory(category) || (type != null && !type.isPaid())) {
                unpaidDays = unpaidDays.add(BigDecimal.ONE);
                unpaidHours = unpaidHours.add(payrollHours(day, standardHours));
                normalOtHours = normalOtHours.add(z(day.getOtHours()));
            } else if (type == null || type.isPaid()) {
                BigDecimal hours = payrollHours(day, standardHours);
                if (hours.compareTo(BigDecimal.ZERO) > 0) {
                    regularPaidDays = regularPaidDays.add(hours.divide(standardHours, 2, RoundingMode.HALF_UP));
                    regularPaidHours = regularPaidHours.add(hours);
                    paidHours = paidHours.add(hours);
                }
                normalOtHours = normalOtHours.add(z(day.getOtHours()));
            }
        }
        if (!daily) {
            BigDecimal divisor = safeMonthDivisor(rule);
            BigDecimal maxRegularDays = divisor.subtract(restPaidDays).max(BigDecimal.ZERO);
            BigDecimal maxRegularHours = maxRegularDays.multiply(standardHours);
            boolean fixed = rule == null || rule.getDivisorMode() == null
                    || !"ACTUAL_MONTH".equalsIgnoreCase(rule.getDivisorMode());
            if (fixed) {
                // Fixed divisor: pay the full month (divisor-based) regardless of how
                // many calendar days it has, reduced only by actual unpaid days.
                BigDecimal billable = maxRegularHours.subtract(unpaidHours).max(BigDecimal.ZERO);
                regularPaidHours = billable;
                regularPaidDays = standardHours.compareTo(BigDecimal.ZERO) > 0
                        ? billable.divide(standardHours, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            } else if (regularPaidHours.compareTo(maxRegularHours) > 0) {
                // Actual-month mode: keep actual paid hours, capped at the divisor ceiling.
                regularPaidHours = maxRegularHours;
                regularPaidDays = maxRegularDays;
            }
            paidHours = regularPaidHours.add(restPaidHours);
        }
        return new PayableBreakdown(regularPaidDays, restPaidDays, unpaidDays,
                regularPaidHours, restPaidHours, paidHours, unpaidHours, normalOtHours, restOtHours);
    }

    private static BigDecimal restHours(TimesheetDay day, BigDecimal standardHours) {
        BigDecimal normal = z(day.getNormalHours());
        return normal.compareTo(BigDecimal.ZERO) > 0 ? normal : z(standardHours);
    }

    private static BigDecimal payrollHours(TimesheetDay day, BigDecimal standardHours) {
        BigDecimal normal = z(day.getNormalHours());
        if (normal.compareTo(BigDecimal.ZERO) > 0) return normal;
        BigDecimal planned = z(day.getPlannedHours());
        if (planned.compareTo(BigDecimal.ZERO) > 0) return planned;
        BigDecimal worked = z(day.getWorkedHours()).subtract(z(day.getOtHours())).max(BigDecimal.ZERO);
        if (worked.compareTo(BigDecimal.ZERO) > 0) return worked;
        return z(standardHours);
    }

    private static BigDecimal actualPayHours(TimesheetDay day) {
        BigDecimal normal = z(day.getNormalHours());
        if (normal.compareTo(BigDecimal.ZERO) > 0) return normal;
        return z(day.getWorkedHours()).subtract(z(day.getOtHours())).max(BigDecimal.ZERO);
    }

    private static boolean isUnpaidCategory(String category) {
        return "UNPAID".equalsIgnoreCase(category);
    }

    private static boolean isDailyRule(PayrollRule rule) {
        return rule != null && "DAILY_RATE".equalsIgnoreCase(rule.getPayItemBasis());
    }

    private static boolean isPayItemEarning(PayrollComponent component) {
        return component != null && "EARNING".equalsIgnoreCase(component.getComponentType());
    }

    private static boolean isSalaryComponent(PayrollComponent component) {
        if (component == null) return false;
        String category = component.getCategory() == null ? "" : component.getCategory();
        String name = component.getName() == null ? "" : component.getName();
        return "SALARY".equalsIgnoreCase(category) || name.toUpperCase().contains("BASE");
    }

    private CategoryPolicy categoryPolicy(PayrollRule rule, PayrollComponent component,
                                          Map<UUID, Map<String, CategoryPolicy>> categoryPolicies) {
        String category = component != null && component.getCategory() != null && !component.getCategory().isBlank()
                ? normalizeCategory(component.getCategory())
                : "OTHER";
        if (rule != null && rule.getId() != null) {
            Map<String, CategoryPolicy> byCategory = categoryPolicies.get(rule.getId());
            if (byCategory != null) {
                CategoryPolicy policy = byCategory.get(category);
                if (policy != null) {
                    return policy;
                }
            }
        }
        String basis = "SALARY".equals(category) ? "FULL_MONTH" : "ACTUAL_PAYABLE";
        return new CategoryPolicy(category, basis, "INHERIT", null);
    }

    private CategoryPolicy categoryPolicy(PayrollRule rule, PayrollComponent component) {
        return categoryPolicy(rule, component, Collections.emptyMap());
    }

    private BigDecimal effectiveDivisor(PayrollRule rule, CategoryPolicy policy, int periodDays) {
        String mode = policy != null ? policy.divisorMode() : "INHERIT";
        if ("INHERIT".equalsIgnoreCase(mode)) {
            mode = rule != null && rule.getDivisorMode() != null ? rule.getDivisorMode() : "FIXED";
        }
        if ("ACTUAL_MONTH".equalsIgnoreCase(mode)) {
            return BigDecimal.valueOf(periodDays > 0 ? periodDays : 30);
        }
        BigDecimal divisor = policy != null && policy.monthDivisor() != null
                ? z(policy.monthDivisor())
                : safeMonthDivisor(rule);
        return divisor.compareTo(BigDecimal.ZERO) > 0 ? divisor : new BigDecimal("30.00");
    }

    private static String normalizeCategoryBasis(String basis) {
        if (basis == null || basis.isBlank()) {
            return "ACTUAL_PAYABLE";
        }
        String normalized = basis.trim().toUpperCase();
        if ("FULL_MONTH".equals(normalized) || "FIXED_AMOUNT".equals(normalized)) {
            return normalized;
        }
        return "ACTUAL_PAYABLE";
    }

    private static String normalizeCategory(String category) {
        return category == null || category.isBlank() ? "OTHER" : category.trim().toUpperCase();
    }

    private static String normalizeCategoryDivisorMode(String divisorMode) {
        if (divisorMode == null || divisorMode.isBlank()) {
            return "INHERIT";
        }
        String normalized = divisorMode.trim().toUpperCase();
        if ("FIXED".equals(normalized) || "ACTUAL_MONTH".equals(normalized)) {
            return normalized;
        }
        return "INHERIT";
    }

    private static String normalizeThresholdScope(String thresholdScope, int thresholdDays) {
        if (thresholdDays <= 0) {
            return "NONE";
        }
        if (thresholdScope != null && "ANNUAL".equalsIgnoreCase(thresholdScope)) {
            return "ANNUAL";
        }
        return "CONSECUTIVE";
    }

    private static BigDecimal safeHours(PayrollRule rule) {
        BigDecimal hours = rule != null ? z(rule.getStandardHoursPerDay()) : BigDecimal.ZERO;
        return hours.compareTo(BigDecimal.ZERO) > 0 ? hours : new BigDecimal("8.00");
    }

    private BigDecimal shiftHours(Timesheet ts, PayrollRule rule) {
        return shiftHours(ts, rule, Collections.emptyMap());
    }

    private BigDecimal shiftHours(Timesheet ts, PayrollRule rule, Map<UUID, Shift> shifts) {
        if (ts != null && ts.getShiftId() != null) {
            Shift shift = shifts.get(ts.getShiftId());
            if (shift == null && shifts.isEmpty()) {
                shift = shiftRepo.findById(ts.getShiftId()).orElse(null);
            }
            if (shift != null && shift.getStandardHours() != null
                    && z(shift.getStandardHours()).compareTo(BigDecimal.ZERO) > 0) {
                return shift.getStandardHours();
            }
        }
        return safeHours(rule);
    }

    private static BigDecimal safeMonthDivisor(PayrollRule rule) {
        BigDecimal divisor = rule != null ? z(rule.getMonthDivisor()) : BigDecimal.ZERO;
        return divisor.compareTo(BigDecimal.ZERO) > 0 ? divisor : new BigDecimal("30.00");
    }

    private static BigDecimal round(BigDecimal value) {
        return z(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record PayableBreakdown(BigDecimal regularPaidDays, BigDecimal restPaidDays, BigDecimal unpaidDays,
                                    BigDecimal regularPaidHours, BigDecimal restPaidHours,
                                    BigDecimal paidHours, BigDecimal unpaidHours,
                                    BigDecimal normalOtHours, BigDecimal restOtHours) {
        BigDecimal payableDays() {
            return z(regularPaidDays).add(z(restPaidDays));
        }
    }

    private record PayrollPolicyContext(List<TimesheetDay> days,
                                        Map<UUID, TimeType> types,
                                        Map<UUID, Map<UUID, TimeTypePayrollRule>> rulesByTimeType,
                                        PayrollRule payrollRule,
                                        BigDecimal shiftHours,
                                        Map<UUID, Integer> consecutivePos,
                                        Map<UUID, Integer> annualPos) {
        boolean hasConfiguredRules() {
            return rulesByTimeType.values().stream().anyMatch(map -> map != null && !map.isEmpty());
        }
    }

    private record ComponentPolicyBreakdown(BigDecimal payQuantity, BigDecimal deductQuantity, String basis, boolean touched) {
        boolean hasAny() {
            return touched && (z(payQuantity).compareTo(BigDecimal.ZERO) > 0 || z(deductQuantity).compareTo(BigDecimal.ZERO) > 0);
        }
    }

    private record CategoryPolicy(String category, String basis, String divisorMode, BigDecimal monthDivisor) {
    }

    private record PayrollRuleKey(UUID projectId, String payGroup) {
    }

    private record AnnualUsageKey(UUID employeeId, UUID timeTypeId, String yearBasis) {
    }

    private record PayrollCalculationContext(Map<UUID, Employee> employees,
                                             Map<UUID, UUID> employeeProjects,
                                             Map<UUID, List<ContractPayItem>> payItemsByEmployee,
                                             Map<UUID, List<TimesheetDay>> daysByTimesheet,
                                             Map<UUID, Shift> shifts,
                                             Map<UUID, TimeType> timeTypes,
                                             Map<UUID, Map<UUID, TimeTypePayrollRule>> rulesByTimeType,
                                             Map<PayrollRuleKey, PayrollRule> rules,
                                             Map<UUID, Map<String, CategoryPolicy>> categoryPolicies,
                                             Map<UUID, PayrollComponent> components,
                                             Map<AnnualUsageKey, Integer> priorAnnualCounts) {
    }

    private record DayEffect(BigDecimal payQuantity, BigDecimal deductQuantity, String basis) {
        static DayEffect none() {
            return new DayEffect(BigDecimal.ZERO, BigDecimal.ZERO, "HOURS");
        }

        boolean hasAny() {
            return z(payQuantity).compareTo(BigDecimal.ZERO) > 0 || z(deductQuantity).compareTo(BigDecimal.ZERO) > 0;
        }
    }

    private List<ContractPayItem> activePayItems(UUID employeeId, LocalDate asOf) {
        return activePayItems(payItemRepo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId), asOf);
    }

    private List<ContractPayItem> activePayItems(List<ContractPayItem> items, LocalDate asOf) {
        return items.stream()
                .filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus()))
                .filter(i -> i.getEffectiveFrom() == null || !i.getEffectiveFrom().isAfter(asOf))
                .filter(i -> i.getEffectiveTo() == null || !i.getEffectiveTo().isBefore(asOf))
                .toList();
    }

    private Map<AnnualUsageKey, Integer> priorAnnualCounts(UUID companyId, PayrollPeriod period,
                                                           Map<UUID, Employee> employees,
                                                           Set<UUID> timeTypeIds,
                                                           Map<UUID, Map<UUID, TimeTypePayrollRule>> rulesByTimeType) {
        if (employees.isEmpty() || timeTypeIds.isEmpty() || period.getStartDate() == null) {
            return Map.of();
        }
        Map<UUID, LocalDate> fromByEmployee = new HashMap<>();
        LocalDate earliest = period.getStartDate();
        for (Employee employee : employees.values()) {
            LocalDate from = annualWindowStart(employee, period.getStartDate(), rulesByTimeType.values().stream()
                    .flatMap(m -> m.values().stream())
                    .filter(r -> "ANNUAL".equalsIgnoreCase(normalizeThresholdScope(r.getThresholdScope(), r.getThresholdDays())))
                    .findFirst().orElse(null));
            fromByEmployee.put(employee.getId(), from);
            if (from.isBefore(earliest)) {
                earliest = from;
            }
        }
        List<TimesheetDay> history = dayRepo.findEmployeeTimeTypeDaysBefore(companyId, employees.keySet(), timeTypeIds, earliest, period.getStartDate());
        Map<UUID, UUID> employeeByTimesheet = new HashMap<>();
        Set<UUID> historyTimesheetIds = new HashSet<>();
        for (TimesheetDay day : history) {
            historyTimesheetIds.add(day.getTimesheetId());
        }
        if (!historyTimesheetIds.isEmpty()) {
            timesheetRepo.findAllById(historyTimesheetIds).forEach(ts -> employeeByTimesheet.put(ts.getId(), ts.getEmployeeId()));
        }
        Map<AnnualUsageKey, Integer> counts = new HashMap<>();
        for (TimesheetDay day : history) {
            UUID employeeId = employeeByTimesheet.get(day.getTimesheetId());
            if (employeeId == null || day.getTimeTypeId() == null) {
                continue;
            }
            Employee employee = employees.get(employeeId);
            Map<UUID, TimeTypePayrollRule> rules = rulesByTimeType.get(day.getTimeTypeId());
            if (employee == null || rules == null || rules.isEmpty()) {
                continue;
            }
            for (TimeTypePayrollRule rule : rules.values()) {
                if (!"ANNUAL".equalsIgnoreCase(normalizeThresholdScope(rule.getThresholdScope(), rule.getThresholdDays()))) {
                    continue;
                }
                LocalDate from = annualWindowStart(employee, period.getStartDate(), rule);
                if (!day.getWorkDate().isBefore(from)) {
                    counts.merge(new AnnualUsageKey(employeeId, day.getTimeTypeId(), annualBasisKey(rule)), 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    private int priorAnnualCountForDay(Map<AnnualUsageKey, Integer> priorAnnualCounts, Employee employee, TimesheetDay day,
                                       Map<UUID, TimeTypePayrollRule> rules) {
        if (employee == null || day.getTimeTypeId() == null || rules == null || rules.isEmpty()) {
            return 0;
        }
        int max = 0;
        for (TimeTypePayrollRule rule : rules.values()) {
            if ("ANNUAL".equalsIgnoreCase(normalizeThresholdScope(rule.getThresholdScope(), rule.getThresholdDays()))) {
                max = Math.max(max, priorAnnualCounts.getOrDefault(
                        new AnnualUsageKey(employee.getId(), day.getTimeTypeId(), annualBasisKey(rule)), 0));
            }
        }
        return max;
    }

    private static LocalDate annualWindowStart(Employee employee, LocalDate periodStart, TimeTypePayrollRule rule) {
        if (rule != null && "HIRE_DATE".equalsIgnoreCase(rule.getYearBasis()) && employee.getHireDate() != null) {
            MonthDay hireDay = MonthDay.from(employee.getHireDate());
            LocalDate candidate = hireDay.isValidYear(periodStart.getYear())
                    ? hireDay.atYear(periodStart.getYear())
                    : LocalDate.of(periodStart.getYear(), 2, 28);
            return candidate.isAfter(periodStart) ? candidate.minusYears(1) : candidate;
        }
        return LocalDate.of(periodStart.getYear(), 1, 1);
    }

    private static String annualBasisKey(TimeTypePayrollRule rule) {
        return rule != null && "HIRE_DATE".equalsIgnoreCase(rule.getYearBasis()) ? "HIRE_DATE" : "CALENDAR";
    }

    private boolean matchesScope(Employee emp, PayrollRun run) {
        if (!normalizePayGroup(run.getPayGroup()).equals("ALL")
                && !normalizePayGroup(run.getPayGroup()).equals(normalizePayGroup(emp.getPayStatus()))) {
            return false;
        }
        return run.getProjectId() == null || run.getProjectId().equals(employeeProject(emp.getId()));
    }

    private void assertPayrollScopeLocked(PayrollRun run) {
        if (run.getProjectId() == null) {
            return;
        }
        String group = normalizePayGroup(run.getPayGroup());
        boolean locked = periodProjectRepo
                .findByCompanyIdAndPeriodIdAndProjectIdAndPayGroup(run.getCompanyId(), run.getPeriodId(), run.getProjectId(), "ALL")
                .map(PayrollPeriodProject::getStatus).filter(s -> "LOCKED".equals(s) || "CLOSED".equals(s)).isPresent()
                || periodProjectRepo
                .findByCompanyIdAndPeriodIdAndProjectIdAndPayGroup(run.getCompanyId(), run.getPeriodId(), run.getProjectId(), group)
                .map(PayrollPeriodProject::getStatus).filter(s -> "LOCKED".equals(s) || "CLOSED".equals(s)).isPresent();
        if (!locked) {
            throw new BusinessRuleException("payroll.scope.not.locked",
                    "Lock this project/pay group from Payroll Calendar before calculating payroll.");
        }
    }

    private UUID employeeProject(UUID employeeId) {
        for (Assignment a : assignmentRepo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId)) {
            if (a.getProjectId() != null && "ACTIVE".equalsIgnoreCase(a.getStatus())) {
                return a.getProjectId();
            }
        }
        return null;
    }

    private PayrollRun getEntity(UUID id) {
        PayrollRun run = runRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + id));
        UUID companyId = TenantContext.requireCompanyId();
        if (!companyId.equals(run.getCompanyId())) {
            throw new ResourceNotFoundException("Payroll run not found: " + id);
        }
        return run;
    }

    private PayrollRunDto toDto(PayrollRun run, boolean includeResults) {
        PayrollRunDto dto = new PayrollRunDto();
        dto.setId(run.getId());
        dto.setPeriodId(run.getPeriodId());
        periodRepo.findById(run.getPeriodId()).ifPresent(period -> {
            dto.setPeriodName(period.getName());
            dto.setPeriodYear(period.getPeriodYear());
            dto.setPeriodMonth(period.getPeriodMonth());
            dto.setPeriodStartDate(period.getStartDate());
            dto.setPeriodEndDate(period.getEndDate());
            dto.setPayDate(period.getPayDate());
        });
        dto.setProjectId(run.getProjectId());
        dto.setPayGroup(run.getPayGroup());
        dto.setRunType(run.getRunType());
        dto.setStatus(run.getStatus());
        dto.setCurrencyCode(run.getCurrencyCode());
        dto.setCalculatedAt(run.getCalculatedAt());
        dto.setNotes(run.getNotes());
        if (includeResults) {
            List<PayrollResult> results = resultRepo.findByRunIdOrderByEmployeeId(run.getId());
            dto.setResults(results.stream().map(this::toDto).toList());
            dto.setEmployeeCount(!results.isEmpty() ? results.size() : eligibleEmployeeCount(run));
            dto.setTotalGross(results.stream().map(PayrollResult::getGross).reduce(BigDecimal.ZERO, BigDecimal::add));
            dto.setTotalNet(results.stream().map(PayrollResult::getNet).reduce(BigDecimal.ZERO, BigDecimal::add));
        } else {
            List<Object[]> summaries = resultRepo.summarizeRun(run.getId());
            Object[] summary = summaries.isEmpty() ? null : summaries.get(0);
            long employeeCount = summary != null && summary.length > 0 ? asLong(summary[0]) : 0;
            dto.setEmployeeCount(employeeCount > 0 ? (int) employeeCount : eligibleEmployeeCount(run));
            dto.setTotalGross(summary != null && summary.length > 1 ? asBigDecimal(summary[1]) : BigDecimal.ZERO);
            dto.setTotalNet(summary != null && summary.length > 2 ? asBigDecimal(summary[2]) : BigDecimal.ZERO);
        }
        return dto;
    }

    private static long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static BigDecimal asBigDecimal(Object value) {
        if (value instanceof BigDecimal b) {
            return b;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private int eligibleEmployeeCount(PayrollRun run) {
        PayrollPeriod period = periodRepo.findById(run.getPeriodId()).orElse(null);
        if (period == null) {
            return 0;
        }
        return timesheetRepo.findPayrollScope(run.getCompanyId(), period.getPeriodYear(), period.getPeriodMonth(),
                run.getProjectId(), normalizePayGroup(run.getPayGroup())).size();
    }

    private PayrollResultDto toDto(PayrollResult result) {
        return toDto(result, employeeRepo.findById(result.getEmployeeId()).orElse(null));
    }

    /** Same mapping, but takes an already-fetched Employee — used by the
     * paginated results endpoint so a page of 25 only costs ONE batch
     * employee query, not 25 individual lookups. */
    private PayrollResultDto toDto(PayrollResult result, Employee employee) {
        PayrollResultDto dto = new PayrollResultDto();
        dto.setId(result.getId());
        dto.setEmployeeId(result.getEmployeeId());
        if (employee != null) {
            dto.setEmployeeNumber(employee.getEmployeeNumber());
            dto.setEmployeeName((employee.getFirstName() + " " + employee.getLastName()).trim());
        }
        dto.setPayStatus(result.getPayStatus());
        dto.setWorkedDays(result.getWorkedDays());
        dto.setNormalHours(result.getNormalHours());
        dto.setOtHours(result.getOtHours());
        dto.setGross(result.getGross());
        dto.setTotalEarnings(result.getTotalEarnings());
        dto.setTotalDeductions(result.getTotalDeductions());
        dto.setNet(result.getNet());
        dto.setStatus(result.getStatus());
        dto.setMessage(result.getMessage());
        dto.setLines(lineRepo.findByResultIdOrderBySortOrderAsc(result.getId()).stream().map(this::toDto).toList());
        return dto;
    }

    private PayrollResultLineDto toDto(PayrollResultLine line) {
        PayrollResultLineDto dto = new PayrollResultLineDto();
        dto.setId(line.getId());
        dto.setComponentCode(line.getComponentCode());
        dto.setComponentName(line.getComponentName());
        dto.setComponentType(line.getComponentType());
        dto.setQuantity(line.getQuantity());
        dto.setRate(line.getRate());
        dto.setAmount(line.getAmount());
        dto.setSource(line.getSource());
        return dto;
    }

    private static BigDecimal z(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String normalizePayGroup(String payGroup) {
        if (payGroup == null || payGroup.isBlank()) {
            return "ALL";
        }
        return payGroup.trim().toUpperCase();
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "system";
        }
        return auth.getName();
    }
}
