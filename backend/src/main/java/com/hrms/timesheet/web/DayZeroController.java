package com.hrms.timesheet.web;

import com.hrms.timesheet.dto.DayZeroDayDto;
import com.hrms.timesheet.service.TimesheetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Day Zero — a direct screen to correct an estimated day on an
 * already-locked period, for cases that don't go through the leave module
 * (e.g. a manual attendance correction discovered after the fact). */
@RestController
@RequestMapping("/api/v1/day-zero")
public class DayZeroController {

    private final TimesheetService service;

    public DayZeroController(TimesheetService service) {
        this.service = service;
    }

    @GetMapping("/employees/{employeeId}/days")
    public List<DayZeroDayDto> estimatedDays(@PathVariable UUID employeeId) {
        return service.findEstimatedDaysForEmployee(employeeId);
    }

    public record DayCorrection(UUID newTimeTypeId, java.math.BigDecimal workedHours) {
    }

    public record CorrectionRequest(Map<UUID, DayCorrection> corrections, String note) {
    }

    @PostMapping("/employees/{employeeId}/correct")
    public Map<String, Object> correct(@PathVariable UUID employeeId, @RequestBody CorrectionRequest request) {
        Map<UUID, TimesheetService.DayCorrectionRequest> mapped = new java.util.HashMap<>();
        request.corrections().forEach((dayId, c) ->
                mapped.put(dayId, new TimesheetService.DayCorrectionRequest(c.newTimeTypeId(), c.workedHours())));
        return service.applyDayZeroCorrection(employeeId, mapped, request.note());
    }
}
