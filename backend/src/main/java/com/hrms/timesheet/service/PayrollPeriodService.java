package com.hrms.timesheet.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.domain.PayrollCalendar;
import com.hrms.timesheet.domain.PayrollPeriod;
import com.hrms.timesheet.domain.PayrollWeek;
import com.hrms.timesheet.domain.Timesheet;
import com.hrms.timesheet.dto.PayrollPeriodDto;
import com.hrms.timesheet.dto.PayrollWeekDto;
import com.hrms.timesheet.repository.PayrollPeriodRepository;
import com.hrms.timesheet.repository.PayrollWeekRepository;
import com.hrms.timesheet.repository.TimesheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Payroll periods & weeks (FTDD Vol.1 Ch.4). A year is GENERATED into 12 monthly
 * periods, each split into weeks. Lifecycle OPEN -> LOCKED -> CLOSED gates
 * timesheet edits and (later) payroll processing.
 */
@Service
@Transactional
public class PayrollPeriodService {

    private static final String OPEN = "OPEN";
    private static final String LOCKED = "LOCKED";
    private static final String CLOSED = "CLOSED";

    private static final Map<String, DayOfWeek> DOW = Map.of(
            "MON", DayOfWeek.MONDAY, "TUE", DayOfWeek.TUESDAY, "WED", DayOfWeek.WEDNESDAY,
            "THU", DayOfWeek.THURSDAY, "FRI", DayOfWeek.FRIDAY, "SAT", DayOfWeek.SATURDAY,
            "SUN", DayOfWeek.SUNDAY);

    private final PayrollPeriodRepository periodRepo;
    private final PayrollWeekRepository weekRepo;
    private final TimesheetRepository timesheetRepo;
    private final PayrollCalendarService calendarService;

    public PayrollPeriodService(PayrollPeriodRepository periodRepo, PayrollWeekRepository weekRepo,
                                TimesheetRepository timesheetRepo, PayrollCalendarService calendarService) {
        this.periodRepo = periodRepo;
        this.weekRepo = weekRepo;
        this.timesheetRepo = timesheetRepo;
        this.calendarService = calendarService;
    }

    /** Generate the 12 monthly periods (and their weeks) for a year. Idempotent. */
    public List<PayrollPeriodDto> generateYear(UUID calendarId, int year) {
        UUID companyId = TenantContext.requireCompanyId();
        PayrollCalendar cal = calendarService.resolve(companyId, calendarId);
        DayOfWeek weekStart = DOW.getOrDefault(cal.getWeekStart(), DayOfWeek.SATURDAY);

        for (int month = 1; month <= 12; month++) {
            if (periodRepo.findByCompanyIdAndCalendarIdAndPeriodYearAndPeriodMonth(
                    companyId, cal.getId(), year, month).isPresent()) {
                continue; // already generated
            }
            YearMonth ym = YearMonth.of(year, month);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();

            PayrollPeriod p = new PayrollPeriod();
            p.setCompanyId(companyId);
            p.setCalendarId(cal.getId());
            p.setPeriodYear(year);
            p.setPeriodMonth(month);
            p.setName(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + year);
            p.setStartDate(start);
            p.setEndDate(end);
            p.setPayDate(end);
            p.setStatus(OPEN);
            p = periodRepo.save(p);

            generateWeeks(companyId, p, weekStart);
        }
        return listPeriodsByYear(year);
    }

    private void generateWeeks(UUID companyId, PayrollPeriod period, DayOfWeek weekStart) {
        DayOfWeek weekEndDow = weekStart.minus(1);
        LocalDate cursor = period.getStartDate();
        LocalDate monthEnd = period.getEndDate();
        int no = 1;
        while (!cursor.isAfter(monthEnd)) {
            LocalDate weekEnd = cursor;
            while (weekEnd.getDayOfWeek() != weekEndDow && weekEnd.isBefore(monthEnd)) {
                weekEnd = weekEnd.plusDays(1);
            }
            PayrollWeek w = new PayrollWeek();
            w.setCompanyId(companyId);
            w.setPeriodId(period.getId());
            w.setWeekNo(no++);
            w.setStartDate(cursor);
            w.setEndDate(weekEnd);
            weekRepo.save(w);
            cursor = weekEnd.plusDays(1);
        }
    }

