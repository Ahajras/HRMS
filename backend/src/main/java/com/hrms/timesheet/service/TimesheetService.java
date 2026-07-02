package com.hrms.timesheet.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.crew.domain.CrewMember;
import com.hrms.crew.repository.CrewMemberRepository;
import com.hrms.crew.service.TimekeeperService;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.AssignmentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.project.domain.CostCode;
import com.hrms.project.domain.Project;
import com.hrms.project.repository.CostCodeRepository;
import com.hrms.security.AuthenticatedUser;
import com.hrms.security.domain.AppUser;
import com.hrms.security.repository.AppUserRepository;
import com.hrms.timesheet.domain.EmployeeShift;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.domain.PayrollPeriodProject;
import com.hrms.timesheet.domain.PublicHoliday;
import com.hrms.timesheet.domain.Shift;
import com.hrms.timesheet.domain.ShiftDay;
import com.hrms.timesheet.domain.TimeType;
import com.hrms.timesheet.domain.Timesheet;
import com.hrms.timesheet.domain.TimesheetDay;
import com.hrms.timesheet.domain.TimesheetDayCost;
import com.hrms.timesheet.dto.GenerateTimesheetRequest;
import com.hrms.timesheet.dto.TimesheetDayCostDto;
import com.hrms.timesheet.dto.TimesheetDayDto;
import com.hrms.timesheet.dto.TimesheetSummaryDto;
import com.hrms.reference.repository.OvertimeCategoryRepository;
import com.hrms.timesheet.dto.TimesheetDto;
import com.hrms.timesheet.repository.EmployeeShiftRepository;
import com.hrms.timesheet.repository.PayrollPeriodProjectRepository;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import com.hrms.timesheet.repository.PublicHolidayRepository;
import com.hrms.timesheet.repository.ShiftDayRepository;
import com.hrms.timesheet.repository.ShiftRepository;
import com.hrms.timesheet.repository.TimeTypeRepository;
import com.hrms.timesheet.repository.TimesheetDayCostRepository;
import com.hrms.timesheet.repository.TimesheetDayRepository;
import com.hrms.timesheet.repository.TimesheetRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Monthly timesheet engine (FTDD Vol.1 Ch.3) — the source of actual worked
 * hours. Generates a month from the assigned shift (classifying weekly-off and
 * public-holiday days), lets the days be edited, recomputes worked/OT, and runs
 * the DRAFT -> SUBMITTED -> APPROVED -> LOCKED lifecycle.
 *
 * <p>OT here is worked-vs-standard only; the OT pay RATES live in the Rule
 * Engine and are applied later by the Overtime/Payroll engines.
 */
@Service
@Transactional
public class TimesheetService {

    private static final String DRAFT = "DRAFT";
    private static final String SUBMITTED = "SUBMITTED";
    private static final String APPROVED = "APPROVED";
    private static final String LOCKED = "LOCKED";

    private final TimesheetRepository timesheetRepo;
    private final TimesheetDayRepository dayRepo;
    private final ShiftRepository shiftRepo;
    private final TimeTypeRepository timeTypeRepo;
    private final PublicHolidayRepository holidayRepo;
    private final EmployeeRepository employeeRepo;
    private final AssignmentRepository assignmentRepo;
    private final PayrollPeriodRepository periodRepo;
    private final EmployeeShiftRepository employeeShiftRepo;
    private final ShiftDayRepository shiftDayRepo;
    private final TimesheetDayCostRepository dayCostRepo;
    private final AppUserRepository appUserRepo;
    private final TimekeeperService timekeeperService;
    private final CrewMemberRepository crewMemberRepo;
    private final OvertimeCategoryRepository overtimeCategoryRepo;
    private final PayrollPeriodProjectRepository periodProjectRepo;
    private final com.hrms.project.repository.ProjectRepository projectRepo;
    private final CostCodeRepository costCodeRepo;

    public TimesheetService(TimesheetRepository timesheetRepo, TimesheetDayRepository dayRepo,
                            ShiftRepository shiftRepo, TimeTypeRepository timeTypeRepo,
                            PublicHolidayRepository holidayRepo, EmployeeRepository employeeRepo,
                            AssignmentRepository assignmentRepo, PayrollPeriodRepository periodRepo,
                            EmployeeShiftRepository employeeShiftRepo, ShiftDayRepository shiftDayRepo,
                            TimesheetDayCostRepository dayCostRepo, AppUserRepository appUserRepo,
                            TimekeeperService timekeeperService, CrewMemberRepository crewMemberRepo,
                            OvertimeCategoryRepository overtimeCategoryRepo,
                            PayrollPeriodProjectRepository periodProjectRepo,
                            com.hrms.project.repository.ProjectRepository projectRepo,
                            CostCodeRepository costCodeRepo) {
        this.timesheetRepo = timesheetRepo;
        this.dayRepo = dayRepo;
        this.shiftRepo = shiftRepo;
        this.timeTypeRepo = timeTypeRepo;
        this.holidayRepo = holidayRepo;
        this.employeeRepo = employeeRepo;
        this.assignmentRepo = assignmentRepo;
        this.shiftDayRepo = shiftDayRepo;
        this.dayCostRepo = dayCostRepo;
        this.appUserRepo = appUserRepo;
        this.timekeeperService = timekeeperService;
        this.crewMemberRepo = crewMemberRepo;
        this.periodRepo = periodRepo;
        this.employeeShiftRepo = employeeShiftRepo;
        this.overtimeCategoryRepo = overtimeCategoryRepo;
        this.periodProjectRepo = periodProjectRepo;
        this.projectRepo = projectRepo;
        this.costCodeRepo = costCodeRepo;
    }

    // --- queries -----------------------------------------------------

    @Transactional(readOnly = true)
    public List<TimesheetDto> listByPeriod(int year, int month, UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        Set<UUID> allowed = restrictedProjects();
        return timesheetRepo.findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(companyId, year, month)
                .stream()
                .filter(t -> matchesProjectScope(t, projectId, allowed))
                .map(t -> toHeaderDto(t)).toList();
    }

    private boolean matchesProjectScope(Timesheet t, UUID projectId, Set<UUID> allowed) {
        UUID employeeProjectId = employeeProject(t.getEmployeeId());
        return (allowed == null || allowed.contains(employeeProjectId))
                && (projectId == null || projectId.equals(employeeProjectId));
    }

    // --- timekeeper project scoping ----------------------------------

