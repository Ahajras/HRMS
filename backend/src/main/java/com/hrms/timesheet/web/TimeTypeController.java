package com.hrms.timesheet.web;

import com.hrms.timesheet.dto.TimeTypeDto;
import com.hrms.timesheet.service.TimeTypeService;
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

/** REST API for configurable time types (FTDD Vol.1 Ch.5). */
@RestController
@RequestMapping("/api/v1/time-types")
public class TimeTypeController {

    private final TimeTypeService service;

    public TimeTypeController(TimeTypeService service) {
        this.service = service;
    }

    @GetMapping
    public List<TimeTypeDto> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeTypeDto create(@Valid @RequestBody TimeTypeDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public TimeTypeDto update(@PathVariable UUID id, @Valid @RequestBody TimeTypeDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