    @Transactional(readOnly = true)
    public List<PayrollPeriodDto> listPeriods() {
        UUID companyId = TenantContext.requireCompanyId();
        return periodRepo.findByCompanyIdOrderByPeriodYearDescPeriodMonthDesc(companyId)
                .stream().map(this::toHeaderDto).toList();
    }

    @Transactional(readOnly = true)
    public List<PayrollPeriodDto> listPeriodsByYear(int year) {
        UUID companyId = TenantContext.requireCompanyId();
        return periodRepo.findByCompanyIdAndPeriodYearOrderByPeriodMonth(companyId, year)
                .stream().map(this::toHeaderDto).toList();
    }

    @Transactional(readOnly = true)
    public PayrollPeriodDto get(UUID id) {
        return toFullDto(getEntity(id));
    }

    public PayrollPeriodDto lock(UUID id) {
        PayrollPeriod p = getEntity(id);
        if (!OPEN.equals(p.getStatus())) {
            throw new BusinessRuleException("period.lock.state", "Only an OPEN period can be locked.");
        }
        p.setStatus(LOCKED);
        p.setLockedAt(Instant.now());
        return toFullDto(periodRepo.save(p));
    }

    public PayrollPeriodDto close(UUID id) {
        PayrollPeriod p = getEntity(id);
        if (!LOCKED.equals(p.getStatus())) {
            throw new BusinessRuleException("period.close.state", "Lock the period before closing it.");
        }
        p.setStatus(CLOSED);
        p.setClosedAt(Instant.now());
        return toFullDto(periodRepo.save(p));
    }

    public PayrollPeriodDto reopen(UUID id) {
        PayrollPeriod p = getEntity(id);
        if (CLOSED.equals(p.getStatus())) {
            throw new BusinessRuleException("period.reopen.closed", "A CLOSED period cannot be reopened.");
        }
        p.setStatus(OPEN);
        p.setLockedAt(null);
        return toFullDto(periodRepo.save(p));
    }

    public void delete(UUID id) {
        PayrollPeriod p = getEntity(id);
        List<Timesheet> linked = timesheetRepo.findByPeriodId(id);
        if (!linked.isEmpty()) {
            throw new BusinessRuleException("period.delete.has-timesheets",
                    "This period has timesheets. Delete them first.");
        }
        periodRepo.delete(p); // weeks cascade in DB
    }

    private PayrollPeriod getEntity(UUID id) {
        return periodRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found: " + id));
    }

    private PayrollPeriodDto toHeaderDto(PayrollPeriod e) {
        PayrollPeriodDto dto = new PayrollPeriodDto();
        dto.setId(e.getId());
        dto.setCompanyId(e.getCompanyId());
        dto.setCalendarId(e.getCalendarId());
        dto.setPeriodYear(e.getPeriodYear());
        dto.setPeriodMonth(e.getPeriodMonth());
        dto.setName(e.getName());
        dto.setStartDate(e.getStartDate());
        dto.setEndDate(e.getEndDate());
        dto.setPayDate(e.getPayDate());
        dto.setStatus(e.getStatus());
        dto.setLockedAt(e.getLockedAt());
        dto.setClosedAt(e.getClosedAt());
        return dto;
    }

    private PayrollPeriodDto toFullDto(PayrollPeriod e) {
        PayrollPeriodDto dto = toHeaderDto(e);
        dto.setWeeks(weekRepo.findByPeriodIdOrderByWeekNo(e.getId()).stream().map(w -> {
            PayrollWeekDto wd = new PayrollWeekDto();
            wd.setId(w.getId());
            wd.setPeriodId(w.getPeriodId());
            wd.setWeekNo(w.getWeekNo());
            wd.setStartDate(w.getStartDate());
            wd.setEndDate(w.getEndDate());
            return wd;
        }).toList());
        return dto;
    }
}
