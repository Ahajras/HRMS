package com.hrms.employee.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.dto.EmployeeTimeTypeUsageDto;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.timesheet.domain.TimeType;
import com.hrms.timesheet.domain.TimeTypePayrollRule;
import com.hrms.timesheet.domain.Timesheet;
import com.hrms.timesheet.domain.TimesheetDay;
import com.hrms.timesheet.repository.TimeTypePayrollRuleRepository;
import com.hrms.timesheet.repository.TimeTypeRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import com.hrms.timesheet.repository.TimesheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EmployeeTimeTypeUsageService {

    private final EmployeeRepository employeeRepository;
    private final TimesheetRepository timesheetRepository;
    private final TimesheetDayRepository dayRepository;
    private final TimeTypeRepository timeTypeRepository;
    private final TimeTypePayrollRuleRepository payrollRuleRepository;

    public EmployeeTimeTypeUsageService(EmployeeRepository employeeRepository,
                                        TimesheetRepository timesheetRepository,
                                        TimesheetDayRepository dayRepository,
                                        TimeTypeRepository timeTypeRepository,
                                        TimeTypePayrollRuleRepository payrollRuleRepository) {
        this.employeeRepository = employeeRepository;
        this.timesheetRepository = timesheetRepository;
        this.dayRepository = dayRepository;
        this.timeTypeRepository = timeTypeRepository;
        this.payrollRuleRepository = payrollRuleRepository;
    }

    public EmployeeTimeTypeUsageDto usage(UUID employeeId, int year) {
        UUID companyId = TenantContext.requireCompanyId();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        if (!companyId.equals(employee.getCompanyId())) {
            throw new ResourceNotFoundException("Employee not found: " + employeeId);
        }

        EmployeeTimeTypeUsageDto dto = new EmployeeTimeTypeUsageDto();
        dto.setYear(year);
        Map<UUID, TimeType> types = new HashMap<>();
        Map<UUID, EmployeeTimeTypeUsageDto.Row> rows = new LinkedHashMap<>();
        Set<UUID> usedTypeIds = new HashSet<>();

        for (Timesheet ts : timesheetRepository.findByCompanyIdAndEmployeeIdAndPeriodYearOrderByPeriodMonth(companyId, employeeId, year)) {
            for (TimesheetDay day : dayRepository.findByTimesheetIdOrderByWorkDate(ts.getId())) {
                if (day.getTimeTypeId() == null) {
                    continue;
                }
                TimeType type = types.computeIfAbsent(day.getTimeTypeId(), id -> timeTypeRepository.findById(id).orElse(null));
                if (type == null || excluded(type)) {
                    continue;
                }
                usedTypeIds.add(type.getId());
                EmployeeTimeTypeUsageDto.Row row = rows.computeIfAbsent(type.getId(), ignored -> newRow(type));
                BigDecimal hours = usageHours(day);
                row.setUsedHours(row.getUsedHours().add(hours));
                row.setUsedDays(row.getUsedDays().add(usageDays(day, hours)));
                row.setOccurrences(row.getOccurrences() + 1);
                row.setFirstDate(min(row.getFirstDate(), day.getWorkDate()));
                row.setLastDate(max(row.getLastDate(), day.getWorkDate()));
            }
        }

        applyThresholds(companyId, usedTypeIds, rows);
        dto.setRows(rows.values().stream()
                .sorted((a, b) -> String.valueOf(a.getTimeTypeCode()).compareTo(String.valueOf(b.getTimeTypeCode())))
                .toList());
        return dto;
    }

    private void applyThresholds(UUID companyId, Set<UUID> timeTypeIds, Map<UUID, EmployeeTimeTypeUsageDto.Row> rows) {
        if (timeTypeIds.isEmpty()) {
            return;
        }
        for (TimeTypePayrollRule rule : payrollRuleRepository.findByCompanyIdAndTimeTypeIdIn(companyId, timeTypeIds)) {
            EmployeeTimeTypeUsageDto.Row row = rows.get(rule.getTimeTypeId());
            if (row == null || rule.getThresholdDays() <= row.getThresholdDays()) {
                continue;
            }
            row.setThresholdDays(rule.getThresholdDays());
            row.setThresholdScope(normalizeThresholdScope(rule.getThresholdScope(), rule.getThresholdDays()));
        }
    }

    private static EmployeeTimeTypeUsageDto.Row newRow(TimeType type) {
        EmployeeTimeTypeUsageDto.Row row = new EmployeeTimeTypeUsageDto.Row();
        row.setTimeTypeCode(type.getCode());
        row.setTimeTypeName(type.getName());
        row.setCategory(type.getCategory());
        row.setThresholdScope("NONE");
        return row;
    }

    private static boolean excluded(TimeType type) {
        String category = type.getCategory() == null ? "" : type.getCategory().trim().toUpperCase();
        return "REGULAR".equals(category) || "REST".equals(category) || "HOLIDAY".equals(category) || "OVERTIME".equals(category);
    }

    private static BigDecimal usageHours(TimesheetDay day) {
        BigDecimal normal = z(day.getNormalHours());
        if (normal.compareTo(BigDecimal.ZERO) > 0) return normal;
        BigDecimal planned = z(day.getPlannedHours());
        if (planned.compareTo(BigDecimal.ZERO) > 0) return planned;
        BigDecimal worked = z(day.getWorkedHours());
        if (worked.compareTo(BigDecimal.ZERO) > 0) return worked;
        return BigDecimal.ZERO;
    }

    private static BigDecimal usageDays(TimesheetDay day, BigDecimal hours) {
        BigDecimal planned = z(day.getPlannedHours());
        if (planned.compareTo(BigDecimal.ZERO) > 0 && z(hours).compareTo(BigDecimal.ZERO) > 0) {
            return z(hours).divide(planned, 4, RoundingMode.HALF_UP);
        }
        return BigDecimal.ONE;
    }

    private static String normalizeThresholdScope(String thresholdScope, int thresholdDays) {
        if (thresholdDays <= 0) return "NONE";
        if (thresholdScope != null && "ANNUAL".equalsIgnoreCase(thresholdScope)) return "ANNUAL";
        return "CONSECUTIVE";
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    private static BigDecimal z(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
