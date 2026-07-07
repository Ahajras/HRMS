package com.hrms.payroll.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.domain.PayrollResult;
import com.hrms.payroll.domain.PayrollRun;
import com.hrms.payroll.dto.CostCodeLineDto;
import com.hrms.payroll.dto.EmployeeCostBreakdownDto;
import com.hrms.payroll.dto.PayrollCostReportDto;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.payroll.repository.PayrollRunRepository;
import com.hrms.project.domain.CostCode;
import com.hrms.project.domain.Project;
import com.hrms.project.repository.CostCodeRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.Timesheet;
import com.hrms.timesheet.domain.TimesheetDay;
import com.hrms.timesheet.domain.TimesheetDayCost;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import com.hrms.timesheet.repository.TimesheetDayCostRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import com.hrms.timesheet.repository.TimesheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the cost-code allocation report for a payroll run, in both
 * directions (per employee, and per cost code). Reuses each employee's
 * hourly rate already computed on their PayrollResult, and the day-level
 * cost splits already recorded on the timesheet — no double bookkeeping.
 *
 * Every lookup here is a BATCH query (fetch-all-then-map-in-memory) rather
 * than one query per employee/day — the N+1 pattern that made bulk timesheet
 * operations crawl earlier is deliberately avoided from the start here.
 */
@Service
@Transactional(readOnly = true)
public class PayrollCostReportService {

    private final PayrollRunRepository runRepo;
    private final PayrollResultRepository resultRepo;
    private final PayrollPeriodRepository periodRepo;
    private final TimesheetRepository timesheetRepo;
    private final TimesheetDayRepository dayRepo;
    private final TimesheetDayCostRepository dayCostRepo;
    private final ProjectRepository projectRepo;
    private final CostCodeRepository costCodeRepo;
    private final EmployeeRepository employeeRepo;

    public PayrollCostReportService(PayrollRunRepository runRepo, PayrollResultRepository resultRepo,
                                    PayrollPeriodRepository periodRepo, TimesheetRepository timesheetRepo,
                                    TimesheetDayRepository dayRepo, TimesheetDayCostRepository dayCostRepo,
                                    ProjectRepository projectRepo, CostCodeRepository costCodeRepo,
                                    EmployeeRepository employeeRepo) {
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.periodRepo = periodRepo;
        this.timesheetRepo = timesheetRepo;
        this.dayRepo = dayRepo;
        this.dayCostRepo = dayCostRepo;
        this.projectRepo = projectRepo;
        this.costCodeRepo = costCodeRepo;
        this.employeeRepo = employeeRepo;
    }

