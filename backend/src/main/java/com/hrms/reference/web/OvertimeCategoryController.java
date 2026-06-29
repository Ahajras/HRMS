package com.hrms.reference.web;

import com.hrms.reference.dto.OvertimeCategoryDto;
import com.hrms.reference.service.OvertimeCategoryService;
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

@RestController
@RequestMapping("/api/v1/overtime-categories")
public class OvertimeCategoryController {

    private final OvertimeCategoryService service;

    public OvertimeCategoryController(OvertimeCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<OvertimeCategoryDto> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OvertimeCategoryDto create(@Valid @RequestBody OvertimeCategoryDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public OvertimeCategoryDto update(@PathVariable UUID id, @Valid @RequestBody OvertimeCategoryDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
