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
import org.springframework.jdbc.core.JdbcTemplate;
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
    private static final UUID DEFAULT_HANDOVER_KEPT_EMPLOYEE_ID =
            UUID.fromString("efe2f49c-1f5e-4ce8-a004-9275da55af1a");
    private static final String HANDOVER_CONFIRMATION = "DELETE HANDOVER";

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
    private final JdbcTemplate jdbc;

    public AuditService(PayrollAdjustmentRepository adjustmentRepo, PayrollRunRepository runRepo,
                        PayrollResultRepository resultRepo, PayrollResultLineRepository lineRepo,
                        PayrollPeriodRepository periodRepo, ProjectRepository projectRepo,
                        EmployeeTimeTypeUsageService usageService, LeaveRequestRepository leaveRequestRepo,
                        LeaveTypeRepository leaveTypeRepo, TimesheetRepository timesheetRepo,
                        TimesheetDayRepository dayRepo, TimesheetService timesheetService,
                        JdbcTemplate jdbc) {
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
        this.jdbc = jdbc;
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

    /** Manager override: deletes a payroll run and everything under it
     * (result lines, then results, then the run). This is the recovery path
     * for large corrections when Day Zero is not enough. */
    @Transactional
    public void deletePayrollRun(UUID runId) {
        PayrollRun run = runRepo.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll run not found: " + runId));
        lineRepo.deleteByRunId(runId);
        resultRepo.deleteByRunId(runId);
        runRepo.delete(run);
    }

    @Transactional
    public AuditDtos.HandoverCleanupResult runHandoverCleanup(String confirmation, UUID keptEmployeeId) {
        if (!HANDOVER_CONFIRMATION.equals(confirmation)) {
            throw new BusinessRuleException("audit.handover.confirmation",
                    "Type DELETE HANDOVER to run the handover cleanup.");
        }
        UUID kept = keptEmployeeId != null ? keptEmployeeId : DEFAULT_HANDOVER_KEPT_EMPLOYEE_ID;
        Integer keptExists = jdbc.queryForObject("select count(*) from employee where id = ?", Integer.class, kept);
        if (keptExists == null || keptExists == 0) {
            throw new BusinessRuleException("audit.handover.employee_missing",
                    "Kept employee was not found: " + kept);
        }

        jdbc.update("""
                insert into project (id, company_id, code, name, status)
                select gen_random_uuid(), e.company_id, 'DEMO', 'Demo Project', 'ACTIVE'
                from employee e
                where e.id = ?
                  and not exists (
                    select 1 from project p where p.code = 'DEMO' and p.company_id = e.company_id
                  )
                """, kept);
        jdbc.update("""
                update assignment a
                set project_id = p.id, cost_code_id = null
                from employee e, project p
                where a.employee_id = e.id
                  and e.id = ?
                  and p.code = 'DEMO'
                  and p.company_id = e.company_id
                """, kept);
        jdbc.update("""
                update assignment
                set supervisor_employee_id = null
                where employee_id = ?
                  and supervisor_employee_id is not null
                  and supervisor_employee_id <> ?
                """, kept, kept);
        jdbc.update("update employee set supervisor_employee_id = null, timekeeper_employee_id = null where id = ?", kept);

        jdbc.update("delete from timesheet where employee_id = ?", kept);
        jdbc.update("delete from leave_adjustment where employee_id = ?", kept);
        jdbc.update("delete from leave_request where employee_id = ?", kept);
        jdbc.update("delete from payroll_adjustment where employee_id = ?", kept);

        jdbc.update("delete from payroll_result");
        jdbc.update("delete from payroll_run");
        jdbc.update("delete from provision_run");
        jdbc.update("delete from ticket_ledger");

        jdbc.update("delete from timesheet where employee_id <> ?", kept);
        jdbc.update("delete from leave_adjustment where employee_id <> ?", kept);
        jdbc.update("delete from leave_request where employee_id <> ?", kept);

        jdbc.update("update crew set parent_crew_id = null where project_id in (select id from project where code <> 'DEMO')");
        jdbc.update("delete from crew where project_id in (select id from project where code <> 'DEMO')");

        jdbc.update("delete from app_user where employee_id <> ? and employee_id is not null", kept);
        jdbc.update("delete from employee_bank_account where employee_id <> ?", kept);
        jdbc.update("delete from employee_dependent where employee_id <> ?", kept);
        jdbc.update("delete from employee_document where employee_id <> ?", kept);
        jdbc.update("delete from employee_shift");
        jdbc.update("delete from timekeeper_project");
        jdbc.update("delete from legacy_employee_raw where employee_id <> ?", kept);
        jdbc.update("delete from contract_pay_item where employee_id <> ?", kept);
        jdbc.update("delete from contract where employee_id <> ?", kept);
        jdbc.update("delete from assignment where employee_id <> ?", kept);

        jdbc.update("delete from payroll_period_project where project_id in (select id from project where code <> 'DEMO')");
        jdbc.update("delete from shift where project_id in (select id from project where code <> 'DEMO')");
        jdbc.update("delete from cost_code where project_id in (select id from project where code <> 'DEMO')");
        jdbc.update("delete from payroll_rule where project_id in (select id from project where code <> 'DEMO')");
        jdbc.update("delete from provision_rule where project_id in (select id from project where code <> 'DEMO')");
        jdbc.update("delete from timekeeper_project where project_id in (select id from project where code <> 'DEMO')");
        jdbc.update("delete from project where code <> 'DEMO'");

        jdbc.update("update employee set supervisor_employee_id = null, timekeeper_employee_id = null where id <> ?", kept);
        jdbc.update("delete from employee where id <> ?", kept);

        AuditDtos.HandoverCleanupResult result = new AuditDtos.HandoverCleanupResult();
        result.setEmployeesLeft(count("employee"));
        result.setProjectsLeft(count("project"));
        result.setAppUsersLeft(count("app_user"));
        result.setTimesheetsLeft(count("timesheet"));
        result.setPayrollResultsLeft(count("payroll_result"));
        result.setCrewsLeft(count("crew"));
        result.setShiftsLeft(count("shift"));
        result.setCostCodesLeft(count("cost_code"));
        return result;
    }

    private long count(String tableName) {
        Number count = jdbc.queryForObject("select count(*) from " + tableName, Number.class);
        return count != null ? count.longValue() : 0L;
    }
}