    /** employeeId of the logged-in user, or null. */
    private UUID currentEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object p = auth != null ? auth.getPrincipal() : null;
        if (p instanceof AuthenticatedUser au && au.userId() != null) {
            return appUserRepo.findById(au.userId()).map(AppUser::getEmployeeId).orElse(null);
        }
        return null;
    }

    /** Projects the current user is limited to; null = unrestricted (admin/manager). */
    private Set<UUID> restrictedProjects() {
        UUID empId = currentEmployeeId();
        if (empId == null) {
            return null;
        }
        List<UUID> projs = timekeeperService.allowedProjectIds(empId);
        return projs.isEmpty() ? null : new HashSet<>(projs);
    }

    /** Employee's current project (from the latest assignment), or null. */
    private UUID employeeProject(UUID employeeId) {
        return defaultAllocation(employeeId)[0];
    }

    private void assertTimesheetEligible(Employee employee) {
        UUID[] alloc = defaultAllocation(employee.getId());
        if (alloc[0] == null) {
            throw new BusinessRuleException("employee.project.required",
                    "Assign the employee to a project before generating a timesheet.");
        }
    }

    private boolean isTimesheetEligible(Employee employee) {
        return employee != null && employeeProject(employee.getId()) != null;
    }

    /** A roster/crew membership counts for a period when its date window overlaps it:
     *  joined on/before the period end, and not ended before the period start. */
    private static boolean activeForPeriod(LocalDate from, LocalDate to, PayrollPeriod period) {
        boolean joinedInTime = from == null || !from.isAfter(period.getEndDate());
        boolean notEnded = to == null || !to.isBefore(period.getStartDate());
        return joinedInTime && notEnded;
    }

    private static boolean employmentOverlapsPeriod(Employee employee, PayrollPeriod period) {
        return isEmployedOnOrBefore(employee.getHireDate(), period.getEndDate())
                && isNotTerminatedBefore(employee.getTerminationDate(), period.getStartDate());
    }

    private static boolean isEmployedOn(Employee employee, LocalDate date) {
        return isEmployedOnOrBefore(employee.getHireDate(), date)
                && isNotTerminatedBefore(employee.getTerminationDate(), date);
    }

    private static boolean isEmployedOnOrBefore(LocalDate hireDate, LocalDate date) {
        return hireDate == null || !hireDate.isAfter(date);
    }

    private static boolean isNotTerminatedBefore(LocalDate terminationDate, LocalDate date) {
        return terminationDate == null || !terminationDate.isBefore(date);
    }

    private String empLabel(UUID employeeId) {
        return employeeRepo.findById(employeeId)
                .map(e -> e.getEmployeeNumber() + " " + (e.getFirstName() + " " + e.getLastName()).trim())
                .orElse(employeeId.toString());
    }

    // --- per-project period lock -------------------------------------

    /** Lock status of a project within a period: OPEN (default) / LOCKED / CLOSED. */
    private String projectStatus(UUID periodId, UUID projectId) {
        return projectStatus(periodId, projectId, "ALL");
    }

    private String projectStatus(UUID periodId, UUID projectId, String payGroup) {
        if (periodId == null || projectId == null) {
            return "OPEN";
        }
        UUID companyId = TenantContext.requireCompanyId();
        String all = periodProjectRepo.findByCompanyIdAndPeriodIdAndProjectIdAndPayGroup(companyId, periodId, projectId, "ALL")
                .map(PayrollPeriodProject::getStatus).orElse("OPEN");
        String group = "ALL".equals(payGroup) ? all
                : periodProjectRepo.findByCompanyIdAndPeriodIdAndProjectIdAndPayGroup(companyId, periodId, projectId, payGroup)
                .map(PayrollPeriodProject::getStatus).orElse("OPEN");
        return strongestStatus(all, group);
    }

    private static String strongestStatus(String a, String b) {
        if ("CLOSED".equals(a) || "CLOSED".equals(b)) {
            return "CLOSED";
        }
        if ("LOCKED".equals(a) || "LOCKED".equals(b)) {
            return "LOCKED";
        }
        return "OPEN";
    }

    /** Block edits when the timesheet's project is locked/closed for its period. */
    private void assertEditable(Timesheet ts) {
        UUID project = employeeProject(ts.getEmployeeId());
        String st = projectStatus(ts.getPeriodId(), project, payGroup(ts.getEmployeeId()));
        if (!"OPEN".equals(st)) {
            throw new BusinessRuleException("period.project.locked",
                    "This project is " + st + " for the period — timesheets are read-only.");
        }
    }

    /** Lock a project for a period after validating its timesheets are ready. */
    public Map<String, Object> lockProject(UUID periodId, UUID projectId, String payGroup) {
        UUID companyId = TenantContext.requireCompanyId();
        String group = normalizePayGroup(payGroup);
        periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodId));
        List<String> blockers = projectReadiness(companyId, periodId, projectId, group);
        if (!blockers.isEmpty()) {
            throw new BusinessRuleException("period.project.not.ready",
                    "Cannot lock — timesheets not ready: " + String.join("  •  ", blockers));
        }
        setProjectStatus(companyId, periodId, projectId, group, "LOCKED");
        return Map.of("status", "LOCKED", "payGroup", group);
    }

    public Map<String, Object> closeProject(UUID periodId, UUID projectId, String payGroup) {
        UUID companyId = TenantContext.requireCompanyId();
        String group = normalizePayGroup(payGroup);
        PayrollPeriodProject pp = periodProjectRepo
                .findByCompanyIdAndPeriodIdAndProjectIdAndPayGroup(companyId, periodId, projectId, group).orElse(null);
        if (pp == null || !"LOCKED".equals(pp.getStatus())) {
            throw new BusinessRuleException("period.project.close.state", "Lock the project before closing it.");
        }
        pp.setStatus("CLOSED");
        pp.setClosedAt(Instant.now());
        periodProjectRepo.save(pp);
        return Map.of("status", "CLOSED");
    }

    public Map<String, Object> reopenProject(UUID periodId, UUID projectId, String payGroup) {
        UUID companyId = TenantContext.requireCompanyId();
        String group = normalizePayGroup(payGroup);
        setProjectStatus(companyId, periodId, projectId, group, "OPEN");
        return Map.of("status", "OPEN", "payGroup", group);
    }

    private void setProjectStatus(UUID companyId, UUID periodId, UUID projectId, String payGroup, String status) {
        PayrollPeriodProject pp = periodProjectRepo
                .findByCompanyIdAndPeriodIdAndProjectIdAndPayGroup(companyId, periodId, projectId, payGroup)
                .orElseGet(() -> {
                    PayrollPeriodProject n = new PayrollPeriodProject();
                    n.setCompanyId(companyId);
                    n.setPeriodId(periodId);
                    n.setProjectId(projectId);
                    n.setPayGroup(payGroup);
                    return n;
                });
        pp.setStatus(status);
        if ("LOCKED".equals(status)) {
            pp.setLockedAt(Instant.now());
        }
        if ("OPEN".equals(status)) {
            pp.setLockedAt(null);
            pp.setClosedAt(null);
        }
        periodProjectRepo.save(pp);
    }

    /** Reasons a project/pay group can't be locked: unapproved timesheets, or rostered employees with none. */
    private List<String> projectReadiness(UUID companyId, UUID periodId, UUID projectId, String payGroup) {
        PayrollPeriod period = periodRepo.findById(periodId).orElseThrow();
        List<String> blockers = new java.util.ArrayList<>();
        // existing timesheets for this project/pay group that are not approved yet
        for (Timesheet t : timesheetRepo.findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(
                companyId, period.getPeriodYear(), period.getPeriodMonth())) {
            if (projectId.equals(employeeProject(t.getEmployeeId()))
                    && payGroupMatches(t.getEmployeeId(), payGroup)
                    && !APPROVED.equals(t.getStatus()) && !LOCKED.equals(t.getStatus())) {
                blockers.add(empLabel(t.getEmployeeId()) + " (still " + t.getStatus() + ")");
            }
        }
        // rostered employees of the project/pay group with no timesheet at all
        Set<UUID> done = new HashSet<>();
        for (Timesheet t : timesheetRepo.findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(
                companyId, period.getPeriodYear(), period.getPeriodMonth())) {
            if (payGroupMatches(t.getEmployeeId(), payGroup)) {
                done.add(t.getEmployeeId());
            }
        }
        for (EmployeeShift es : employeeShiftRepo.findByCompanyIdOrderByEffectiveFromDesc(companyId)) {
            Employee employee = employeeRepo.findById(es.getEmployeeId()).orElse(null);
            if (employee != null
                    && activeForPeriod(es.getEffectiveFrom(), es.getEffectiveTo(), period)
                    && employmentOverlapsPeriod(employee, period)
                    && projectId.equals(employeeProject(es.getEmployeeId()))
                    && payGroupMatches(es.getEmployeeId(), payGroup)
                    && !done.contains(es.getEmployeeId())) {
                blockers.add(empLabel(es.getEmployeeId()) + " (no timesheet)");
                done.add(es.getEmployeeId());
            }
        }
        return blockers;
    }

    private boolean payGroupMatches(UUID employeeId, String payGroup) {
        return "ALL".equals(payGroup) || payGroup.equals(payGroup(employeeId));
    }

    private String payGroup(UUID employeeId) {
        return employeeRepo.findById(employeeId)
                .map(Employee::getPayStatus)
                .map(TimesheetService::normalizePayGroup)
                .orElse("ALL");
    }

    private static String normalizePayGroup(String payGroup) {
        if (payGroup == null || payGroup.isBlank()) {
            return "ALL";
        }
        String p = payGroup.trim().toUpperCase();
        if (p.contains("DAILY")) {
            return "DAILY";
        }
        if (p.contains("MONTH")) {
            return "MONTHLY";
        }
        return "ALL";
    }

    /** Per-project lock status for a period (for the Calendar screen). */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> projectLockStatuses(UUID periodId, String payGroup) {
        UUID companyId = TenantContext.requireCompanyId();
        String group = normalizePayGroup(payGroup);
        Map<UUID, String> byProject = new java.util.LinkedHashMap<>();
        projectRepo.findByCompanyIdOrderByName(companyId).forEach(p ->
                byProject.put(p.getId(), p.getCode() + " - " + p.getName()));
        Map<UUID, String> allStatusByProject = new HashMap<>();
        Map<UUID, String> groupStatusByProject = new HashMap<>();
        for (PayrollPeriodProject pp : periodProjectRepo.findByPeriodId(periodId)) {
            String ppGroup = normalizePayGroup(pp.getPayGroup());
            if ("ALL".equals(ppGroup)) {
                allStatusByProject.put(pp.getProjectId(), pp.getStatus());
            }
            if (group.equals(ppGroup)) {
                groupStatusByProject.put(pp.getProjectId(), pp.getStatus());
            }
        }
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Map.Entry<UUID, String> e : byProject.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("projectId", e.getKey().toString());
            row.put("projectLabel", e.getValue());
            row.put("payGroup", group);
            row.put("status", strongestStatus(
                    allStatusByProject.getOrDefault(e.getKey(), "OPEN"),
                    groupStatusByProject.getOrDefault(e.getKey(), "OPEN")));
            out.add(row);
        }
        return out;
    }

    private void assertProjectAllowed(UUID employeeId) {
        Set<UUID> allowed = restrictedProjects();
        if (allowed != null && !allowed.contains(employeeProject(employeeId))) {
            throw new BusinessRuleException("timekeeper.scope",
                    "You are not assigned to this employee's project.");
        }
    }

    @Transactional(readOnly = true)
    public TimesheetDto get(UUID id) {
        Timesheet t = getEntity(id);
        return toFullDto(t);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> eligibleEmployees(UUID periodId) {
        UUID companyId = TenantContext.requireCompanyId();
        PayrollPeriod period = periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodId));
        Set<UUID> allowed = restrictedProjects();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Employee employee : employeeRepo.findByCompanyIdOrderByEmployeeNumber(companyId)) {
            UUID projectId = employeeProject(employee.getId());
            if (projectId == null || (allowed != null && !allowed.contains(projectId))) {
                continue;
            }
            if (!employmentOverlapsPeriod(employee, period)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", employee.getId());
            row.put("employeeNumber", employee.getEmployeeNumber());
            row.put("firstName", employee.getFirstName());
            row.put("lastName", employee.getLastName());
            row.put("status", employee.getStatus());
            row.put("hireDate", employee.getHireDate());
            row.put("terminationDate", employee.getTerminationDate());
            row.put("projectId", projectId);
            out.add(row);
        }
        return out;
    }

    // --- generation --------------------------------------------------

    public TimesheetDto generate(GenerateTimesheetRequest req) {
        UUID companyId = TenantContext.requireCompanyId();
        Employee employee = employeeRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + req.getEmployeeId()));
        assertTimesheetEligible(employee);
        assertProjectAllowed(req.getEmployeeId());

        // A timesheet must live inside an OPEN payroll period.
        PayrollPeriod period = periodRepo.findById(req.getPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + req.getPeriodId()));
        if (!"OPEN".equals(period.getStatus())) {
            throw new BusinessRuleException("period.not.open",
                    "The period is " + period.getStatus() + ". Reopen it to edit timesheets.");
        }
        if (!employmentOverlapsPeriod(employee, period)) {
            throw new BusinessRuleException("employee.not.active.period",
                    "Employee was not active during this payroll period.");
        }
        UUID employeeProjectId = employeeProject(req.getEmployeeId());
        String pst = projectStatus(req.getPeriodId(), employeeProjectId, payGroup(req.getEmployeeId()));
        if (!"OPEN".equals(pst)) {
            throw new BusinessRuleException("period.project.locked",
                    "This employee's project is " + pst + " for the period — can't generate.");
        }
        int year = period.getPeriodYear();
        int month = period.getPeriodMonth();

        // Resolve the shift: explicit > roster (effective on period start) > company default.
        Shift shift = req.getShiftId() != null
                ? resolveShift(companyId, req.getShiftId())
                : resolveRosterShift(companyId, req.getEmployeeId(), period.getStartDate());
        if (shift == null) {
            throw new BusinessRuleException("timesheet.shift.required",
                    "Assign the employee to a shift for this project before generating a timesheet.");
        }
        validateShiftProject(shift, employeeProjectId);

        Timesheet existing = timesheetRepo
                .findByCompanyIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                        companyId, req.getEmployeeId(), year, month)
                .orElse(null);
        if (existing != null) {
            if (!req.isOverwrite()) {
                throw new BusinessRuleException("timesheet.exists",
                        "A timesheet already exists for this employee and period.");
            }
            if (!DRAFT.equals(existing.getStatus())) {
                throw new BusinessRuleException("timesheet.not.draft",
                        "Only a DRAFT timesheet can be regenerated (current status: " + existing.getStatus() + ").");
            }
            dayRepo.deleteByTimesheetId(existing.getId());
        }

        Timesheet ts = existing != null ? existing : new Timesheet();
        ts.setCompanyId(companyId);
        ts.setEmployeeId(req.getEmployeeId());
        ts.setPeriodId(period.getId());
        ts.setPeriodYear(year);
        ts.setPeriodMonth(month);
        ts.setShiftId(shift != null ? shift.getId() : null);
        ts.setStatus(DRAFT);
        ts = timesheetRepo.save(ts);

        // Lookups for classification.
        Map<String, TimeType> typesByCode = new HashMap<>();
        for (TimeType tt : timeTypeRepo.findByCompanyIdOrderBySortOrderAscNameAsc(companyId)) {
            typesByCode.put(tt.getCode(), tt);
        }
        YearMonth ym = YearMonth.of(year, month);
        Set<LocalDate> holidays = new HashSet<>();
        for (PublicHoliday h : holidayRepo.findByCompanyIdAndHolidayDateBetween(
                companyId, ym.atDay(1), ym.atEndOfMonth())) {
            holidays.add(h.getHolidayDate());
        }
        UUID[] alloc = defaultAllocation(req.getEmployeeId());

        BigDecimal standard = shift != null && shift.getStandardHours() != null
                ? shift.getStandardHours() : BigDecimal.ZERO;
        String weeklyOff = shift != null ? shift.getWeeklyOff() : null;

        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            TimesheetDay day = new TimesheetDay();
            day.setTimesheetId(ts.getId());
            day.setWorkDate(date);
            day.setShiftId(shift != null ? shift.getId() : null);
            day.setProjectId(alloc[0]);
            day.setCostCodeId(alloc[1]);

            String code;
            if (isWeeklyOff(date, weeklyOff)) {
                code = "W";
                day.setPlannedHours(BigDecimal.ZERO);
                day.setWorkedHours(BigDecimal.ZERO);
            } else if (holidays.contains(date)) {
                code = "H";
                day.setPlannedHours(BigDecimal.ZERO);
                day.setWorkedHours(BigDecimal.ZERO);
            } else {
                code = "N";
                day.setPlannedHours(standard);
                day.setWorkedHours(standard);
                if (shift != null && shift.getStartTime() != null && shift.getEndTime() != null) {
                    day.setActualIn(shift.getStartTime());
                    day.setActualOut(shift.getEndTime());
                }
            }
            TimeType tt = typesByCode.get(code);
            day.setTimeTypeId(tt != null ? tt.getId() : null);
            day.setOtHours(BigDecimal.ZERO);
            if (!isEmployedOn(employee, date)) {
                TimeType nt = typesByCode.get("U");
                day.setTimeTypeId(nt != null ? nt.getId() : null);
                day.setPlannedHours(BigDecimal.ZERO);
                day.setWorkedHours(BigDecimal.ZERO);
                day.setOtHours(BigDecimal.ZERO);
                day.setActualIn(null);
                day.setActualOut(null);
                day.setProjectId(null);
                day.setCostCodeId(null);
            }
            dayRepo.save(day);
        }

        recomputeTotals(ts);
        return toFullDto(timesheetRepo.save(ts));
    }

    /** Generate timesheets for every employee rostered in the period (skips existing). */
    public Map<String, Integer> generateBulk(UUID periodId) {
        UUID companyId = TenantContext.requireCompanyId();
        PayrollPeriod period = periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodId));
        if (!"OPEN".equals(period.getStatus())) {
            throw new BusinessRuleException("period.not.open",
                    "The period is " + period.getStatus() + ". Reopen it to add timesheets.");
        }
        Set<UUID> allowed = restrictedProjects();
        Set<UUID> emps = new java.util.LinkedHashSet<>();
        for (EmployeeShift es : employeeShiftRepo.findByCompanyIdOrderByEffectiveFromDesc(companyId)) {
            Employee emp = employeeRepo.findById(es.getEmployeeId()).orElse(null);
            if (activeForPeriod(es.getEffectiveFrom(), es.getEffectiveTo(), period)
                    && isTimesheetEligible(emp)
                    && employmentOverlapsPeriod(emp, period)
                    && (allowed == null || allowed.contains(employeeProject(es.getEmployeeId())))) {
                emps.add(es.getEmployeeId());
            }
        }
        int created = 0;
        int skipped = 0;
        for (UUID empId : emps) {
            boolean exists = timesheetRepo.findByCompanyIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                    companyId, empId, period.getPeriodYear(), period.getPeriodMonth()).isPresent();
            if (exists) {
                skipped++;
                continue;
            }
            GenerateTimesheetRequest req = new GenerateTimesheetRequest();
            req.setEmployeeId(empId);
            req.setPeriodId(periodId);
            generate(req);
            created++;
        }
        return Map.of("created", created, "skipped", skipped);
    }

    /** Generate timesheets for the members of one crew (each on the member's shift).
     *  Returns created/skipped counts plus a message for every member that was skipped. */
    public Map<String, Object> generateByCrew(UUID crewId, UUID periodId) {
        UUID companyId = TenantContext.requireCompanyId();
        PayrollPeriod period = periodRepo.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + periodId));
        if (!"OPEN".equals(period.getStatus())) {
            throw new BusinessRuleException("period.not.open",
                    "The period is " + period.getStatus() + ". Reopen it to add timesheets.");
        }
        Set<UUID> allowed = restrictedProjects();
        // Most-recent membership row per employee (rows are ordered effectiveFrom desc).
        Map<UUID, CrewMember> latest = new java.util.LinkedHashMap<>();
        for (CrewMember m : crewMemberRepo.findByCrewIdOrderByEffectiveFromDesc(crewId)) {
            latest.putIfAbsent(m.getEmployeeId(), m);
        }
        int created = 0;
        int skipped = 0;
        List<String> messages = new java.util.ArrayList<>();
        for (CrewMember m : latest.values()) {
            UUID empId = m.getEmployeeId();
            if (!activeForPeriod(m.getEffectiveFrom(), m.getEffectiveTo(), period)) {
                skipped++;
                messages.add(empLabel(empId) + " — not in the crew during this period (member from "
                        + m.getEffectiveFrom() + ").");
                continue;
            }
            if (allowed != null && !allowed.contains(employeeProject(empId))) {
                skipped++;
                messages.add(empLabel(empId) + " — outside your project scope.");
                continue;
            }
            Employee emp = employeeRepo.findById(empId).orElse(null);
            if (!isTimesheetEligible(emp)) {
                skipped++;
                messages.add(empLabel(empId) + " - not assigned to a project.");
                continue;
            }
            if (!employmentOverlapsPeriod(emp, period)) {
                skipped++;
                messages.add(empLabel(empId) + " - outside the employee service dates.");
                continue;
            }
            boolean exists = timesheetRepo.findByCompanyIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                    companyId, empId, period.getPeriodYear(), period.getPeriodMonth()).isPresent();
            if (exists) {
                skipped++;
                messages.add(empLabel(empId) + " — already has a timesheet for this period.");
                continue;
            }
            GenerateTimesheetRequest req = new GenerateTimesheetRequest();
            req.setEmployeeId(empId);
            req.setPeriodId(periodId);
            req.setShiftId(m.getShiftId());
            generate(req);
            created++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("messages", messages);
        return result;
    }

    /** Submit every DRAFT timesheet in the period. */
    public Map<String, Integer> submitAll(int year, int month, UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        Set<UUID> allowed = restrictedProjects();
        int submitted = 0;
        for (Timesheet t : timesheetRepo.findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(companyId, year, month)) {
            if (DRAFT.equals(t.getStatus()) && matchesProjectScope(t, projectId, allowed)) {
                submit(t.getId());
                submitted++;
            }
        }
        return Map.of("submitted", submitted);
    }

    /** Approve every SUBMITTED timesheet in the period. */
    public Map<String, Integer> approveAll(int year, int month, UUID projectId) {
        UUID companyId = TenantContext.requireCompanyId();
        Set<UUID> allowed = restrictedProjects();
        int approved = 0;
        for (Timesheet t : timesheetRepo.findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(companyId, year, month)) {
            if (SUBMITTED.equals(t.getStatus()) && matchesProjectScope(t, projectId, allowed)) {
                approve(t.getId());
                approved++;
            }
        }
        return Map.of("approved", approved);
    }

    // --- editing -----------------------------------------------------

    /** Replace the editable fields of every supplied day, then recompute. DRAFT only. */
    public TimesheetDto saveDays(UUID timesheetId, List<TimesheetDayDto> dayDtos) {
        Timesheet ts = getEntity(timesheetId);
        requireDraft(ts);
        assertEditable(ts);
        Employee employee = employeeRepo.findById(ts.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + ts.getEmployeeId()));

        Map<UUID, TimesheetDay> byId = new HashMap<>();
        for (TimesheetDay d : dayRepo.findByTimesheetIdOrderByWorkDate(timesheetId)) {
            byId.put(d.getId(), d);
        }
        Map<UUID, Shift> shiftCache = new HashMap<>();
        Map<UUID, TimeType> typeCache = new HashMap<>();
        Map<UUID, Map<String, ShiftDay>> weekCache = new HashMap<>();
        UUID notEmployedTypeId = timeTypeRepo.findByCompanyIdOrderBySortOrderAscNameAsc(ts.getCompanyId()).stream()
                .filter(t -> "U".equals(t.getCode()))
                .findFirst()
                .map(TimeType::getId)
                .orElse(null);
        boolean monthly = isMonthlyPaid(ts);
        boolean otEligibleSave = isOtEligible(ts);

        for (TimesheetDayDto dto : dayDtos) {
            TimesheetDay day = byId.get(dto.getId());
            if (day == null) {
                continue;
            }
            if (!isEmployedOn(employee, day.getWorkDate())) {
                forceNotEmployedDay(day, notEmployedTypeId);
                dayRepo.save(day);
                saveDayCosts(day.getId(), List.of());
                continue;
            }
            day.setShiftId(dto.getShiftId());
            day.setTimeTypeId(dto.getTimeTypeId());
            day.setActualIn(dto.getActualIn());
            day.setActualOut(dto.getActualOut());
            if (dto.getPlannedHours() != null) {
                day.setPlannedHours(dto.getPlannedHours());
            }
            if (dto.getWorkedHours() != null) {
                day.setWorkedHours(dto.getWorkedHours());
            }
            day.setProjectId(dto.getProjectId());
            day.setCostCodeId(dto.getCostCodeId());
            day.setRemarks(dto.getRemarks());
            recomputeDay(day, ts, shiftCache, typeCache, weekCache, monthly, otEligibleSave);
            validateDayAllocation(day, dto.getCosts());
            validateCostSplit(day, dto.getCosts());
            dayRepo.save(day);
            saveDayCosts(day.getId(), dto.getCosts());
        }
        recomputeTotals(ts);
        return toFullDto(timesheetRepo.save(ts));
    }

    private void forceNotEmployedDay(TimesheetDay day, UUID notEmployedTypeId) {
        day.setTimeTypeId(notEmployedTypeId);
        day.setPlannedHours(BigDecimal.ZERO);
        day.setActualIn(null);
        day.setActualOut(null);
        day.setWorkedHours(BigDecimal.ZERO);
        day.setOtHours(BigDecimal.ZERO);
        day.setNormalHours(BigDecimal.ZERO);
        day.setDeclaredOtHours(BigDecimal.ZERO);
        day.setUndeclaredOtHours(BigDecimal.ZERO);
        day.setIneligibleOtHours(BigDecimal.ZERO);
        day.setProjectId(null);
        day.setCostCodeId(null);
        day.setRemarks(null);
    }

    /** A worked day needs a default allocation unless detailed cost splits replace it. */
    private void validateDayAllocation(TimesheetDay day, List<TimesheetDayCostDto> costs) {
        BigDecimal worked = day.getWorkedHours() != null ? day.getWorkedHours() : BigDecimal.ZERO;
        if (worked.compareTo(BigDecimal.ZERO) <= 0 || hasActiveCostRows(costs)) {
            return;
        }
        if (day.getProjectId() == null || day.getCostCodeId() == null) {
            throw new BusinessRuleException("timesheet.allocation.required",
                    "Day " + day.getWorkDate() + ": worked hours require a project and cost code.");
        }
    }

    /** When a day is split across cost codes, the split must add up to the worked hours. */
    private void validateCostSplit(TimesheetDay day, List<TimesheetDayCostDto> costs) {
        if (!hasActiveCostRows(costs)) {
            return;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (TimesheetDayCostDto c : costs) {
            if (!isActiveCostRow(c)) {
                continue;
            }
            if (c.getProjectId() == null || c.getCostCodeId() == null) {
                throw new BusinessRuleException("timesheet.cost.split.required",
                        "Day " + day.getWorkDate() + ": each cost split line requires a project and cost code.");
            }
            sum = sum.add(c.getHours() != null ? c.getHours() : BigDecimal.ZERO);
        }
        BigDecimal allocatable = allocatableCostHours(day);
        if (sum.subtract(allocatable).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BusinessRuleException("timesheet.cost.split.mismatch",
                    "Day " + day.getWorkDate() + ": cost-code hours (" + sum
                            + ") must equal the costed hours (" + allocatable + ").");
        }
    }

    private static BigDecimal allocatableCostHours(TimesheetDay day) {
        BigDecimal normal = day.getNormalHours() != null ? day.getNormalHours() : BigDecimal.ZERO;
        BigDecimal overtime = day.getOtHours() != null ? day.getOtHours() : BigDecimal.ZERO;
        return normal.add(overtime);
    }

    private static boolean hasActiveCostRows(List<TimesheetDayCostDto> costs) {
        if (costs == null || costs.isEmpty()) {
            return false;
        }
        return costs.stream().anyMatch(TimesheetService::isActiveCostRow);
    }

    private static boolean isActiveCostRow(TimesheetDayCostDto c) {
        return c != null && (c.getProjectId() != null || c.getCostCodeId() != null
                || (c.getHours() != null && c.getHours().compareTo(BigDecimal.ZERO) != 0));
    }

    /** Replace a day's cost-code allocation rows (legacy PAYIN HR_CC1..8). */
    private void saveDayCosts(UUID dayId, List<TimesheetDayCostDto> costs) {
        dayCostRepo.deleteByTimesheetDayId(dayId);
        if (costs == null) {
            return;
        }
        for (TimesheetDayCostDto c : costs) {
            if (!isActiveCostRow(c)) {
                continue;
            }
            TimesheetDayCost e = new TimesheetDayCost();
            e.setTimesheetDayId(dayId);
            e.setProjectId(c.getProjectId());
            e.setCostCodeId(c.getCostCodeId());
            e.setHours(c.getHours() != null ? c.getHours() : BigDecimal.ZERO);
            dayCostRepo.save(e);
        }
    }

    // --- lifecycle ---------------------------------------------------

    public TimesheetDto submit(UUID id) {
        Timesheet ts = getEntity(id);
        if (!DRAFT.equals(ts.getStatus())) {
            throw new BusinessRuleException("timesheet.submit.state", "Only a DRAFT timesheet can be submitted.");
        }
        assertEditable(ts);
        recomputeTotals(ts);
        ts.setStatus(SUBMITTED);
        ts.setSubmittedAt(Instant.now());
        return toFullDto(timesheetRepo.save(ts));
    }

    public TimesheetDto approve(UUID id) {
        Timesheet ts = getEntity(id);
        if (!SUBMITTED.equals(ts.getStatus())) {
            throw new BusinessRuleException("timesheet.approve.state", "Only a SUBMITTED timesheet can be approved.");
        }
        assertEditable(ts);
        ts.setStatus(APPROVED);
        ts.setApprovedAt(Instant.now());
        ts.setApprovedBy(currentUsername());
        return toFullDto(timesheetRepo.save(ts));
    }

    public TimesheetDto lock(UUID id) {
        Timesheet ts = getEntity(id);
        if (!APPROVED.equals(ts.getStatus())) {
            throw new BusinessRuleException("timesheet.lock.state", "Only an APPROVED timesheet can be locked.");
        }
        ts.setStatus(LOCKED);
        return toFullDto(timesheetRepo.save(ts));
    }

    /** Send a SUBMITTED/APPROVED timesheet back to DRAFT, invalidating the approval. */
    public TimesheetDto reopen(UUID id) {
        Timesheet ts = getEntity(id);
        if (LOCKED.equals(ts.getStatus())) {
            throw new BusinessRuleException("timesheet.reopen.locked", "A LOCKED timesheet cannot be reopened.");
        }
        assertEditable(ts);
        if (DRAFT.equals(ts.getStatus())) {
            return toFullDto(ts);
        }
        ts.setStatus(DRAFT);
        ts.setSubmittedAt(null);
        ts.setApprovedAt(null);
        ts.setApprovedBy(null);
        return toFullDto(timesheetRepo.save(ts));
    }

    public void delete(UUID id) {
        Timesheet ts = getEntity(id);
        if (LOCKED.equals(ts.getStatus())) {
            throw new BusinessRuleException("timesheet.delete.locked", "A LOCKED timesheet cannot be deleted.");
        }
        assertEditable(ts);
        timesheetRepo.delete(ts);
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "system";
        }
        return auth.getName();
    }

    // --- computation -------------------------------------------------

    private void recomputeDay(TimesheetDay day, Timesheet ts, Map<UUID, Shift> shiftCache,
                              Map<UUID, TimeType> typeCache, Map<UUID, Map<String, ShiftDay>> weekCache,
                              boolean isMonthly, boolean otEligible) {
        Shift shift = resolveDayShift(day, ts, shiftCache);

        // Worked hours from clock when both punches are present.
        if (day.getActualIn() != null && day.getActualOut() != null) {
            int breakMin = shift != null ? shift.getBreakMinutes() : 0;
            boolean cross = shift != null && shift.isCrossesMidnight();
            day.setWorkedHours(clockHours(day.getActualIn(), day.getActualOut(), breakMin, cross));
        }
        BigDecimal worked = day.getWorkedHours() != null ? day.getWorkedHours() : BigDecimal.ZERO;

        String category = "REGULAR";
        boolean paid = true;
        if (day.getTimeTypeId() != null) {
            TimeType tt = typeCache.computeIfAbsent(day.getTimeTypeId(),
                    k -> timeTypeRepo.findById(k).orElse(null));
            if (tt != null && tt.getCategory() != null) {
                category = tt.getCategory();
                paid = tt.isPaid();
            }
        }

        // Sample-week values for this weekday (legacy PAYCAL normal + declared OT).
        ShiftDay sample = sampleDay(shift, day.getWorkDate(), weekCache);
        BigDecimal sampleNormal = sample != null ? sample.getNormalHours()
                : (shift != null && shift.getStandardHours() != null ? shift.getStandardHours() : BigDecimal.ZERO);
        BigDecimal declaredLimit = sample != null ? sample.getDeclaredOt() : BigDecimal.ZERO;

        BigDecimal normal;
        BigDecimal ot;
        if ("REST".equals(category) || "HOLIDAY".equals(category)) {
            // Weekend/holiday are paid time types. Any hours actually worked on the
            // day are premium overtime.
            normal = paid && isMonthly ? sampleNormal : BigDecimal.ZERO;
            ot = worked;
        } else if (isNonWorking(category)) {
            // Paid leave/absence-like types keep planned hours for payroll;
            // unpaid types become deduction hours.
            normal = paid ? sampleNormal : BigDecimal.ZERO;
            ot = BigDecimal.ZERO;
            worked = BigDecimal.ZERO;
            day.setWorkedHours(BigDecimal.ZERO);
        } else {
            normal = worked.min(sampleNormal);
            ot = worked.subtract(sampleNormal).max(BigDecimal.ZERO);
        }
        BigDecimal declared = ot.min(declaredLimit);
        BigDecimal undeclared = ot.subtract(declared).max(BigDecimal.ZERO);

        // Overtime eligibility gate: if the employee's overtime category is not
        // eligible, the overtime worked is recorded separately (ineligible_ot_hours)
        // — not paid and not shown to the employee — and payable OT becomes zero.
        BigDecimal ineligibleOt = BigDecimal.ZERO;
        if (!otEligible) {
            ineligibleOt = ot;
            ot = BigDecimal.ZERO;
            declared = BigDecimal.ZERO;
            undeclared = BigDecimal.ZERO;
        }

        day.setNormalHours(normal);
        day.setOtHours(ot);
        day.setDeclaredOtHours(declared);
        day.setUndeclaredOtHours(undeclared);
        day.setIneligibleOtHours(ineligibleOt);
    }

    /** True unless the employee's overtime category is explicitly not eligible. */
    private boolean isOtEligible(Timesheet ts) {
        return employeeRepo.findById(ts.getEmployeeId())
                .map(com.hrms.employee.domain.Employee::getOvertimeCategoryCode)
                .filter(code -> code != null && !code.isBlank())
                .flatMap(code -> overtimeCategoryRepo.findByCompanyIdAndCode(ts.getCompanyId(), code))
                .map(com.hrms.reference.domain.OvertimeCategory::isOtEligible)
                .orElse(true);
    }

    private static boolean isNonWorking(String category) {
        return "ABSENCE".equals(category) || "LEAVE".equals(category) || "SICK".equals(category)
                || "ACCIDENT".equals(category) || "RR".equals(category) || "UNPAID".equals(category)
                || "NOT_EMPLOYED".equals(category);
    }

    /** Sample-week row for the weekday of {@code date} on the given shift. */
    private ShiftDay sampleDay(Shift shift, LocalDate date, Map<UUID, Map<String, ShiftDay>> weekCache) {
        if (shift == null || date == null) {
            return null;
        }
        Map<String, ShiftDay> week = weekCache.computeIfAbsent(shift.getId(), id -> {
            Map<String, ShiftDay> m = new HashMap<>();
            for (ShiftDay sd : shiftDayRepo.findByShiftId(id)) {
                m.put(sd.getDayOfWeek(), sd);
            }
            return m;
        });
        return week.get(date.getDayOfWeek().name().substring(0, 3));
    }

    /** True when the timesheet's employee is monthly-paid (gets weekend/holiday normal pay). */
    private boolean isMonthlyPaid(Timesheet ts) {
        return employeeRepo.findById(ts.getEmployeeId())
                .map(e -> e.getPayStatus() != null && e.getPayStatus().toUpperCase().contains("MONTH"))
                .orElse(false);
    }

    private void recomputeTotals(Timesheet ts) {
        Map<UUID, Shift> shiftCache = new HashMap<>();
        Map<UUID, TimeType> typeCache = new HashMap<>();
        Map<UUID, Map<String, ShiftDay>> weekCache = new HashMap<>();
        boolean monthly = isMonthlyPaid(ts);
        boolean otEligible = isOtEligible(ts);
        BigDecimal worked = BigDecimal.ZERO;
        BigDecimal ot = BigDecimal.ZERO;
        BigDecimal absence = BigDecimal.ZERO;
        for (TimesheetDay day : dayRepo.findByTimesheetIdOrderByWorkDate(ts.getId())) {
            recomputeDay(day, ts, shiftCache, typeCache, weekCache, monthly, otEligible);
            dayRepo.save(day);
            worked = worked.add(day.getWorkedHours() != null ? day.getWorkedHours() : BigDecimal.ZERO);
            ot = ot.add(day.getOtHours() != null ? day.getOtHours() : BigDecimal.ZERO);
            if (day.getTimeTypeId() != null) {
                TimeType tt = typeCache.get(day.getTimeTypeId());
                if (tt != null && "ABSENCE".equals(tt.getCategory())) {
                    absence = absence.add(BigDecimal.ONE);
                }
            }
        }
        ts.setTotalWorkedHours(worked);
        ts.setTotalOtHours(ot);
        ts.setTotalAbsenceDays(absence);
    }

    private static BigDecimal clockHours(LocalTime in, LocalTime out, int breakMinutes, boolean crossesMidnight) {
        long minutes = Duration.between(in, out).toMinutes();
        if (minutes < 0 || crossesMidnight) {
            minutes += 24 * 60;
        }
        minutes -= breakMinutes;
        if (minutes < 0) {
            minutes = 0;
        }
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
    }

    private static boolean isWeeklyOff(LocalDate date, String weeklyOff) {
        if (weeklyOff == null || weeklyOff.isBlank()) {
            return false;
        }
        String token = date.getDayOfWeek().name().substring(0, 3);
        for (String part : weeklyOff.split(",")) {
            if (part.trim().equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    // --- helpers -----------------------------------------------------

    private Shift resolveShift(UUID companyId, UUID shiftId) {
        if (shiftId != null) {
            Shift shift = shiftRepo.findById(shiftId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + shiftId));
            if (!companyId.equals(shift.getCompanyId())) {
                throw new ResourceNotFoundException("Shift not found: " + shiftId);
            }
            return shift;
        }
        return null;
    }

    private void validateShiftProject(Shift shift, UUID projectId) {
        if (!shiftProjectAllowed(shift, projectId)) {
            throw new BusinessRuleException("timesheet.shift.project.mismatch",
                    "Shift must be shared or belong to the employee's assigned project.");
        }
    }

    private static boolean shiftProjectAllowed(Shift shift, UUID projectId) {
        return shift == null || shift.getProjectId() == null || shift.getProjectId().equals(projectId);
    }

    /** The shift the employee is rostered on, effective on the given date. */
    private Shift resolveRosterShift(UUID companyId, UUID employeeId, LocalDate onDate) {
        for (EmployeeShift es : employeeShiftRepo.findByCompanyIdAndEmployeeIdOrderByEffectiveFromDesc(companyId, employeeId)) {
            if (es.isEffectiveOn(onDate)) {
                return shiftRepo.findById(es.getShiftId()).orElse(null);
            }
        }
        return null;
    }

    private Shift resolveDayShift(TimesheetDay day, Timesheet ts, Map<UUID, Shift> cache) {
        UUID sid = day.getShiftId() != null ? day.getShiftId() : ts.getShiftId();
        if (sid == null) {
            return null;
        }
        return cache.computeIfAbsent(sid, k -> shiftRepo.findById(k).orElse(null));
    }

    /** Default project/cost code from the employee's most recent assignment. */
    private UUID[] defaultAllocation(UUID employeeId) {
        List<Assignment> assignments = assignmentRepo.findByEmployeeIdOrderByEffectiveFromDesc(employeeId);
        for (Assignment a : assignments) {
            if (a.getProjectId() != null && "ACTIVE".equalsIgnoreCase(a.getStatus())) {
                return new UUID[]{a.getProjectId(), a.getCostCodeId()};
            }
        }
        return new UUID[]{null, null};
    }

    /**
     * Roll a timesheet's daily records up into a per-category summary (worked /
     * overtime / absence / leave hours and day counts) — the figures the payroll
     * engine will consume, surfaced for review.
     */
    @Transactional(readOnly = true)
    public TimesheetSummaryDto summarize(UUID timesheetId) {
        Timesheet ts = getEntity(timesheetId);
        TimesheetSummaryDto dto = new TimesheetSummaryDto();
        dto.setTimesheetId(ts.getId());
        dto.setEmployeeId(ts.getEmployeeId());
        dto.setPeriodYear(ts.getPeriodYear());
        dto.setPeriodMonth(ts.getPeriodMonth());
        dto.setStatus(ts.getStatus());
        employeeRepo.findById(ts.getEmployeeId())
                .ifPresent(e -> dto.setEmployeeName((e.getFirstName() + " " + e.getLastName()).trim()));

        Map<UUID, TimeType> typeCache = new HashMap<>();
        // category -> [days, hours, paid]
        Map<String, int[]> daysByCat = new HashMap<>();
        Map<String, BigDecimal> hoursByCat = new HashMap<>();
        Map<String, Boolean> paidByCat = new HashMap<>();
        Map<AllocationKey, BigDecimal> allocationHours = new HashMap<>();

        BigDecimal normal = BigDecimal.ZERO;
        BigDecimal ot = BigDecimal.ZERO;
        BigDecimal restHours = BigDecimal.ZERO;
        BigDecimal holidayHours = BigDecimal.ZERO;
        BigDecimal absenceHours = BigDecimal.ZERO;
        BigDecimal leaveHours = BigDecimal.ZERO;
        BigDecimal standardDayHours = BigDecimal.ZERO;
        int worked = 0, absence = 0, leave = 0, rest = 0, holiday = 0, total = 0;

        for (TimesheetDay day : dayRepo.findByTimesheetIdOrderByWorkDate(timesheetId)) {
            total++;
            String category = "REGULAR";
            boolean paid = true;
            if (day.getTimeTypeId() != null) {
                TimeType tt = typeCache.computeIfAbsent(day.getTimeTypeId(),
                        id -> timeTypeRepo.findById(id).orElse(null));
                if (tt != null) {
                    category = tt.getCategory();
                    paid = tt.isPaid();
                }
            }
            BigDecimal dayNormal = nz(day.getNormalHours());
            BigDecimal dayOt = nz(day.getOtHours());
            BigDecimal dayWorked = nz(day.getWorkedHours());
            BigDecimal dayPlanned = nz(day.getPlannedHours());
            if (standardDayHours.compareTo(BigDecimal.ZERO) == 0 && dayNormal.compareTo(BigDecimal.ZERO) > 0) {
                standardDayHours = dayNormal;
            }
            ot = ot.add(dayOt);
            collectAllocationHours(day, allocationHours);

            // category lines: worked categories accrue worked hours, non-working
            // categories (absence/leave) accrue the planned (would-have-worked) hours.
            BigDecimal lineHours = isNonWorking(category) ? dayPlanned
                    : (("REST".equals(category) || "HOLIDAY".equals(category)) ? dayNormal : dayNormal);
            daysByCat.computeIfAbsent(category, k -> new int[1])[0]++;
            hoursByCat.merge(category, lineHours, BigDecimal::add);
            paidByCat.putIfAbsent(category, paid);

            switch (category) {
                case "ABSENCE", "UNPAID" -> { absence++; absenceHours = absenceHours.add(dayPlanned); }
                case "LEAVE", "SICK", "ACCIDENT" -> { leave++; leaveHours = leaveHours.add(dayPlanned); }
                case "REST" -> { rest++; restHours = restHours.add(dayNormal); }
                case "HOLIDAY" -> { holiday++; holidayHours = holidayHours.add(dayNormal); }
                default -> {
                    normal = normal.add(dayNormal);
                    if (dayWorked.signum() > 0) worked++;
                }
            }
        }
        if (isMonthlyPaid(ts) && standardDayHours.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxRegularHours = new BigDecimal("30")
                    .subtract(BigDecimal.valueOf(rest + holiday))
                    .max(BigDecimal.ZERO)
                    .multiply(standardDayHours);
            if (normal.compareTo(maxRegularHours) > 0) {
                normal = maxRegularHours;
                hoursByCat.put("REGULAR", normal);
            }
        }

        dto.setNormalHours(normal);
        dto.setOvertimeHours(ot);
        dto.setRestHours(restHours);
        dto.setHolidayHours(holidayHours);
        dto.setWorkedHours(normal.add(restHours).add(holidayHours).add(ot));
        dto.setAbsenceHours(absenceHours);
        dto.setLeaveHours(leaveHours);
        dto.setWorkedDays(worked);
        dto.setAbsenceDays(absence);
        dto.setLeaveDays(leave);
        dto.setRestDays(rest);
        dto.setHolidayDays(holiday);
        dto.setTotalDays(total);

        List<TimesheetSummaryDto.CategoryLine> lines = new ArrayList<>();
        for (String cat : daysByCat.keySet()) {
            lines.add(new TimesheetSummaryDto.CategoryLine(
                    cat, daysByCat.get(cat)[0],
                    hoursByCat.getOrDefault(cat, BigDecimal.ZERO),
                    paidByCat.getOrDefault(cat, true)));
        }
        lines.sort((a, b) -> a.getCategory().compareTo(b.getCategory()));
        dto.setLines(lines);
        dto.setAllocationLines(toAllocationLines(allocationHours));
        return dto;
    }

    private void collectAllocationHours(TimesheetDay day, Map<AllocationKey, BigDecimal> allocationHours) {
        List<TimesheetDayCost> costs = dayCostRepo.findByTimesheetDayId(day.getId());
        if (!costs.isEmpty()) {
            for (TimesheetDayCost c : costs) {
                if (c.getProjectId() != null && c.getCostCodeId() != null) {
                    allocationHours.merge(new AllocationKey(c.getProjectId(), c.getCostCodeId()), nz(c.getHours()), BigDecimal::add);
                }
            }
            return;
        }

        BigDecimal hours = allocatableCostHours(day);
        if (hours.signum() > 0 && day.getProjectId() != null && day.getCostCodeId() != null) {
            allocationHours.merge(new AllocationKey(day.getProjectId(), day.getCostCodeId()), hours, BigDecimal::add);
        }
    }

    private List<TimesheetSummaryDto.AllocationLine> toAllocationLines(Map<AllocationKey, BigDecimal> allocationHours) {
        Map<UUID, Project> projects = new HashMap<>();
        Map<UUID, CostCode> costCodes = new HashMap<>();
        List<TimesheetSummaryDto.AllocationLine> out = new ArrayList<>();
        for (Map.Entry<AllocationKey, BigDecimal> e : allocationHours.entrySet()) {
            AllocationKey key = e.getKey();
            Project p = projects.computeIfAbsent(key.projectId(), id -> projectRepo.findById(id).orElse(null));
            CostCode c = costCodes.computeIfAbsent(key.costCodeId(), id -> costCodeRepo.findById(id).orElse(null));
            out.add(new TimesheetSummaryDto.AllocationLine(
                    key.projectId(), p != null ? p.getCode() : null, p != null ? p.getName() : null,
                    key.costCodeId(), c != null ? c.getCode() : null, c != null ? c.getName() : null,
                    e.getValue()));
        }
        out.sort((a, b) -> {
            int pc = String.valueOf(a.getProjectCode()).compareTo(String.valueOf(b.getProjectCode()));
            if (pc != 0) {
                return pc;
            }
            return String.valueOf(a.getCostCode()).compareTo(String.valueOf(b.getCostCode()));
        });
        return out;
    }

    private record AllocationKey(UUID projectId, UUID costCodeId) {}

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private Timesheet getEntity(UUID id) {
        return timesheetRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found: " + id));
    }

    private void requireDraft(Timesheet ts) {
        if (!DRAFT.equals(ts.getStatus())) {
            throw new BusinessRuleException("timesheet.edit.state",
                    "Only a DRAFT timesheet can be edited (current status: " + ts.getStatus() + ").");
        }
    }

    private TimesheetDto toHeaderDto(Timesheet t) {
        TimesheetDto dto = baseDto(t);
        employeeRepo.findById(t.getEmployeeId()).ifPresent(e -> {
            dto.setEmployeeName((e.getFirstName() + " " + e.getLastName()).trim());
            dto.setEmployeeNumber(e.getEmployeeNumber());
        });
        return dto;
    }

    private TimesheetDto toFullDto(Timesheet t) {
        TimesheetDto dto = toHeaderDto(t);
        Map<UUID, String> typeCodes = new HashMap<>();
        for (TimeType tt : timeTypeRepo.findByCompanyIdOrderBySortOrderAscNameAsc(t.getCompanyId())) {
            typeCodes.put(tt.getId(), tt.getCode());
        }
        for (TimesheetDay d : dayRepo.findByTimesheetIdOrderByWorkDate(t.getId())) {
            dto.getDays().add(toDayDto(d, typeCodes));
        }
        return dto;
    }

    private TimesheetDto baseDto(Timesheet t) {
        TimesheetDto dto = new TimesheetDto();
        dto.setId(t.getId());
        dto.setCompanyId(t.getCompanyId());
        dto.setEmployeeId(t.getEmployeeId());
        dto.setPeriodId(t.getPeriodId());
        dto.setPeriodYear(t.getPeriodYear());
        dto.setPeriodMonth(t.getPeriodMonth());
        dto.setShiftId(t.getShiftId());
        dto.setStatus(t.getStatus());
        dto.setTotalWorkedHours(t.getTotalWorkedHours());
        dto.setTotalOtHours(t.getTotalOtHours());
        dto.setTotalAbsenceDays(t.getTotalAbsenceDays());
        dto.setSubmittedAt(t.getSubmittedAt());
        dto.setApprovedAt(t.getApprovedAt());
        dto.setApprovedBy(t.getApprovedBy());
        return dto;
    }

    private TimesheetDayDto toDayDto(TimesheetDay d, Map<UUID, String> typeCodes) {
        TimesheetDayDto dto = new TimesheetDayDto();
        dto.setId(d.getId());
        dto.setTimesheetId(d.getTimesheetId());
        dto.setWorkDate(d.getWorkDate());
        dto.setShiftId(d.getShiftId());
        dto.setTimeTypeId(d.getTimeTypeId());
        dto.setTimeTypeCode(d.getTimeTypeId() != null ? typeCodes.get(d.getTimeTypeId()) : null);
        dto.setPlannedHours(d.getPlannedHours());
        dto.setActualIn(d.getActualIn());
        dto.setActualOut(d.getActualOut());
        dto.setWorkedHours(d.getWorkedHours());
        dto.setIneligibleOtHours(d.getIneligibleOtHours());
        dto.setOtHours(d.getOtHours());
        dto.setNormalHours(d.getNormalHours());
        dto.setDeclaredOtHours(d.getDeclaredOtHours());
        dto.setUndeclaredOtHours(d.getUndeclaredOtHours());
        dto.setProjectId(d.getProjectId());
        dto.setCostCodeId(d.getCostCodeId());
        dto.setRemarks(d.getRemarks());
        for (TimesheetDayCost c : dayCostRepo.findByTimesheetDayId(d.getId())) {
            TimesheetDayCostDto cd = new TimesheetDayCostDto();
            cd.setId(c.getId());
            cd.setProjectId(c.getProjectId());
            cd.setCostCodeId(c.getCostCodeId());
            cd.setHours(c.getHours());
            dto.getCosts().add(cd);
        }
        return dto;
    }
}

