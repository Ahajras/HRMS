package com.hrms.payroll.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.dto.EmployeeTimeTypeUsageDto;
import com.hrms.employee.service.EmployeeTimeTypeUsageService;
import com.hrms.leave.domain.LeaveRequest;
import com.hrms.leave.domain.LeaveType;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.payroll.domain.PayrollAdjustment;
import com.hrms.payroll.domain.PayrollRun;
import com.hrms.payroll.dto.AuditDtos;
import com.hrms.payroll.repository.PayrollAdjustmentRepository;
import com.hrms.payroll.repository.PayrollResultLineRepository;
import com.hrms.payroll.repository.PayrollResultRepository;
import com.hrms.payroll.repository.PayrollRunRepository;
import com.hrms.project.domain.Project;
import com.hrms.project.repository.ProjectRepository;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.domain.Timesheet;
import com.hrms.timesheet.domain.TimesheetDay;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import com.hrms.timesheet.repository.TimesheetRepository;
import com.hrms.timesheet.service.TimesheetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit Tools — manager-only, guarded cleanup actions that previously had
 * to be done by hand against the database (e.g. deleting a still-pending
 * Day Zero adjustment, or a draft/calculated payroll run that was created
 * by mistake). Every action here refuses to touch anything that has
 * already been approved, locked, or applied — those require reopening
 * through the normal screens, not a shortcut here.
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    private final PayrollAdjustmentRepository adjustmentRepo;
    private final PayrollRunRepository runRepo;
    private final PayrollResultRepository resultRepo;
    private final PayrollResultLineRepository lineRepo;
    private final PayrollPeriodRepository periodRepo;
    private final ProjectRepository projectRepo;
    private final EmployeeTimeTypeUsageService usageService;
    private final LeaveRequestRepository leaveRequestRepo;
    private final LeaveTypeRepository leaveTypeRepo;
    private final TimesheetRepository timesheetRepo;
    private final TimesheetDayRepository dayRepo;
    private final TimesheetService timesheetService;

    public AuditService(PayrollAdjustmentRepository adjustmentRepo, PayrollRunRepository runRepo,
                        PayrollResultRepository resultRepo, PayrollResultLineRepository lineRepo,
                        PayrollPeriodRepository periodRepo, ProjectRepository projectRepo,
                        EmployeeTimeTypeUsageService usageService, LeaveRequestRepository leaveRequestRepo,
                        LeaveTypeRepository leaveTypeRepo, TimesheetRepository timesheetRepo,
                        TimesheetDayRepository dayRepo, TimesheetService timesheetService) {
        this.adjustmentRepo = adjustmentRepo;
        this.runRepo = runRepo;
        this.resultRepo = resultRepo;
        this.lineRepo = lineRepo;
        this.periodRepo = periodRepo;
        this.projectRepo = projectRepo;
        this.usageService = usageService;
        this.leaveRequestRepo = leaveRequestRepo;
        this.leaveTypeRepo = leaveTypeRepo;
        this.timesheetRepo = timesheetRepo;
        this.dayRepo = dayRepo;
        this.timesheetService = timesheetService;
    }

    /** Recalculate Time Usage — this is always computed live from the
     * current timesheet (including Day Zero corrections), so "recalculate"
     * here just means "look at it right now" — there is no separate cached
     * number that can go stale. Exposed directly in Audit Tools so a
     * manager can check any employee without leaving this screen. */
    public EmployeeTimeTypeUsageDto recalculateTimeUsage(UUID employeeId, int year) {
        return usageService.usage(employeeId, year);
    }

    /** Recalculate Leave — leave balances are NOT always-live like Time
     * Usage: each request's total_days is a fixed number set when it was
     * approved. If the underlying days were later corrected (edited after
     * reopening, or via Day Zero), that fixed number can drift from what
     * the timesheet actually shows now. This compares the two and lets a
     * manager confirm a fix — it does not silently auto-correct. */
    public List<AuditDtos.LeaveDiscrepancyRow> findLeaveDiscrepancies(UUID employeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        List<AuditDtos.LeaveDiscrepancyRow> out = new ArrayList<>();
        for (LeaveRequest lr : leaveRequestRepo.findByCompanyIdAndEmployeeIdOrderByStartDateDesc(companyId, employeeId)) {
            if (!"APPROVED".equalsIgnoreCase(lr.getStatus())) {
                continue;
            }
            LeaveType type = leaveTypeRepo.findById(lr.getLeaveTypeId()).orElse(null);
            if (type == null || type.getTimeTypeId() == null || lr.getStartDate() == null || lr.getEndDate() == null) {
                continue;
            }
            int actualDays = countEffectiveDays(companyId, employeeId, type.getTimeTypeId(), lr.getStartDate(), lr.getEndDate());
            BigDecimal recorded = lr.getTotalDays() != null ? lr.getTotalDays() : BigDecimal.ZERO;
            if (BigDecimal.valueOf(actualDays).compareTo(recorded) != 0) {
                AuditDtos.LeaveDiscrepancyRow row = new AuditDtos.LeaveDiscrepancyRow();
                row.setLeaveRequestId(lr.getId());
                row.setLeaveTypeCode(type.getCode());
                row.setStartDate(lr.getStartDate());
                row.setEndDate(lr.getEndDate());
                row.setRecordedDays(recorded);
                row.setActualDays(BigDecimal.valueOf(actualDays));
                out.add(row);
            }
        }
        return out;
    }

    /** Counts how many days in [from, to] currently have the given time
     * type as their EFFECTIVE type — a raw timesheet day of that type, or
     * a day Day-Zero-corrected TO that type — mirroring exactly how Time
     * Usage / payroll thresholds already count it, so this stays
     * consistent with what the rest of the system considers "true". */
    private int countEffectiveDays(UUID companyId, UUID employeeId, UUID timeTypeId, LocalDate from, LocalDate to) {
        List<PayrollAdjustment> corrections = adjustmentRepo
                .findByCompanyIdAndEmployeeIdAndWorkDateBetweenAndNewTimeTypeIdIsNotNull(companyId, employeeId, from, to);
        Map<UUID, UUID> correctedTypeByDayId = new HashMap<>();
        for (PayrollAdjustment adj : corrections) {
            if (adj.getTimesheetDayId() != null) {
                correctedTypeByDayId.put(adj.getTimesheetDayId(), adj.getNewTimeTypeId());
            }
        }
        int count = 0;
        for (int y = from.getYear(); y <= to.getYear(); y++) {
            for (Timesheet ts : timesheetRepo.findByCompanyIdAndEmployeeIdAndPeriodYearOrderByPeriodMonth(companyId, employeeId, y)) {
                for (TimesheetDay day : dayRepo.findByTimesheetIdOrderByWorkDate(ts.getId())) {
                    if (day.getWorkDate() == null || day.getWorkDate().isBefore(from) || day.getWorkDate().isAfter(to)) {
                        continue;
                    }
                    UUID effectiveType = correctedTypeByDayId.getOrDefault(day.getId(), day.getTimeTypeId());
                    if (timeTypeId.equals(effectiveType)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** Applies a reviewed correction — sets total_days to match what the
     * timesheet actually shows today. Requires the manager to have already
     * seen the before/after via findLeaveDiscrepancies; this does not run
     * on its own. */
    @Transactional
    public void recalculateLeaveRequest(UUID leaveRequestId, BigDecimal newTotalDays) {
        LeaveRequest lr = leaveRequestRepo.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + leaveRequestId));
        lr.setTotalDays(newTotalDays);
        leaveRequestRepo.save(lr);
    }

    // --- Day Zero adjustments -----------------------------------------

    public List<AuditDtos.DayZeroAdjustmentRow> findDayZeroAdjustments(UUID employeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        Map<UUID, PayrollPeriod> periodCache = new LinkedHashMap<>();
        return adjustmentRepo.findByCompanyIdAndEmployeeIdOrderByCreatedAtDesc(companyId, employeeId).stream()
                .map(a -> toRow(a, periodCache))
                .toList();
    }

    private AuditDtos.DayZeroAdjustmentRow toRow(PayrollAdjustment a, Map<UUID, PayrollPeriod> periodCache) {
        AuditDtos.DayZeroAdjustmentRow row = new AuditDtos.DayZeroAdjustmentRow();
        row.setId(a.getId());
        row.setWorkDate(a.getWorkDate());
        row.setAmount(a.getAmount());
        row.setStatus(a.getStatus());
        row.setReason(a.getReason());
        row.setCreatedAt(a.getCreatedAt());
        PayrollPeriod period = periodCache.computeIfAbsent(a.getOriginalPeriodId(),
                id -> periodRepo.findById(id).orElse(null));
        if (period != null) {
            row.setPeriodYear(period.getPeriodYear());
            row.setPeriodMonth(period.getPeriodMonth());
        }
        return row;
    }

    /** Deletes a Day Zero adjustment — only while it is still PENDING (has
     * not yet been folded into a calculated payslip). An APPLIED adjustment
     * already affected a real payroll run; removing it here would silently
     * desync that payslip from its own audit trail, so it is refused. */
    @Transactional
    public void deleteDayZeroAdjustment(UUID adjustmentId) {
        PayrollAdjustment adj = adjustmentRepo.findById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found: " + adjustmentId));
        if (!"PENDING".equalsIgnoreCase(adj.getStatus())) {
            throw new BusinessRuleException("audit.adjustment_not_pending",
                    "This adjustment is already " + adj.getStatus()
                            + " — it has been applied to a real payslip and cannot be deleted here.");
        }
        adjustmentRepo.delete(adj);
    }

    // --- Payroll runs ---------------------------------------------------

    public List<AuditDtos.PayrollRunRow> findPayrollRuns(int limit) {
        UUID companyId = TenantContext.requireCompanyId();
        List<PayrollRun> runs = runRepo.findByCompanyIdOrderByCreatedAtDesc(companyId);
        Map<UUID, PayrollPeriod> periodCache = new LinkedHashMap<>();
        Map<UUID, Project> projectCache = new LinkedHashMap<>();
        return runs.stream().limit(Math.max(limit, 1)).map(r -> toRow(r, periodCache, projectCache)).toList();
    }

    private AuditDtos.PayrollRunRow toRow(PayrollRun r, Map<UUID, PayrollPeriod> periodCache, Map<UUID, Project> projectCache) {
        AuditDtos.PayrollRunRow row = new AuditDtos.PayrollRunRow();
        row.setId(r.getId());
        row.setPayGroup(r.getPayGroup());
        row.setStatus(r.getStatus());
        row.setCreatedAt(r.getCreatedAt());
        row.setEmployeeCount(resultRepo.findByRunIdOrderByEmployeeId(r.getId()).size());
        PayrollPeriod period = periodCache.computeIfAbsent(r.getPeriodId(), id -> periodRepo.findById(id).orElse(null));
        if (period != null) {
            row.setPeriodYear(period.getPeriodYear());
            row.setPeriodMonth(period.getPeriodMonth());
        }
        if (r.getProjectId() != null) {
            Project project = projectCache.computeIfAbsent(r.getProjectId(), id -> projectRepo.findById(id).orElse(null));
            row.setProjectCode(project != null ? project.getCode() : null);
        }
        return row;
    }

    /** Deletes a payroll run and everything under it (result lines, then
     * results, then the run) — only while it is DRAFT or CALCULATED. Once
     * a run has been APPROVED or LOCKED it represents money that may
     * already be considered final/disbursed, so deleting it here is
     * refused; use Reopen from the normal screen instead. */
    @Transactional
    public void deletePayrollRun(UUID runId) {
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + runId));
        String status = run.getStatus();
        if (!"DRAFT".equalsIgnoreCase(status) && !"CALCULATED".equalsIgnoreCase(status)) {
            throw new BusinessRuleException("audit.run_not_deletable",
                    "This run is " + status + " — only DRAFT or CALCULATED runs can be deleted here. "
                            + "Reopen it from the Payroll Runs screen first if it truly needs to be removed.");
        }
        lineRepo.deleteByRunId(runId);
        resultRepo.deleteByRunId(runId);
        runRepo.delete(run);
    }
}
