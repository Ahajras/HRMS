package com.hrms.payroll.web;

import com.hrms.payroll.dto.ProvisionDtos;
import com.hrms.payroll.service.ProvisionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provisions")
public class ProvisionController {

    private final ProvisionService service;

    public ProvisionController(ProvisionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProvisionDtos.RunDto> list(@RequestParam(required = false) UUID periodId) {
        return service.list(periodId);
    }

    @GetMapping("/{id}")
    public ProvisionDtos.RunDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionDtos.RunDto calculate(@RequestBody ProvisionDtos.CreateRequest request) {
        return service.calculate(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
