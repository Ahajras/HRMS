package com.hrms.leave.service;

import com.hrms.employee.domain.Employee;
import com.hrms.leave.domain.LeaveAdjustment;
import com.hrms.leave.domain.LeaveRequest;
import com.hrms.leave.domain.LeaveType;
import com.hrms.leave.dto.LeaveBalanceDto;
import com.hrms.leave.repository.LeaveAdjustmentRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.rule.domain.Rule;
import com.hrms.rule.repository.CompanyRulePackageRepository;
import com.hrms.rule.repository.RulePackageRepository;
import com.hrms.rule.repository.RuleRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

@Service
public class LeaveBalanceService {
    private final LeaveAdjustmentRepository adjustmentRepo;
    private final LeaveRequestRepository requestRepo;
    private final TimesheetDayRepository timesheetDayRepo;
    private final CompanyRulePackageRepository companyRulePackageRepo;
    private final RulePackageRepository rulePackageRepo;
    private final RuleRepository ruleRepo;

    public LeaveBalanceService(LeaveAdjustmentRepository adjustmentRepo,
                               LeaveRequestRepository requestRepo,
                               TimesheetDayRepository timesheetDayRepo,
                               CompanyRulePackageRepository companyRulePackageRepo,
                               RulePackageRepository rulePackageRepo,
                               RuleRepository ruleRepo) {
        this.adjustmentRepo = adjustmentRepo;
        this.requestRepo = requestRepo;
        this.timesheetDayRepo = timesheetDayRepo;
        this.companyRulePackageRepo = companyRulePackageRepo;
        this.rulePackageRepo = rulePackageRepo;
        this.ruleRepo = ruleRepo;
    }

    public LeaveBalanceDto balance(UUID companyId, Employee employee, LeaveType type, LocalDate asOf) {
        BalanceNumbers numbers = numbers(companyId, employee, type, asOf, null, null);
        LeaveBalanceDto dto = new LeaveBalanceDto();
        dto.setEmployeeId(employee.getId());
        dto.setLeaveTypeId(type.getId());
        dto.setLeaveTypeCode(type.getCode());
        dto.setLeaveTypeName(type.getName());
        dto.setAsOfDate(asOf);
        dto.setAnnualRate(numbers.annualRate());
        dto.setEntitledToDate(numbers.entitlement());
        dto.setAdjustments(numbers.adjustments());
        dto.setUsedApproved(numbers.approved());
        dto.setUsedTimesheet(numbers.manualTimesheet());
        dto.setPending(numbers.pending());
        dto.setBalance(numbers.available());
        return dto;
    }

    public BigDecimal availableBeforeDay(UUID companyId, Employee employee, LeaveType type, LocalDate asOf,
                                         UUID excludeLeaveRequestId, UUID excludeTimesheetDayId) {
        return numbers(companyId, employee, type, asOf, excludeLeaveRequestId, excludeTimesheetDayId).available();
    }

    private BalanceNumbers numbers(UUID companyId, Employee employee, LeaveType type, LocalDate asOf,
                                   UUID excludeLeaveRequestId, UUID excludeTimesheetDayId) {
        BigDecimal annualRate = annualLeaveRate(companyId, employee, asOf);
        BigDecimal entitlement = "ANNUAL".equalsIgnoreCase(type.getCode())
                ? proratedEntitlement(companyId, employee.getHireDate(), asOf)
                : BigDecimal.ZERO;
        BigDecimal adjustments = adjustments(companyId, employee.getId(), type.getId(), asOf);
        BigDecimal approved = requestDays(companyId, employee.getId(), type.getId(), Set.of("APPROVED"), asOf, excludeLeaveRequestId);
        BigDecimal pending = requestDays(companyId, employee.getId(), type.getId(), Set.of("DRAFT", "SUBMITTED"), asOf, excludeLeaveRequestId);
        BigDecimal manualTimesheet = manualTimesheetLeaveDays(companyId, employee.getId(), type, asOf, excludeTimesheetDayId);
        BigDecimal available = entitlement.add(adjustments).subtract(approved).subtract(manualTimesheet);
        return new BalanceNumbers(annualRate, entitlement, adjustments, approved, pending, manualTimesheet, available);
    }