    public PayrollCostReportDto build(UUID runId) {
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + runId));
        PayrollPeriod period = periodRepo.findById(run.getPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + run.getPeriodId()));
        List<PayrollResult> results = resultRepo.findByRunIdOrderByEmployeeId(runId);

        // 1) all employees in one query
        List<UUID> employeeIds = results.stream().map(PayrollResult::getEmployeeId).toList();
        Map<UUID, Employee> employeeById = employeeRepo.findAllById(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));

        // 2) all timesheets for the period in one query, mapped by employee
        Map<UUID, Timesheet> timesheetByEmployee = timesheetRepo
                .findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(
                        run.getCompanyId(), period.getPeriodYear(), period.getPeriodMonth())
                .stream()
                .collect(Collectors.toMap(Timesheet::getEmployeeId, t -> t, (a, b) -> a));

        // 3) all days for those timesheets in one query
        List<UUID> timesheetIds = timesheetByEmployee.values().stream().map(Timesheet::getId).toList();
        List<TimesheetDay> allDays = dayRepo.findByTimesheetIdInOrderByTimesheetIdAscWorkDateAsc(timesheetIds);
        Map<UUID, List<TimesheetDay>> daysByTimesheet = allDays.stream()
                .collect(Collectors.groupingBy(TimesheetDay::getTimesheetId));

        // 4) all cost splits for those days in one query
        List<UUID> dayIds = allDays.stream().map(TimesheetDay::getId).toList();
        Map<UUID, List<TimesheetDayCost>> costsByDay = dayCostRepo.findByTimesheetDayIdIn(dayIds).stream()
                .collect(Collectors.groupingBy(TimesheetDayCost::getTimesheetDayId));

        // 5) all referenced projects / cost codes in one query each, for display names
        Map<UUID, Project> projectById = new LinkedHashMap<>();
        Map<UUID, CostCode> costCodeById = new LinkedHashMap<>();
        collectProjectAndCostCodeIds(allDays, costsByDay, projectById, costCodeById);
        if (!projectById.isEmpty()) {
            projectRepo.findAllById(projectById.keySet()).forEach(p -> projectById.put(p.getId(), p));
        }
        if (!costCodeById.isEmpty()) {
            costCodeRepo.findAllById(costCodeById.keySet()).forEach(c -> costCodeById.put(c.getId(), c));
        }

        // 6) build the per-employee breakdown, accumulating the per-cost-code aggregate as we go
        Map<String, CostCodeLineDto> aggregate = new LinkedHashMap<>();
        List<EmployeeCostBreakdownDto> byEmployee = new ArrayList<>();
        for (PayrollResult result : results) {
            Timesheet ts = timesheetByEmployee.get(result.getEmployeeId());
            List<TimesheetDay> days = ts != null ? daysByTimesheet.getOrDefault(ts.getId(), List.of()) : List.of();
            Map<String, BigDecimal> hoursByKey = new LinkedHashMap<>();
            for (TimesheetDay day : days) {
                List<TimesheetDayCost> costs = costsByDay.get(day.getId());
                if (costs == null || costs.isEmpty()) {
                    if (day.getProjectId() != null || day.getCostCodeId() != null) {
                        String key = key(day.getProjectId(), day.getCostCodeId());
                        hoursByKey.merge(key, z(day.getWorkedHours()), BigDecimal::add);
                    }
                } else {
                    for (TimesheetDayCost c : costs) {
                        String key = key(c.getProjectId(), c.getCostCodeId());
                        hoursByKey.merge(key, z(c.getHours()), BigDecimal::add);
                    }
                }
            }
            BigDecimal hourlyRate = z(result.getHourlyRate());
            List<CostCodeLineDto> lines = new ArrayList<>();
            BigDecimal empHours = BigDecimal.ZERO;
            BigDecimal empValue = BigDecimal.ZERO;
            for (Map.Entry<String, BigDecimal> e : hoursByKey.entrySet()) {
                UUID[] ids = parseKey(e.getKey());
                BigDecimal hours = e.getValue();
                BigDecimal value = hours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
                CostCodeLineDto line = newLine(ids[0], ids[1], projectById, costCodeById);
                line.add(hours, value);
                lines.add(line);
                empHours = empHours.add(hours);
                empValue = empValue.add(value);
                aggregate.computeIfAbsent(e.getKey(), k -> newLine(ids[0], ids[1], projectById, costCodeById))
                        .add(hours, value);
            }
            Employee emp = employeeById.get(result.getEmployeeId());
            String empNumber = emp != null ? emp.getEmployeeNumber() : "";
            String empName = emp != null ? (nzs(emp.getFirstName()) + " " + nzs(emp.getLastName())).trim() : "";
            byEmployee.add(new EmployeeCostBreakdownDto(result.getEmployeeId(), empNumber, empName, lines, empHours, empValue));
        }

        return new PayrollCostReportDto(runId, byEmployee, new ArrayList<>(aggregate.values()));
    }

    private void collectProjectAndCostCodeIds(List<TimesheetDay> days, Map<UUID, List<TimesheetDayCost>> costsByDay,
                                              Map<UUID, Project> projectById, Map<UUID, CostCode> costCodeById) {
        for (TimesheetDay day : days) {
            List<TimesheetDayCost> costs = costsByDay.get(day.getId());
            if (costs == null || costs.isEmpty()) {
                if (day.getProjectId() != null) projectById.putIfAbsent(day.getProjectId(), null);
                if (day.getCostCodeId() != null) costCodeById.putIfAbsent(day.getCostCodeId(), null);
            } else {
                for (TimesheetDayCost c : costs) {
                    if (c.getProjectId() != null) projectById.putIfAbsent(c.getProjectId(), null);
                    if (c.getCostCodeId() != null) costCodeById.putIfAbsent(c.getCostCodeId(), null);
                }
            }
        }
    }

    private CostCodeLineDto newLine(UUID projectId, UUID costCodeId,
                                    Map<UUID, Project> projectById, Map<UUID, CostCode> costCodeById) {
        Project p = projectId != null ? projectById.get(projectId) : null;
        CostCode c = costCodeId != null ? costCodeById.get(costCodeId) : null;
        return new CostCodeLineDto(
                projectId, p != null ? p.getCode() : null, p != null ? p.getName() : "Unassigned",
                costCodeId, c != null ? c.getCode() : null, c != null ? c.getName() : "Unassigned");
    }

    private String key(UUID projectId, UUID costCodeId) {
        return projectId + "|" + costCodeId;
    }

    private UUID[] parseKey(String key) {
        String[] parts = key.split("\\|", -1);
        return new UUID[]{
                "null".equals(parts[0]) ? null : UUID.fromString(parts[0]),
                "null".equals(parts[1]) ? null : UUID.fromString(parts[1])
        };
    }

    private static BigDecimal z(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }
    private static String nzs(String v) { return v != null ? v : ""; }
}
