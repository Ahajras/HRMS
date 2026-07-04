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
import com.hrms.payroll.domain.PayrollResult;
import com.hrms.payroll.domain.PayrollResultLine;
import com.hrms.payroll.domain.PayrollRule;
import com.hrms.payroll.domain.PayrollRun;
import com.hrms.payroll.dto.PayrollResultDto;
import com.hrms.payroll.dto.PayrollResultLineDto;
import com.hrms.payroll.dto.PayrollRunDto;
import com.hrms.payroll.repository.PayrollComponentRepository;
import com.hrms.payroll.repository.PayrollResultLineRepository;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.payroll.repository.PayrollRuleRepository;
import com.hrms.payroll.repository.PayrollRunRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.domain.PayrollPeriodProject;
import com.hrms.timesheet.domain.TimeType;
import com.hrms.timesheet.domain.Timesheet;
import com.hrms.timesheet.domain.TimesheetDay;
import com.hrms.timesheet.domain.TimeTypePayrollRule;
import com.hrms.timesheet.repository.PayrollPeriodProjectRepository;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import com.hrms.timesheet.repository.TimeTypeRepository;
import com.hrms.timesheet.repository.TimeTypePayrollRuleRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import com.hrms.timesheet.repository.TimesheetRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TimeTypeRepository timeTypeRepo;
    private final TimeTypePayrollRuleRepository timeTypePayrollRuleRepo;
    private final EmployeeRepository employeeRepo;
    private final AssignmentRepository assignmentRepo;
    private final ContractPayItemRepository payItemRepo;
    private final PayrollComponentRepository componentRepo;
    private final PayrollRuleRepository ruleRepo;

    public PayrollRunService(PayrollRunRepository runRepo,
                             PayrollResultRepository resultRepo,
                             PayrollResultLineRepository lineRepo,
                             PayrollPeriodRepository periodRepo,
                             PayrollPeriodProjectRepository periodProjectRepo,
                             TimesheetRepository timesheetRepo,
                             TimesheetDayRepository dayRepo,
                             TimeTypeRepository timeTypeRepo,
                             TimeTypePayrollRuleRepository timeTypePayrollRuleRepo,
                             EmployeeRepository employeeRepo,
                             AssignmentRepository assignmentRepo,
                             ContractPayItemRepository payItemRepo,
                             PayrollComponentRepository componentRepo,
                             PayrollRuleRepository ruleRepo) {
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.lineRepo = lineRepo;
        this.periodRepo = periodRepo;
        this.periodProjectRepo = periodProjectRepo;
        this.timesheetRepo = timesheetRepo;
        this.dayRepo = dayRepo;
        this.timeTypeRepo = timeTypeRepo;
        this.timeTypePayrollRuleRepo = timeTypePayrollRuleRepo;
        this.employeeRepo = employeeRepo;
        this.assignmentRepo = assignmentRepo;
        this.payItemRepo = payItemRepo;
        this.componentRepo = componentRepo;
        this.ruleRepo = ruleRepo;
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
        return toDto(getEntity(id), true);
    }

    public PayrollRunDto create(UUID periodId, UUID projectId, String payGroup) {
        UUID companyId = TenantContext.requireCompanyId();
        periodRepo.findById(periodId).orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodId));
        String normalizedPayGroup = normalizePayGroup(payGroup);
        List<PayrollRun> existing = runRepo.findExistingScope(companyId, periodId, projectId, normalizedPayGroup);
        if (!existing.isEmpty()) {
            return toDto(existing.get(0), true);
        }
        PayrollRun run = new PayrollRun();
        run.setCompanyId(companyId);
        run.setPeriodId(periodId);
        run.setProjectId(projectId);
        run.setPayGroup(normalizedPayGroup);
        run.setRunType("REGULAR");
        run.setStatus(DRAFT);
        return toDto(runRepo.save(run), true);
    }

    public PayrollRunDto calculate(UUID id) {
        PayrollRun run = getEntity(id);
        if (!DRAFT.equals(run.getStatus()) && !CALCULATED.equals(run.getStatus())) {
            throw new BusinessRuleException("payroll.run.state", "Only a DRAFT/CALCULATED payroll run can be recalculated.");
        }
        PayrollPeriod period = periodRepo.findById(run.getPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + run.getPeriodId()));
        assertPayrollScopeLocked(run);
        resultRepo.deleteByRunId(run.getId());
        resultRepo.flush();

        Map<UUID, PayrollComponent> components = new HashMap<>();
        componentRepo.findByCompanyIdOrderByPriority(run.getCompanyId()).forEach(c -> components.put(c.getId(), c));

        for (Timesheet ts : timesheetRepo.findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(
                run.getCompanyId(), period.getPeriodYear(), period.getPeriodMonth())) {
            if (!"APPROVED".equals(ts.getStatus()) && !"LOCKED".equals(ts.getStatus())) {
                continue;
            }
            Employee emp = employeeRepo.findById(ts.getEmployeeId()).orElse(null);
            if (emp == null || !matchesScope(emp, run)) {
                continue;
            }
            UUID empProject = run.getProjectId() != null ? run.getProjectId() : employeeProject(emp.getId());
            PayrollRule rule = payrollRule(run.getCompanyId(), empProject, emp.getPayStatus());
            PayableBreakdown breakdown = payableBreakdown(ts, rule);
            PayrollPolicyContext policy = payrollPolicyContext(run.getCompanyId(), ts, rule);
            PayrollResult result = buildResult(run, ts, emp, rule, breakdown);
            result = resultRepo.save(result);
            BigDecimal earnings = BigDecimal.ZERO;
            BigDecimal deductions = BigDecimal.ZERO;
            for (ContractPayItem item : activePayItems(emp.getId(), period.getEndDate())) {
                PayrollComponent component = components.get(item.getPayComponentId());
                if (component == null) {
                    continue;
                }
                for (PayrollResultLine line : buildLines(run.getCompanyId(), result.getId(), item, component, result, rule, breakdown, policy)) {
                    lineRepo.save(line);
                    if ("DEDUCTION".equalsIgnoreCase(line.getComponentType())) {
                        deductions = deductions.add(line.getAmount());
                    } else {
                        earnings = earnings.add(line.getAmount());
                    }
                }
            }
            for (PayrollResultLine otLine : buildOvertimeLines(run.getCompanyId(), result.getId(), result, rule, breakdown)) {
                lineRepo.save(otLine);
                earnings = earnings.add(otLine.getAmount());
            }
            PayrollResultLine unpaidDeduction = buildMonthlyUnpaidDeductionLine(run.getCompanyId(), result.getId(), earnings, rule, breakdown, policy);
            if (unpaidDeduction != null) {
                lineRepo.save(unpaidDeduction);
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
            resultRepo.save(result);
        }
        run.setStatus(CALCULATED);
        run.setCalculatedAt(Instant.now());
        return toDto(runRepo.save(run), true);
    }

    public PayrollRunDto approve(UUID id) {
        PayrollRun run = getEntity(id);
        if (!CALCULATED.equals(run.getStatus())) {
            throw new BusinessRuleException("payroll.run.approve.state", "Only a CALCULATED payroll run can be approved.");
        }
        run.setStatus("APPROVED");
        run.setApprovedAt(Instant.now());
        run.setApprovedBy(currentUsername());
        return toDto(runRepo.save(run), true);
    }

    public PayrollRunDto lock(UUID id) {
        PayrollRun run = getEntity(id);
        if (!"APPROVED".equals(run.getStatus())) {
            throw new BusinessRuleException("payroll.run.lock.state", "Only an APPROVED payroll run can be locked.");
        }
        run.setStatus("LOCKED");
        run.setLockedAt(Instant.now());
        return toDto(runRepo.save(run), true);
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
                                               PayrollPolicyContext policy) {
        ComponentPolicyBreakdown policyBreakdown = componentPolicyBreakdown(component, item, rule, policy);
        // Monthly base salary is always paid as the fixed full amount (paid leave counts
        // like a normal day). Time-type rules apply only as DEDUCTIONS on top of it,
        // instead of replacing the whole base calculation.
        boolean monthlyBase = isSalaryComponent(component) && isPayItemEarning(component) && !isDailyRule(rule);
        if (policyBreakdown.hasAny() && !monthlyBase) {
            return buildPolicyLines(companyId, resultId, component, item, result, rule, policyBreakdown);
        }
        if (isPayItemEarning(component)) {
            BigDecimal rate = z(item.getAmount());
            if (isSalaryComponent(component) && result.getDailyRate().compareTo(BigDecimal.ZERO) == 0) {
                if (isDailyRule(rule)) {
                    result.setDailyRate(rate);
                    result.setHourlyRate(rate.divide(safeHours(rule), 4, RoundingMode.HALF_UP));
                } else {
                    BigDecimal hourly = rate.divide(safeMonthDivisor(rule).multiply(safeHours(rule)), 4, RoundingMode.HALF_UP);
                    result.setHourlyRate(hourly);
                    result.setDailyRate(hourly.multiply(safeHours(rule)));
                }
            }
            if (!isDailyRule(rule)) {
                BigDecimal baseHours = safeMonthDivisor(rule).multiply(safeHours(rule));
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
                        lines.add(manualLine(companyId, resultId, component.getCode(),
                                component.getName() + " - Time type deduction", "DEDUCTION", component.getCategory(),
                                deductQty, hourly, round(hourly.multiply(deductQty)),
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
            result.setHourlyRate(dailyRate.divide(safeHours(rule), 4, RoundingMode.HALF_UP));
        }
        return List.of(line);
    }

    private List<PayrollResultLine> buildPolicyLines(UUID companyId, UUID resultId, PayrollComponent component,
                                                     ContractPayItem item, PayrollResult result, PayrollRule rule,
                                                     ComponentPolicyBreakdown breakdown) {
        BigDecimal amount = z(item.getAmount());
        BigDecimal unitRate = unitRate(amount, rule, breakdown.basis());
        if (isSalaryComponent(component) && result.getDailyRate().compareTo(BigDecimal.ZERO) == 0) {
            if (isDailyRule(rule)) {
                result.setDailyRate(amount);
                result.setHourlyRate(amount.divide(safeHours(rule), 4, RoundingMode.HALF_UP));
            } else {
                BigDecimal hourly = amount.divide(safeMonthDivisor(rule).multiply(safeHours(rule)), 4, RoundingMode.HALF_UP);
                result.setHourlyRate(hourly);
                result.setDailyRate(amount.divide(safeMonthDivisor(rule), 4, RoundingMode.HALF_UP));
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

    private PayrollPolicyContext payrollPolicyContext(UUID companyId, Timesheet ts, PayrollRule rule) {
        List<TimesheetDay> days = dayRepo.findByTimesheetIdOrderByWorkDate(ts.getId());
        Set<UUID> timeTypeIds = new HashSet<>();
        for (TimesheetDay day : days) {
            if (day.getTimeTypeId() != null) {
                timeTypeIds.add(day.getTimeTypeId());
            }
        }
        Map<UUID, TimeType> types = new HashMap<>();
        for (UUID timeTypeId : timeTypeIds) {
            timeTypeRepo.findById(timeTypeId).ifPresent(type -> types.put(timeTypeId, type));
        }
        Map<UUID, Map<UUID, TimeTypePayrollRule>> rulesByTimeType = new HashMap<>();
        if (!timeTypeIds.isEmpty()) {
            for (TimeTypePayrollRule ruleRow : timeTypePayrollRuleRepo.findByCompanyIdAndTimeTypeIdIn(companyId, timeTypeIds)) {
                rulesByTimeType.computeIfAbsent(ruleRow.getTimeTypeId(), ignored -> new HashMap<>())
                        .put(ruleRow.getPayrollComponentId(), ruleRow);
            }
        }
        Map<UUID, Integer> consecutivePos = new HashMap<>();
        Map<UUID, Integer> annualPos = new HashMap<>();
        computeThresholdPositions(days, types, consecutivePos, annualPos);
        return new PayrollPolicyContext(days, types, rulesByTimeType, rule, consecutivePos, annualPos);
    }

    /**
     * For each day, record its position in the running count of its own time type:
     *  - consecutivePos: length of the current unbroken spell (REST/HOLIDAY days
     *    bridge the spell and are not counted; any other time type breaks it).
     *  - annualPos: cumulative count of that time type within the period.
     * These feed the "apply effect only after N days" thresholds. (Annual counting
     * is within the current period for now; cross-period carry-in is a follow-up.)
     */
    private void computeThresholdPositions(List<TimesheetDay> days, Map<UUID, TimeType> types,
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
            annualPos.put(d.getId(), annual.merge(tt, 1, Integer::sum));
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
                                                              PayrollRule rule, PayrollPolicyContext policy) {
        BigDecimal payQty = BigDecimal.ZERO;
        BigDecimal deductQty = BigDecimal.ZERO;
        BigDecimal legacyPayHours = BigDecimal.ZERO;
        String basis = "HOURS";
        boolean touched = false;
        for (TimesheetDay day : policy.days()) {
            TimeType type = day.getTimeTypeId() != null ? policy.types().get(day.getTimeTypeId()) : null;
            Map<UUID, TimeTypePayrollRule> byComponent = day.getTimeTypeId() != null ? policy.rulesByTimeType().get(day.getTimeTypeId()) : null;
            TimeTypePayrollRule explicit = byComponent != null ? byComponent.get(item.getPayComponentId()) : null;
            DayEffect effect = explicit != null
                    ? explicitEffect(explicit, day, type, rule)
                    : legacyEffect(type, day, rule);
            if (!effect.hasAny()) {
                continue;
            }
            // Threshold gate: for a rule with a threshold, the first N qualifying
            // days are protected — the effect applies only from day N+1 onward.
            if (explicit != null && explicit.getThresholdDays() > 0
                    && !"NONE".equalsIgnoreCase(explicit.getThresholdScope())) {
                Integer pos = "ANNUAL".equalsIgnoreCase(explicit.getThresholdScope())
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
        // Monthly: an allowance paid per present day is normalised to the divisor, so it
        // is the same every month (divisor × shift-hours) regardless of 28/30/31 days.
        // Unpaid days still reduce it through the deduction quantity.
        if (!isDailyRule(rule) && legacyPayHours.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisorFull = safeMonthDivisor(rule).multiply(safeHours(rule));
            payQty = payQty.subtract(legacyPayHours).add(divisorFull);
        }
        return new ComponentPolicyBreakdown(payQty, deductQty, basis, touched);
    }

    private DayEffect explicitEffect(TimeTypePayrollRule explicit, TimesheetDay day, TimeType type, PayrollRule rule) {
        BigDecimal baseQuantity = quantityForBasis(day, explicit.getBasis(), rule);
        BigDecimal scaled = baseQuantity.multiply(z(explicit.getPercent()).divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        if ("DEDUCT".equalsIgnoreCase(explicit.getAction())) {
            DayEffect legacy = legacyEffect(type, day, rule);
            return new DayEffect(legacy.payQuantity(), scaled, explicit.getBasis());
        }
        if ("PAY".equalsIgnoreCase(explicit.getAction())) {
            return new DayEffect(scaled, BigDecimal.ZERO, explicit.getBasis());
        }
        return DayEffect.none();
    }

    private DayEffect legacyEffect(TimeType type, TimesheetDay day, PayrollRule rule) {
        String category = type != null ? type.getCategory() : "REGULAR";
        boolean rest = "REST".equalsIgnoreCase(category) || "HOLIDAY".equalsIgnoreCase(category);
        boolean daily = isDailyRule(rule);
        BigDecimal standardHours = safeHours(rule);
        if (rest) {
            if (!daily && (type == null || type.isPaid() || rule.isWeeklyRestPaid())) {
                return new DayEffect(restHours(day, standardHours), BigDecimal.ZERO, "HOURS");
            }
            return DayEffect.none();
        }
        if (isUnpaidCategory(category) || (type != null && !type.isPaid())) {
            return new DayEffect(BigDecimal.ZERO, payrollHours(day, standardHours), "HOURS");
        }
        if (type == null || type.isPaid()) {
            return new DayEffect(payrollHours(day, standardHours), BigDecimal.ZERO, "HOURS");
        }
        return DayEffect.none();
    }

    private BigDecimal quantityForBasis(TimesheetDay day, String basis, PayrollRule rule) {
        String normalized = basis == null ? "HOURS" : basis.toUpperCase();
        BigDecimal standardHours = safeHours(rule);
        if ("DAYS".equals(normalized) || "FIXED".equals(normalized)) {
            BigDecimal hours = payrollHours(day, standardHours);
            if (hours.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            return hours.divide(standardHours, 4, RoundingMode.HALF_UP);
        }
        return payrollHours(day, standardHours);
    }

    private BigDecimal unitRate(BigDecimal amount, PayrollRule rule, String basis) {
        String normalized = basis == null ? "HOURS" : basis.toUpperCase();
        if ("DAYS".equals(normalized) || "FIXED".equals(normalized)) {
            return isDailyRule(rule)
                    ? amount
                    : amount.divide(safeMonthDivisor(rule), 4, RoundingMode.HALF_UP);
        }
        return isDailyRule(rule)
                ? amount.divide(safeHours(rule), 4, RoundingMode.HALF_UP)
                : amount.divide(safeMonthDivisor(rule).multiply(safeHours(rule)), 4, RoundingMode.HALF_UP);
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
        // Project-specific rule first, then the company-wide default (project_id NULL).
        if (projectId != null) {
            Optional<PayrollRule> projectRule =
                    ruleRepo.findByCompanyIdAndProjectIdAndPayGroupAndStatus(companyId, projectId, group, "ACTIVE");
            if (projectRule.isPresent()) {
                return projectRule.get();
            }
        }
        return ruleRepo.findDefaultByCompanyIdAndPayGroupAndStatus(companyId, group, "ACTIVE")
                .orElseGet(() -> defaultRule(companyId, group));
    }

    private PayrollRule defaultRule(UUID companyId, String group) {
        PayrollRule rule = new PayrollRule();
        rule.setCompanyId(companyId);
        rule.setPayGroup(group);
        rule.setPayItemBasis("DAILY".equals(group) ? "DAILY_RATE" : "FIXED_AMOUNT");
        rule.setOtMultiplier(new BigDecimal("1.2500"));
        rule.setRestDayOtMultiplier(new BigDecimal("1.5000"));
        rule.setStandardHoursPerDay(new BigDecimal("8.00"));
        rule.setWeeklyRestPaid(true);
        return rule;
    }

    private PayableBreakdown payableBreakdown(Timesheet ts, PayrollRule rule) {
        BigDecimal standardHours = z(rule.getStandardHoursPerDay()).compareTo(BigDecimal.ZERO) > 0
                ? rule.getStandardHoursPerDay() : new BigDecimal("8.00");
        BigDecimal regularPaidDays = BigDecimal.ZERO;
        BigDecimal restPaidDays = BigDecimal.ZERO;
        BigDecimal unpaidDays = BigDecimal.ZERO;
        BigDecimal regularPaidHours = BigDecimal.ZERO;
        BigDecimal restPaidHours = BigDecimal.ZERO;
        BigDecimal paidHours = BigDecimal.ZERO;
        BigDecimal unpaidHours = BigDecimal.ZERO;
        BigDecimal normalOtHours = BigDecimal.ZERO;
        BigDecimal restOtHours = BigDecimal.ZERO;
        Map<UUID, TimeType> types = new HashMap<>();
        boolean daily = isDailyRule(rule);
        for (TimesheetDay day : dayRepo.findByTimesheetIdOrderByWorkDate(ts.getId())) {
            TimeType type = day.getTimeTypeId() == null ? null
                    : types.computeIfAbsent(day.getTimeTypeId(), id -> timeTypeRepo.findById(id).orElse(null));
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

    private static BigDecimal safeHours(PayrollRule rule) {
        BigDecimal hours = rule != null ? z(rule.getStandardHoursPerDay()) : BigDecimal.ZERO;
        return hours.compareTo(BigDecimal.ZERO) > 0 ? hours : new BigDecimal("8.00");
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

    private record DayEffect(BigDecimal payQuantity, BigDecimal deductQuantity, String basis) {
        static DayEffect none() {
            return new DayEffect(BigDecimal.ZERO, BigDecimal.ZERO, "HOURS");
        }

        boolean hasAny() {
            return z(payQuantity).compareTo(BigDecimal.ZERO) > 0 || z(deductQuantity).compareTo(BigDecimal.ZERO) > 0;
        }
    }

    private List<ContractPayItem> activePayItems(UUID employeeId, LocalDate asOf) {
        return payItemRepo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId).stream()
                .filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus()))
                .filter(i -> i.getEffectiveFrom() == null || !i.getEffectiveFrom().isAfter(asOf))
                .filter(i -> i.getEffectiveTo() == null || !i.getEffectiveTo().isBefore(asOf))
                .toList();
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
        List<PayrollResult> results = resultRepo.findByRunIdOrderByEmployeeId(run.getId());
        if (includeResults) {
            dto.setResults(results.stream().map(this::toDto).toList());
        }
        dto.setEmployeeCount(!results.isEmpty() ? results.size() : eligibleEmployeeCount(run));
        dto.setTotalGross(results.stream().map(PayrollResult::getGross).reduce(BigDecimal.ZERO, BigDecimal::add));
        dto.setTotalNet(results.stream().map(PayrollResult::getNet).reduce(BigDecimal.ZERO, BigDecimal::add));
        return dto;
    }

    private int eligibleEmployeeCount(PayrollRun run) {
        PayrollPeriod period = periodRepo.findById(run.getPeriodId()).orElse(null);
        if (period == null) {
            return 0;
        }
        int count = 0;
        for (Timesheet ts : timesheetRepo.findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(
                run.getCompanyId(), period.getPeriodYear(), period.getPeriodMonth())) {
            if (!"APPROVED".equals(ts.getStatus()) && !"LOCKED".equals(ts.getStatus())) {
                continue;
            }
            Employee emp = employeeRepo.findById(ts.getEmployeeId()).orElse(null);
            if (emp != null && matchesScope(emp, run)) {
                count++;
            }
        }
        return count;
    }

    private PayrollResultDto toDto(PayrollResult result) {
        PayrollResultDto dto = new PayrollResultDto();
        dto.setId(result.getId());
        dto.setEmployeeId(result.getEmployeeId());
        employeeRepo.findById(result.getEmployeeId()).ifPresent(e -> {
            dto.setEmployeeNumber(e.getEmployeeNumber());
            dto.setEmployeeName((e.getFirstName() + " " + e.getLastName()).trim());
        });
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
        String p = payGroup.trim().toUpperCase();
        if (p.contains("DAILY")) return "DAILY";
        if (p.contains("MONTH")) return "MONTHLY";
        return "ALL";
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "system";
        }
        return auth.getName();
    }
}
