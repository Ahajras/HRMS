package com.hrms.crew.web;

import com.hrms.crew.dto.TimekeeperProjectDto;
import com.hrms.crew.service.TimekeeperService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST API for timekeeper-to-project assignment (FTDD Vol.1 Ch.4). */
@RestController
@RequestMapping("/api/v1/timekeeper-projects")
public class TimekeeperController {

    private final TimekeeperService service;

    public TimekeeperController(TimekeeperService service) {
        this.service = service;
    }

    @GetMapping
    public List<TimekeeperProjectDto> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimekeeperProjectDto create(@Valid @RequestBody TimekeeperProjectDto dto) {
        return service.create(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
