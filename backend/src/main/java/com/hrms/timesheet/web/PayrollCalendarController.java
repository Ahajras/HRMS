package com.hrms.timesheet.web;

import com.hrms.timesheet.dto.PayrollCalendarDto;
import com.hrms.timesheet.service.PayrollCalendarService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST API for payroll calendars (FTDD Vol.1 Ch.4). */
@RestController
@RequestMapping("/api/v1/payroll-calendars")
public class PayrollCalendarController {

    private final PayrollCalendarService service;

    public PayrollCalendarController(PayrollCalendarService service) {
        this.service = service;
    }

    @GetMapping
    public List<PayrollCalendarDto> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PayrollCalendarDto create(@Valid @RequestBody PayrollCalendarDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public PayrollCalendarDto update(@PathVariable UUID id, @Valid @RequestBody PayrollCalendarDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
