package com.hrms.timesheet.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Assignment;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.AssignmentRepository;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.timesheet.domain.EmployeeShift;
import com.hrms.timesheet.domain.PayrollPeriod;
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
import com.hrms.timesheet.dto.TimesheetDto;
import com.hrms.timesheet.repository.EmployeeShiftRepository;
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

    public TimesheetService(TimesheetRepository timesheetRepo, TimesheetDayRepository dayRepo,
                            ShiftRepository shiftRepo, TimeTypeRepository timeTypeRepo,
                            PublicHolidayRepository holidayRepo, EmployeeRepository employeeRepo,
                            AssignmentRepository assignmentRepo, PayrollPeriodRepository periodRepo,
                            EmployeeShiftRepository employeeShiftRepo, ShiftDayRepository shiftDayRepo,
                            TimesheetDayCostRepository dayCostRepo) {
        this.timesheetRepo = timesheetRepo;
        this.dayRepo = dayRepo;
        this.shiftRepo = shiftRepo;
        this.timeTypeRepo = timeTypeRepo;
        this.holidayRepo = holidayRepo;
        this.employeeRepo = employeeRepo;
        this.assignmentRepo = assignmentRepo;
        this.shiftDayRepo = shiftDayRepo;
        this.dayCostRepo = dayCostRepo;
        this.periodRepo = periodRepo;
        this.employeeShiftRepo = employeeShiftRepo;
    }

    // --- queries -----------------------------------------------------

    @Transactional(readOnly = true)
    public List<TimesheetDto> listByPeriod(int year, int month) {
        UUID companyId = TenantContext.requireCompanyId();
        return timesheetRepo.findByCompanyIdAndPeriodYearAndPeriodMonthOrderByEmployeeId(companyId, year, month)
                .stream().map(t -> toHeaderDto(t)).toList();
    }

    @Transactional(readOnly = true)
    public TimesheetDto get(UUID id) {
        Timesheet t = getEntity(id);
        return toFullDto(t);
    }

    // --- generation --------------------------------------------------

    public TimesheetDto generate(GenerateTimesheetRequest req) {
        UUID companyId = TenantContext.requireCompanyId();
        employeeRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + req.getEmployeeId()));

        // A timesheet must live inside an OPEN payroll period.
        PayrollPeriod period = periodRepo.findById(req.getPeriodId())
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + req.getPeriodId()));
        if (!"OPEN".equals(period.getStatus())) {
            throw new BusinessRuleException("period.not.open",
                    "The period is " + period.getStatus() + ". Reopen it to edit timesheets.");
        }
        int year = period.getPeriodYear();
        int month = period.getPeriodMonth();

        // Resolve the shift: explicit > roster (effective on period start) > company default.
        Shift shift = req.getShiftId() != null
                ? resolveShift(companyId, req.getShiftId())
                : resolveRosterShift(companyId, req.getEmployeeId(), period.getStartDate());
        if (shift == null) {
            shift = resolveShift(companyId, null);
        }

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
                code = "REST";
                day.setPlannedHours(BigDecimal.ZERO);
                day.setWorkedHours(BigDecimal.ZERO);
            } else if (holidays.contains(date)) {
                code = "HOLIDAY";
                day.setPlannedHours(BigDecimal.ZERO);
                day.setWorkedHours(BigDecimal.ZERO);
            } else {
                code = "REGULAR";
                day.setPlannedHours(standard);
                day.setWorkedHours(standard);
            }
            TimeType tt = typesByCode.get(code);
            day.setTimeTypeId(tt != null ? tt.getId() : null);
            day.setOtHours(BigDecimal.ZERO);
            dayRepo.save(day);
        }

        recomputeTotals(ts);
        return toFullDto(timesheetRepo.save(ts));
    }

    // --- editing -----------------------------------------------------

    /** Replace the editable fields of every supplied day, then recompute. DRAFT only. */
    public TimesheetDto saveDays(UUID timesheetId, List<TimesheetDayDto> dayDtos) {
        Timesheet ts = getEntity(timesheetId);
        requireDraft(ts);

        Map<UUID, TimesheetDay> byId = new HashMap<>();
        for (TimesheetDay d : dayRepo.findByTimesheetIdOrderByWorkDate(timesheetId)) {
            byId.put(d.getId(), d);
        }
        Map<UUID, Shift> shiftCache = new HashMap<>();
        Map<UUID, TimeType> typeCache = new HashMap<>();
        Map<UUID, Map<String, ShiftDay>> weekCache = new HashMap<>();

        for (TimesheetDayDto dto : dayDtos) {
            TimesheetDay day = byId.get(dto.getId());
            if (day == null) {
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
            recomputeDay(day, ts, shiftCache, typeCache, weekCache);
            dayRepo.save(day);
            saveDayCosts(day.getId(), dto.getCosts());
        }
        recomputeTotals(ts);
        return toFullDto(timesheetRepo.save(ts));
    }

    /** Replace a day's cost-code allocation rows (legacy PAYIN HR_CC1..8). */
    private void saveDayCosts(UUID dayId, List<TimesheetDayCostDto> costs) {
        dayCostRepo.deleteByTimesheetDayId(dayId);
        if (costs == null) {
            return;
        }
        for (TimesheetDayCostDto c : costs) {
            if (c.getCostCodeId() == null && c.getProjectId() == null) {
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
                              Map<UUID, TimeType> typeCache, Map<UUID, Map<String, ShiftDay>> weekCache) {
        Shift shift = resolveDayShift(day, ts, shiftCache);

        // Worked hours from clock when both punches are present.
        if (day.getActualIn() != null && day.getActualOut() != null) {
            int breakMin = shift != null ? shift.getBreakMinutes() : 0;
            boolean cross = shift != null && shift.isCrossesMidnight();
            day.setWorkedHours(clockHours(day.getActualIn(), day.getActualOut(), breakMin, cross));
        }
        BigDecimal worked = day.getWorkedHours() != null ? day.getWorkedHours() : BigDecimal.ZERO;

        String category = "REGULAR";
        if (day.getTimeTypeId() != null) {
            TimeType tt = typeCache.computeIfAbsent(day.getTimeTypeId(),
                    k -> timeTypeRepo.findById(k).orElse(null));
            if (tt != null && tt.getCategory() != null) {
                category = tt.getCategory();
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
            normal = BigDecimal.ZERO;
            ot = worked; // every hour worked on a rest/holiday day is premium time
        } else if (isNonWorking(category)) {
            normal = BigDecimal.ZERO;
            ot = BigDecimal.ZERO;
        } else {
            normal = worked.min(sampleNormal);
            ot = worked.subtract(sampleNormal).max(BigDecimal.ZERO);
        }
        BigDecimal declared = ot.min(declaredLimit);
        BigDecimal undeclared = ot.subtract(declared).max(BigDecimal.ZERO);

        day.setNormalHours(normal);
        day.setOtHours(ot);
        day.setDeclaredOtHours(declared);
        day.setUndeclaredOtHours(undeclared);
    }

    private static boolean isNonWorking(String category) {
        return "ABSENCE".equals(category) || "LEAVE".equals(category) || "SICK".equals(category)
                || "ACCIDENT".equals(category) || "RR".equals(category) || "UNPAID".equals(category);
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

    private void recomputeTotals(Timesheet ts) {
        Map<UUID, Shift> shiftCache = new HashMap<>();
        Map<UUID, TimeType> typeCache = new HashMap<>();
        Map<UUID, Map<String, ShiftDay>> weekCache = new HashMap<>();
        BigDecimal worked = BigDecimal.ZERO;
        BigDecimal ot = BigDecimal.ZERO;
        BigDecimal absence = BigDecimal.ZERO;
        for (TimesheetDay day : dayRepo.findByTimesheetIdOrderByWorkDate(ts.getId())) {
            recomputeDay(day, ts, shiftCache, typeCache, weekCache);
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
            return shiftRepo.findById(shiftId)
                    .orElseThrow(() -> new ResourceNotFoundException("Shift not found: " + shiftId));
        }
        List<Shift> shifts = shiftRepo.findByCompanyIdOrderByCode(companyId);
        return shifts.isEmpty() ? null : shifts.get(0);
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
        if (!assignments.isEmpty()) {
            Assignment a = assignments.get(0);
            return new UUID[]{a.getProjectId(), a.getCostCodeId()};
        }
        return new UUID[]{null, null};
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
