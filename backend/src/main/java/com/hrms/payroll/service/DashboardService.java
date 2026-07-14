package com.hrms.payroll.service;

import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.payroll.dto.DashboardDto;
import com.hrms.payroll.repository.PayrollResultLineRepository;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates the numbers behind the management dashboard — headcount,
 * today's roster (present/on leave/absent), this month's cumulative
 * attendance, and the most recent LOCKED payroll's totals. Every number
 * comes from a database-side aggregate query, never from fetching and
 * counting rows in Java, so this stays fast at any company size.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepo;
    private final ProjectRepository projectRepo;
    private final PayrollPeriodRepository periodRepo;
    private final TimesheetDayRepository dayRepo;
    private final PayrollResultRepository resultRepo;
    private final PayrollResultLineRepository lineRepo;

    public DashboardService(EmployeeRepository employeeRepo, ProjectRepository projectRepo,
                            PayrollPeriodRepository periodRepo, TimesheetDayRepository dayRepo,
                            PayrollResultRepository resultRepo, PayrollResultLineRepository lineRepo) {
        this.employeeRepo = employeeRepo;
        this.projectRepo = projectRepo;
        this.periodRepo = periodRepo;
        this.dayRepo = dayRepo;
        this.resultRepo = resultRepo;
        this.lineRepo = lineRepo;
    }

    public DashboardDto summary() {
        UUID companyId = TenantContext.requireCompanyId();
        LocalDate today = LocalDate.now();

        DashboardDto dto = new DashboardDto();
        dto.setProjectCount((int) projectRepo.countByCompanyIdAndStatus(companyId, "ACTIVE"));
        dto.setActiveEmployeeCount((int) employeeRepo.countByCompanyIdAndStatus(companyId, "ACTIVE"));

        // Today's roster.
        List<Object[]> dailyResult = dayRepo.dailyAttendanceBreakdown(companyId, today);
        Object[] daily = dailyResult.isEmpty() ? new Object[]{0, 0, 0, 0} : dailyResult.get(0);
        int present = toInt(daily[0]);
        int onLeave = toInt(daily[1]);
        int absent = toInt(daily[2]);
        int marked = toInt(daily[3]);
        dto.setPresentToday(present);
        dto.setOnLeaveToday(onLeave);
        dto.setAbsentToday(absent);
        dto.setNotMarkedToday(Math.max(0, dto.getActiveEmployeeCount() - marked));

        // This month so far.
        List<Object[]> monthlyResult = dayRepo.monthlyAttendanceBreakdown(companyId, today.getYear(), today.getMonthValue(), today);
        Object[] monthly = monthlyResult.isEmpty() ? new Object[]{0, 0, 0} : monthlyResult.get(0);
        dto.setPresentDaysMonth(toInt(monthly[0]));
        dto.setLeaveDaysMonth(toInt(monthly[1]));
        dto.setAbsentDaysMonth(toInt(monthly[2]));

        // Reference period — this calendar month if it exists, otherwise
        // the most recently created period, so the payroll figures below
        // are never a confusing zero just because this month isn't set
        // up yet.
        List<PayrollPeriod> periods = periodRepo.findByCompanyIdOrderByPeriodYearDescPeriodMonthDesc(companyId);
        PayrollPeriod referencePeriod = periods.stream()
                .filter(p -> p.getPeriodYear() == today.getYear() && p.getPeriodMonth() == today.getMonthValue())
                .findFirst()
                .orElse(periods.isEmpty() ? null : periods.get(0));

        if (referencePeriod != null) {
            dto.setPeriodYear(referencePeriod.getPeriodYear());
            dto.setPeriodMonth(referencePeriod.getPeriodMonth());
            List<Object[]> summary = resultRepo.summarizeLockedPeriod(referencePeriod.getId());
            if (!summary.isEmpty()) {
                Object[] row = summary.get(0);
                int payslipCount = toInt(row[0]);
                dto.setPayslipCount(payslipCount);
                dto.setPeriodLocked(payslipCount > 0);
                dto.setNetDisbursed(toBigDecimal(row[1]));
                dto.setTotalDeductions(toBigDecimal(row[2]));
                dto.setTotalAllowances(lineRepo.sumAllowancesForLockedPeriod(referencePeriod.getId()));
            }
        }

        return dto;
    }

    private int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }
}
