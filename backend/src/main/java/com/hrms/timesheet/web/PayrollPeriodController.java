package com.hrms.timesheet.web;

import com.hrms.timesheet.dto.PayrollPeriodDto;
import com.hrms.timesheet.service.PayrollPeriodService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST API for payroll periods (FTDD Vol.1 Ch.4). */
@RestController
@RequestMapping("/api/v1/payroll-periods")
public class PayrollPeriodController {

    private final PayrollPeriodService service;

    public PayrollPeriodController(PayrollPeriodService service) {
        this.service = service;
    }

    @GetMapping
    public List<PayrollPeriodDto> list(@RequestParam(required = false) Integer year) {
        return year != null ? service.listPeriodsByYear(year) : service.listPeriods();
    }

    @GetMapping("/{id}")
    public PayrollPeriodDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    /** Generate the 12 monthly periods (and weeks) for a year. Idempotent. */
    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PayrollPeriodDto> generate(@RequestParam int year,
                                           @RequestParam(required = false) UUID calendarId) {
        return service.generateYear(calendarId, year);
    }

    @PostMapping("/{id}/lock")
    public PayrollPeriodDto lock(@PathVariable UUID id) {
        return service.lock(id);
    }

    @PostMapping("/{id}/close")
    public PayrollPeriodDto close(@PathVariable UUID id) {
        return service.close(id);
    }

    @PostMapping("/{id}/reopen")
    public PayrollPeriodDto reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
