package com.hrms.payroll.service;

import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.repository.AssignmentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.DashboardDto;
import com.hrms.payroll.repository.PayrollResultLineRepository;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.project.domain.Project;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import com.hrms.timesheet.repository.TimesheetDayCostRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aggregates the numbers behind the management dashboard for one selected
 * period — headcount, roster (present/on leave/absent), per-project
 * manpower/man-hours/pay, salary-by-category, and the period's LOCKED
 * payroll totals. Every number comes from a database-side aggregate
 * query or a single batch fetch, never row-by-row counting in Java, so
 * this stays fast at any company size.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepo;
    private final ProjectRepository projectRepo;
    private final PayrollPeriodRepository periodRepo;
    private final TimesheetDayRepository dayRepo;
    private final TimesheetDayCostRepository dayCostRepo;
    private final AssignmentRepository assignmentRepo;
    private final PayrollResultRepository resultRepo;
    private final PayrollResultLineRepository lineRepo;

    public DashboardService(EmployeeRepository employeeRepo, ProjectRepository projectRepo,
                            PayrollPeriodRepository periodRepo, TimesheetDayRepository dayRepo,
                            TimesheetDayCostRepository dayCostRepo, AssignmentRepository assignmentRepo,
                            PayrollResultRepository resultRepo, PayrollResultLineRepository lineRepo) {
        this.employeeRepo = employeeRepo;
        this.projectRepo = projectRepo;
        this.periodRepo = periodRepo;
        this.dayRepo = dayRepo;
        this.dayCostRepo = dayCostRepo;
        this.assignmentRepo = assignmentRepo;
        this.resultRepo = resultRepo;
        this.lineRepo = lineRepo;
    }

    /** @param periodId the period to show; null means "the current
     *         calendar month if it exists, else the most recent period". */
    public DashboardDto summary(UUID periodId) {
        UUID companyId = TenantContext.requireCompanyId();
        LocalDate today = LocalDate.now();

        DashboardDto dto = new DashboardDto();
        dto.setProjectCount((int) projectRepo.countByCompanyIdAndStatus(companyId, "ACTIVE"));
        dto.setActiveEmployeeCount((int) employeeRepo.countByCompanyIdAndStatus(companyId, "ACTIVE"));

        PayrollPeriod period = resolvePeriod(companyId, periodId, today);
        boolean isCurrentMonth = period != null && period.getPeriodYear() == today.getYear()
                && period.getPeriodMonth() == today.getMonthValue();
        dto.setCurrentMonth(isCurrentMonth);

        if (isCurrentMonth) {
            List<Object[]> dailyResult = dayRepo.dailyAttendanceBreakdown(companyId, today);
            Object[] daily = dailyResult.isEmpty() ? new Object[]{0, 0, 0, 0} : dailyResult.get(0);
            dto.setPresentToday(toInt(daily[0]));
            dto.setOnLeaveToday(toInt(daily[1]));
            dto.setAbsentToday(toInt(daily[2]));
            dto.setNotMarkedToday(Math.max(0, dto.getActiveEmployeeCount() - toInt(daily[3])));
        }

        if (period != null) {
            dto.setPeriodYear(period.getPeriodYear());
            dto.setPeriodMonth(period.getPeriodMonth());

            // Cumulative attendance across the whole selected period — up
            // to today if it's the current month, otherwise the full period.
            LocalDate asOf = isCurrentMonth ? today : LocalDate.of(period.getPeriodYear(), period.getPeriodMonth(), 1).plusMonths(1).minusDays(1);
            List<Object[]> monthlyResult = dayRepo.monthlyAttendanceBreakdown(companyId, period.getPeriodYear(), period.getPeriodMonth(), asOf);
            Object[] monthly = monthlyResult.isEmpty() ? new Object[]{0, 0, 0} : monthlyResult.get(0);
            dto.setPresentDaysMonth(toInt(monthly[0]));
            dto.setLeaveDaysMonth(toInt(monthly[1]));
            dto.setAbsentDaysMonth(toInt(monthly[2]));

            // LOCKED payroll totals for the period.
            List<Object[]> summary = resultRepo.summarizeLockedPeriod(period.getId());
            if (!summary.isEmpty()) {
                Object[] row = summary.get(0);
                int payslipCount = toInt(row[0]);
                dto.setPayslipCount(payslipCount);
                dto.setPeriodLocked(payslipCount > 0);
                dto.setNetDisbursed(toBigDecimal(row[1]));
                dto.setTotalDeductions(toBigDecimal(row[2]));
                dto.setTotalAllowances(lineRepo.sumAllowancesForLockedPeriod(period.getId()));
            }

            // Salary by category (Basic / Allowance / Overtime / ...).
            for (Object[] row : lineRepo.earningsByCategoryForLockedPeriod(period.getId())) {
                DashboardDto.CategoryStat stat = new DashboardDto.CategoryStat();
                stat.setCategory(row[0] == null ? "Other" : row[0].toString());
                stat.setAmount(toBigDecimal(row[1]));
                dto.getCategoryStats().add(stat);
            }

            dto.getProjectStats().addAll(buildProjectStats(companyId, period));
        }

        return dto;
    }

    private List<DashboardDto.ProjectStat> buildProjectStats(UUID companyId, PayrollPeriod period) {
        Map<UUID, Project> projectsById = projectRepo.findByCompanyIdOrderByName(companyId).stream()
                .collect(java.util.stream.Collectors.toMap(Project::getId, p -> p, (a, b) -> a));

        // Headcount + employee-to-project map (one batch query).
        Map<UUID, Integer> headcountByProject = new HashMap<>();
        Map<UUID, UUID> projectByEmployee = new HashMap<>();
        for (Assignment a : assignmentRepo.findActiveWithProjectByCompanyId(companyId)) {
            if (projectByEmployee.putIfAbsent(a.getEmployeeId(), a.getProjectId()) == null) {
                headcountByProject.merge(a.getProjectId(), 1, Integer::sum);
            }
        }

        // Man-hours per project (one aggregate query).
        Map<UUID, BigDecimal> hoursByProject = new HashMap<>();
        for (Object[] row : dayCostRepo.manHoursByProject(companyId, period.getPeriodYear(), period.getPeriodMonth())) {
            UUID projectId = toUuid(row[0]);
            if (projectId != null) {
                hoursByProject.put(projectId, toBigDecimal(row[1]));
            }
        }

        // Net pay per employee for the LOCKED period, grouped by project
        // via the map built above (one batch query, no per-employee call).
        Map<UUID, BigDecimal> netByProject = new HashMap<>();
        for (Object[] row : resultRepo.employeeNetForLockedPeriod(period.getId())) {
            UUID employeeId = toUuid(row[0]);
            UUID projectId = employeeId == null ? null : projectByEmployee.get(employeeId);
            if (projectId == null) {
                continue;
            }
            netByProject.merge(projectId, toBigDecimal(row[1]), BigDecimal::add);
        }

        Map<UUID, DashboardDto.ProjectStat> stats = new LinkedHashMap<>();
        for (UUID projectId : java.util.stream.Stream.of(headcountByProject.keySet(), hoursByProject.keySet(), netByProject.keySet())
                .flatMap(java.util.Set::stream).distinct().toList()) {
            Project project = projectsById.get(projectId);
            DashboardDto.ProjectStat stat = new DashboardDto.ProjectStat();
            stat.setProjectCode(project != null ? project.getCode() : "?");
            stat.setProjectName(project != null ? project.getName() : "Unknown");
            stat.setHeadcount(headcountByProject.getOrDefault(projectId, 0));
            stat.setManHours(hoursByProject.getOrDefault(projectId, BigDecimal.ZERO));
            stat.setNetPay(netByProject.getOrDefault(projectId, BigDecimal.ZERO));
            stats.put(projectId, stat);
        }
        return stats.values().stream()
                .sorted((a, b) -> Integer.compare(b.getHeadcount(), a.getHeadcount()))
                .toList();
    }

    private PayrollPeriod resolvePeriod(UUID companyId, UUID periodId, LocalDate today) {
        if (periodId != null) {
            return periodRepo.findById(periodId).orElse(null);
        }
        List<PayrollPeriod> periods = periodRepo.findByCompanyIdOrderByPeriodYearDescPeriodMonthDesc(companyId);
        return periods.stream()
                .filter(p -> p.getPeriodYear() == today.getYear() && p.getPeriodMonth() == today.getMonthValue())
                .findFirst()
                .orElse(periods.isEmpty() ? null : periods.get(0));
    }

    private int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private UUID toUuid(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }
}
