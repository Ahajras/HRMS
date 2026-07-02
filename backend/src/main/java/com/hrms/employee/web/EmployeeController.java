package com.hrms.employee.web;

import com.hrms.common.web.PageResponse;
import com.hrms.employee.dto.EmployeeDto;
import com.hrms.employee.dto.EmployeeSummaryDto;
import com.hrms.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    private final EmployeeService service;

    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<EmployeeDto> findAll(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String payStatus,
                                             @RequestParam(required = false) UUID projectId,
                                             @RequestParam(defaultValue = "false") boolean activeOnly,
                                             @RequestParam(defaultValue = "false") boolean assignedOnly,
                                             @RequestParam(defaultValue = "false") boolean unassigned,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return service.findAll(q, payStatus, projectId, activeOnly, assignedOnly, unassigned, pageable);
    }

    @GetMapping("/summary")
    public EmployeeSummaryDto summary(@RequestParam(required = false) String q,
                                      @RequestParam(required = false) UUID projectId) {
        return service.summary(q, projectId);
    }

    @GetMapping("/{id}")
    public EmployeeDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDto create(@Valid @RequestBody EmployeeDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public EmployeeDto update(@PathVariable UUID id, @Valid @RequestBody EmployeeDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
