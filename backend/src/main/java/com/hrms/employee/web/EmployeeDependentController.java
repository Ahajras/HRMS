package com.hrms.employee.web;

import com.hrms.employee.dto.EmployeeDependentDto;
import com.hrms.employee.service.EmployeeDependentService;
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
@RequestMapping("/api/v1/employee-dependents")
public class EmployeeDependentController {

    private final EmployeeDependentService service;

    public EmployeeDependentController(EmployeeDependentService service) {
        this.service = service;
    }

    @GetMapping
    public List<EmployeeDependentDto> findByEmployee(@RequestParam UUID employeeId) {
        return service.findByEmployee(employeeId);
    }

    @GetMapping("/{id}")
    public EmployeeDependentDto findById(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeDependentDto create(@Valid @RequestBody EmployeeDependentDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public EmployeeDependentDto update(@PathVariable UUID id, @Valid @RequestBody EmployeeDependentDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
