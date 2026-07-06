package com.hrms.timesheet.web;

import com.hrms.timesheet.dto.GenerateTimesheetRequest;
import com.hrms.timesheet.dto.TimesheetDayDto;
import com.hrms.timesheet.dto.TimesheetDto;
import com.hrms.timesheet.dto.TimesheetSummaryDto;
import com.hrms.timesheet.service.TimesheetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** REST API for monthly timesheets (FTDD Vol.1 Ch.3). */
@RestController
@RequestMapping("/api/v1/timesheets")
public class TimesheetController {

    private final TimesheetService service;

    public TimesheetController(TimesheetService service) {
        this.service = service;
    }

    @GetMapping
    public List<TimesheetDto> listByPeriod(@RequestParam int year, @RequestParam int month,
                                           @RequestParam(required = false) UUID projectId) {
        return service.listByPeriod(year, month, projectId);
    }

    @GetMapping("/{id}")
    public TimesheetDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/summary")
    public TimesheetSummaryDto summary(@PathVariable UUID id) {
        return service.summarize(id);
    }

    @GetMapping("/eligible-employees")
    public List<Map<String, Object>> eligibleEmployees(@RequestParam UUID periodId) {
        return service.eligibleEmployees(periodId);
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public TimesheetDto generate(@Valid @RequestBody GenerateTimesheetRequest req) {
        return service.generate(req);
    }

    @PostMapping("/generate-bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Integer> generateBulk(@RequestParam UUID periodId,
                                             @RequestParam(required = false) UUID projectId) {
        return service.generateBulk(periodId, projectId);
    }

    @PostMapping("/generate-by-crew")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> generateByCrew(@RequestParam UUID crewId, @RequestParam UUID periodId) {
        return service.generateByCrew(crewId, periodId);
    }

    @PostMapping("/submit-all")
    public Map<String, Integer> submitAll(@RequestParam int year, @RequestParam int month,
                                          @RequestParam(required = false) UUID projectId) {
        return service.submitAll(year, month, projectId);
    }

    @PostMapping("/approve-all")
    public Map<String, Integer> approveAll(@RequestParam int year, @RequestParam int month,
                                           @RequestParam(required = false) UUID projectId) {
        return service.approveAll(year, month, projectId);
    }

    @PutMapping("/{id}/days")
    public TimesheetDto saveDays(@PathVariable UUID id, @RequestBody List<TimesheetDayDto> days) {
        return service.saveDays(id, days);
    }

    @PostMapping("/{id}/submit")
    public TimesheetDto submit(@PathVariable UUID id) {
        return service.submit(id);
    }

    @PostMapping("/{id}/approve")
    public TimesheetDto approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/lock")
    public TimesheetDto lock(@PathVariable UUID id) {
        return service.lock(id);
    }

    @PostMapping("/{id}/reopen")
    public TimesheetDto reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
