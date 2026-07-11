package com.hrms.payroll.web;

import com.hrms.employee.dto.EmployeeTimeTypeUsageDto;
import com.hrms.payroll.dto.AuditDtos;
import com.hrms.payroll.service.AuditService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Audit Tools — manager-only cleanup utilities. Every endpoint here is
 * gated by the 'audit.tools' permission, granted only to the company
 * administrator role, not automatically to any other role. */
@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAuthority('audit.tools')")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/day-zero-adjustments")
    public List<AuditDtos.DayZeroAdjustmentRow> dayZeroAdjustments(@RequestParam UUID employeeId) {
        return service.findDayZeroAdjustments(employeeId);
    }

    @DeleteMapping("/day-zero-adjustments/{id}")
    public void deleteDayZeroAdjustment(@PathVariable UUID id) {
        service.deleteDayZeroAdjustment(id);
    }

    @GetMapping("/payroll-runs")
    public List<AuditDtos.PayrollRunRow> payrollRuns(@RequestParam(defaultValue = "50") int limit) {
        return service.findPayrollRuns(limit);
    }

    @DeleteMapping("/payroll-runs/{id}")
    public void deletePayrollRun(@PathVariable UUID id) {
        service.deletePayrollRun(id);
    }

    @GetMapping("/time-usage")
    public EmployeeTimeTypeUsageDto timeUsage(@RequestParam UUID employeeId, @RequestParam int year) {
        return service.recalculateTimeUsage(employeeId, year);
    }

    @GetMapping("/leave-discrepancies")
    public List<AuditDtos.LeaveDiscrepancyRow> leaveDiscrepancies(@RequestParam UUID employeeId) {
        return service.findLeaveDiscrepancies(employeeId);
    }

    public record RecalculateLeaveRequest(BigDecimal newTotalDays) {
    }

    @PostMapping("/leave-requests/{id}/recalculate")
    public void recalculateLeaveRequest(@PathVariable UUID id, @RequestBody RecalculateLeaveRequest request) {
        service.recalculateLeaveRequest(id, request.newTotalDays());
    }
}
