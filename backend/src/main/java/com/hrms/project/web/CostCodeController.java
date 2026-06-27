package com.hrms.project.web;

import com.hrms.project.dto.CostCodeDto;
import com.hrms.project.service.CostCodeService;
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

@RestController
@RequestMapping("/api/v1/cost-codes")
public class CostCodeController {

    private final CostCodeService service;

    public CostCodeController(CostCodeService service) {
        this.service = service;
    }

    @GetMapping
    public List<CostCodeDto> find(@RequestParam(required = false) UUID projectId) {
        return projectId != null ? service.findByProject(projectId) : service.findAll();
    }

    @GetMapping("/{id}")
    public CostCodeDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CostCodeDto create(@Valid @RequestBody CostCodeDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public CostCodeDto update(@PathVariable UUID id, @Valid @RequestBody CostCodeDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