    private BigDecimal adjustments(UUID companyId, UUID employeeId, UUID leaveTypeId, LocalDate asOf) {
        BigDecimal total = BigDecimal.ZERO;
        for (LeaveAdjustment a : adjustmentRepo.findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndEffectiveDateLessThanEqual(companyId, employeeId, leaveTypeId, asOf)) {
            BigDecimal days = a.getDays() != null ? a.getDays() : BigDecimal.ZERO;
            if ("MANUAL_CREDIT".equalsIgnoreCase(a.getAdjustmentType())) total = total.add(days);
            else total = total.subtract(days);
        }
        return total;
    }

    private BigDecimal requestDays(UUID companyId, UUID employeeId, UUID leaveTypeId, Collection<String> statuses,
                                   LocalDate asOf, UUID excludeLeaveRequestId) {
        return requestRepo.findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndStatusInAndStartDateLessThanEqual(
                        companyId, employeeId, leaveTypeId, statuses, asOf).stream()
                .filter(r -> excludeLeaveRequestId == null || !excludeLeaveRequestId.equals(r.getId()))
                .map(r -> r.getTotalDays() != null ? r.getTotalDays() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal manualTimesheetLeaveDays(UUID companyId, UUID employeeId, LeaveType type, LocalDate asOf, UUID excludeTimesheetDayId) {
        if (type.getTimeTypeId() == null) {
            return BigDecimal.ZERO;
        }
        return timesheetDayRepo.sumManualLeaveDaysExcludingDay(companyId, employeeId, type.getTimeTypeId(), asOf, excludeTimesheetDayId);
    }

    private BigDecimal proratedEntitlement(UUID companyId, LocalDate hireDate, LocalDate asOf) {
        if (hireDate == null || asOf.isBefore(hireDate)) return BigDecimal.ZERO;
        LocalDate fiveYear = hireDate.plusYears(5);
        BigDecimal under = rule(companyId, "ANNUAL_LEAVE_DAYS_UNDER_5Y", new BigDecimal("21"));
        BigDecimal plus = rule(companyId, "ANNUAL_LEAVE_DAYS_5Y_PLUS", new BigDecimal("28"));
        BigDecimal total = BigDecimal.ZERO;
        if (hireDate.isBefore(fiveYear)) {
            LocalDate end = asOf.isBefore(fiveYear) ? asOf : fiveYear.minusDays(1);
            total = total.add(prorate(under, hireDate, end));
        }
        if (!asOf.isBefore(fiveYear)) total = total.add(prorate(plus, fiveYear, asOf));
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal annualLeaveRate(UUID companyId, Employee employee, LocalDate asOf) {
        if (employee.getHireDate() != null && !asOf.isBefore(employee.getHireDate().plusYears(5))) {
            return rule(companyId, "ANNUAL_LEAVE_DAYS_5Y_PLUS", new BigDecimal("28"));
        }
        return rule(companyId, "ANNUAL_LEAVE_DAYS_UNDER_5Y", new BigDecimal("21"));
    }

    private BigDecimal prorate(BigDecimal annual, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return BigDecimal.ZERO;
        long days = ChronoUnit.DAYS.between(start, end.plusDays(1));
        return annual.multiply(BigDecimal.valueOf(days)).divide(new BigDecimal("365"), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal rule(UUID companyId, String code, BigDecimal fallback) {
        String packageCode = companyRulePackageRepo.findById(companyId).map(c -> c.getPackageCode()).orElse("QATAR");
        return rulePackageRepo.findByCompanyIdIsNullAndCode(packageCode)
                .flatMap(pkg -> ruleRepo.findByPackageIdAndCodeAndStatus(pkg.getId(), code, "ACTIVE").stream().findFirst())
                .map(Rule::getValueNumber).orElse(fallback);
    }

    private record BalanceNumbers(BigDecimal annualRate, BigDecimal entitlement, BigDecimal adjustments,
                                  BigDecimal approved, BigDecimal pending, BigDecimal manualTimesheet,
                                  BigDecimal available) {
    }
}
