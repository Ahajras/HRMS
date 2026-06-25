package com.hrms.reference.web;

import com.hrms.reference.dto.LookupValueDto;
import com.hrms.reference.service.LookupValueService;
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

/**
 * Configurable dropdown sources. {@code GET /api/v1/lookups?category=GENDER}.
 */
@RestController
@RequestMapping("/api/v1/lookups")
public class LookupValueController {

    private final LookupValueService service;

    public LookupValueController(LookupValueService service) {
        this.service = service;
    }

    @GetMapping
    public List<LookupValueDto> find(@RequestParam(required = false) String category) {
        return category != null ? service.findByCategory(category) : service.findAll();
    }

    @GetMapping("/{id}")
    public LookupValueDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LookupValueDto create(@Valid @RequestBody LookupValueDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public LookupValueDto update(@PathVariable UUID id, @Valid @RequestBody LookupValueDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
