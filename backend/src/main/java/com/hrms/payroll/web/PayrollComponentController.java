package com.hrms.payroll.web;

import com.hrms.payroll.dto.PayrollComponentDto;
import com.hrms.payroll.service.PayrollComponentService;
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
@RequestMapping("/api/v1/payroll-components")
public class PayrollComponentController {

    private final PayrollComponentService service;

    public PayrollComponentController(PayrollComponentService service) {
        this.service = service;
    }

    @GetMapping
    public List<PayrollComponentDto> findAll(@RequestParam(required = false) String category) {
        return service.findAll(category);
    }

    @GetMapping("/{id}")
    public PayrollComponentDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PayrollComponentDto create(@Valid @RequestBody PayrollComponentDto dto) {
        return service.create(dto);
    }

    @PostMapping("/initialize-defaults")
    public List<PayrollComponentDto> initializeDefaults() {
        return service.initializeDefaults();
    }

    @PutMapping("/{id}")
    public PayrollComponentDto update(@PathVariable UUID id, @Valid @RequestBody PayrollComponentDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
