package com.hrms.timesheet.web;

import com.hrms.timesheet.dto.ShiftDto;
import com.hrms.timesheet.service.ShiftService;
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

/** REST API for working shifts (FTDD Vol.1 Ch.4). */
@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftService service;

    public ShiftController(ShiftService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShiftDto> findAll(@RequestParam(required = false) UUID projectId) {
        return service.findAll(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftDto create(@Valid @RequestBody ShiftDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ShiftDto update(@PathVariable UUID id, @Valid @RequestBody ShiftDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
