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
import com.hrms.payroll.domain.ProvisionRule;
import com.hrms.payroll.domain.ProvisionRun;
import com.hrms.payroll.dto.ProvisionDtos;
import com.hrms.payroll.repository.PayrollComponentRepository;
import com.hrms.payroll.repository.ProvisionResultRepository;
import com.hrms.payroll.repository.ProvisionRuleRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final ProvisionRuleRepository ruleRepo;

    public ProvisionService(ProvisionRunRepository runRepo,
                            ProvisionResultRepository resultRepo,
                            PayrollPeriodRepository periodRepo,
                            ProjectRepository projectRepo,
                            EmployeeRepository employeeRepo,
                            AssignmentRepository assignmentRepo,
                            ContractPayItemRepository payItemRepo,
                            PayrollComponentRepository componentRepo,
                            ProvisionRuleRepository ruleRepo) {
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.periodRepo = periodRepo;
        this.projectRepo = projectRepo;
        this.employeeRepo = employeeRepo;
        this.assignmentRepo = assignmentRepo;
        this.payItemRepo = payItemRepo;
        this.componentRepo = componentRepo;
        this.ruleRepo = ruleRepo;
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
        ProvisionRule rule = ruleRepo.findMatching(companyId, provisionType, request.getProjectId(), payGroup, period.getEndDate())
                .stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("provision.rule.required",
                        "No active provision rule found for " + provisionType + " / " + payGroup + ". Configure Provision Rules first."));
        Map<UUID, PayrollComponent> componentsById = componentRepo.findByCompanyIdOrderByPriority(companyId)
                .stream().collect(Collectors.toMap(PayrollComponent::getId, c -> c, (a, b) -> a));

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
        run.setNotes("Rule: " + rule.getName() + ". Formula: " + rule.getFormulaExpression());
        run = runRepo.save(run);

        BigDecimal totalEligible = BigDecimal.ZERO;
        BigDecimal totalProvision = BigDecimal.ZERO;
        for (Employee employee : employees) {
            BigDecimal eligible = activeEligibleAmount(itemsByEmployee.getOrDefault(employee.getId(), List.of()),
                    componentsById, rule, provisionType, period.getEndDate());
            Map<String, BigDecimal> variables = variables(employee, period, rule, eligible);
            BigDecimal provision = new FormulaEvaluator(rule.getFormulaExpression(), variables).parse();
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
            result.setFormulaNote(formulaNote(rule, variables, provision));
            result.setStatus("OK");
            if (eligible.compareTo(BigDecimal.ZERO) == 0 && !"FIXED_AMOUNT".equalsIgnoreCase(rule.getBasisMode())) {
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

    private BigDecimal activeEligibleAmount(List<ContractPayItem> items, Map<UUID, PayrollComponent> componentsById,
                                            ProvisionRule rule, String provisionType, LocalDate asOf) {
        if ("FIXED_AMOUNT".equalsIgnoreCase(rule.getBasisMode())) {
            return nz(rule.getFixedAmount(), BigDecimal.ZERO);
        }
        BigDecimal total = BigDecimal.ZERO;
        Set<UUID> seenComponentIds = new HashSet<>();
        Set<String> categorySet = csv(rule.getBasisCategories());
        Set<String> codeSet = csv(rule.getBasisComponentCodes());
        for (ContractPayItem item : items) {
            PayrollComponent component = componentsById.get(item.getPayComponentId());
            if (component == null || !componentIncluded(component, rule, provisionType, categorySet, codeSet)) continue;
            if (seenComponentIds.contains(item.getPayComponentId())) continue;
            if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) continue;
            if (item.getEffectiveFrom().isAfter(asOf)) continue;
            if (item.getEffectiveTo() != null && item.getEffectiveTo().isBefore(asOf)) continue;
            total = total.add(item.getAmount() == null ? BigDecimal.ZERO : item.getAmount());
            seenComponentIds.add(item.getPayComponentId());
        }
        return total;
    }

    private boolean componentIncluded(PayrollComponent component, ProvisionRule rule, String type,
                                      Set<String> categories, Set<String> codes) {
        if (!"ACTIVE".equalsIgnoreCase(component.getStatus())) return false;
        String mode = rule.getBasisMode() == null ? "COMPONENT_FLAGS" : rule.getBasisMode().toUpperCase();
        return switch (mode) {
            case "COMPONENT_CATEGORIES" -> categories.contains(component.getCategory().toUpperCase());
            case "COMPONENT_CODES" -> codes.contains(component.getCode().toUpperCase());
            default -> switch (type) {
                case "LEAVE" -> component.isLeaveIncluded() || component.isProvisionIncluded();
                case "EOS" -> component.isEosIncluded() || component.isProvisionIncluded();
                default -> component.isProvisionIncluded();
            };
        };
    }

    private Map<String, BigDecimal> variables(Employee employee, PayrollPeriod period, ProvisionRule rule, BigDecimal basisAmount) {
        BigDecimal serviceMonths = BigDecimal.valueOf(Math.max(0, ChronoUnit.MONTHS.between(
                employee.getHireDate().withDayOfMonth(1), period.getEndDate().withDayOfMonth(1)) + 1));
        BigDecimal serviceYears = serviceMonths.divide(BigDecimal.valueOf(12), 6, RoundingMode.HALF_UP);
        BigDecimal entitlementDays = serviceYears.compareTo(BigDecimal.valueOf(5)) >= 0
                ? nz(rule.getEntitlementDaysFiveOrMore(), BigDecimal.valueOf(28))
                : nz(rule.getEntitlementDaysUnderFive(), BigDecimal.valueOf(21));
        Map<String, BigDecimal> vars = new LinkedHashMap<>();
        vars.put("basis_amount", scale4(basisAmount));
        vars.put("fixed_amount", scale4(rule.getFixedAmount()));
        vars.put("divisor", scale4(rule.getDivisor()));
        vars.put("entitlement_days", scale4(entitlementDays));
        vars.put("service_years", scale4(serviceYears));
        vars.put("service_months", scale4(serviceMonths));
        vars.put("period_days", BigDecimal.valueOf(ChronoUnit.DAYS.between(period.getStartDate(), period.getEndDate()) + 1));
        vars.put("month_days", BigDecimal.valueOf(period.getEndDate().lengthOfMonth()));
        vars.put("ticket_cycle_months", BigDecimal.valueOf(Math.max(1, rule.getTicketCycleMonths())));
        return vars;
    }

    private String formulaNote(ProvisionRule rule, Map<String, BigDecimal> variables, BigDecimal result) {
        String values = variables.entrySet().stream()
                .map(e -> e.getKey() + "=" + scale(e.getValue()))
                .collect(Collectors.joining(", "));
        return rule.getName() + ": " + rule.getFormulaExpression() + " => " + scale(result) + " [" + values + "]";
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

    private Set<String> csv(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim).filter(s -> !s.isBlank()).map(String::toUpperCase).collect(Collectors.toSet());
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

    private BigDecimal scale4(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private String employeeName(Employee employee) {
        return (employee.getFirstName() + " " + (employee.getMiddleName() == null ? "" : employee.getMiddleName() + " ")
                + employee.getLastName()).trim().replaceAll("\\s+", " ");
    }

    private static class FormulaEvaluator {
        private final String expression;
        private final Map<String, BigDecimal> variables;
        private int pos;

        FormulaEvaluator(String expression, Map<String, BigDecimal> variables) {
            this.expression = expression == null ? "" : expression;
            this.variables = variables;
        }

        BigDecimal parse() {
            BigDecimal value = expression();
            skipWs();
            if (pos != expression.length()) {
                throw new BusinessRuleException("provision.formula.invalid", "Invalid formula near: " + expression.substring(pos));
            }
            return value.setScale(4, RoundingMode.HALF_UP);
        }

        private BigDecimal expression() {
            BigDecimal value = term();
            while (true) {
                skipWs();
                if (match('+')) value = value.add(term());
                else if (match('-')) value = value.subtract(term());
                else return value;
            }
        }

        private BigDecimal term() {
            BigDecimal value = factor();
            while (true) {
                skipWs();
                if (match('*')) value = value.multiply(factor());
                else if (match('/')) {
                    BigDecimal divisor = factor();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw new BusinessRuleException("provision.formula.divide_by_zero", "Provision formula divides by zero.");
                    }
                    value = value.divide(divisor, 8, RoundingMode.HALF_UP);
                } else return value;
            }
        }

        private BigDecimal factor() {
            skipWs();
            if (match('+')) return factor();
            if (match('-')) return factor().negate();
            if (match('(')) {
                BigDecimal value = expression();
                if (!match(')')) throw new BusinessRuleException("provision.formula.invalid", "Missing ')' in provision formula.");
                return value;
            }
            if (pos < expression.length() && (Character.isDigit(expression.charAt(pos)) || expression.charAt(pos) == '.')) {
                return number();
            }
            return variable();
        }

        private BigDecimal number() {
            int start = pos;
            while (pos < expression.length() && (Character.isDigit(expression.charAt(pos)) || expression.charAt(pos) == '.')) pos++;
            return new BigDecimal(expression.substring(start, pos));
        }

        private BigDecimal variable() {
            int start = pos;
            while (pos < expression.length()) {
                char c = expression.charAt(pos);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.') pos++;
                else break;
            }
            if (start == pos) throw new BusinessRuleException("provision.formula.invalid", "Invalid provision formula.");
            String name = expression.substring(start, pos);
            BigDecimal value = variables.get(name);
            if (value == null) value = variables.get(name.toLowerCase());
            if (value == null) throw new BusinessRuleException("provision.formula.variable", "Unknown provision variable: " + name);
            return value;
        }

        private boolean match(char c) {
            skipWs();
            if (pos < expression.length() && expression.charAt(pos) == c) {
                pos++;
                return true;
            }
            return false;
        }

        private void skipWs() {
            while (pos < expression.length() && Character.isWhitespace(expression.charAt(pos))) pos++;
        }
    }
}
