package com.hrms.timesheet.web;

import com.hrms.timesheet.dto.EmployeeShiftDto;
import com.hrms.timesheet.service.EmployeeShiftService;
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
import java.util.UUID;

/** REST API for the shift roster — employee-to-shift assignment (FTDD Vol.1 Ch.4). */
@RestController
@RequestMapping("/api/v1/employee-shifts")
public class EmployeeShiftController {

    private final EmployeeShiftService service;

    public EmployeeShiftController(EmployeeShiftService service) {
        this.service = service;
    }

    @GetMapping
    public List<EmployeeShiftDto> findAll(@RequestParam(required = false) UUID employeeId) {
        return service.findAll(employeeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeShiftDto create(@Valid @RequestBody EmployeeShiftDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public EmployeeShiftDto update(@PathVariable UUID id, @Valid @RequestBody EmployeeShiftDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
