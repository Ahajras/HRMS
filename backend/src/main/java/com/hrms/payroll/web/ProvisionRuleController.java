package com.hrms.payroll.web;

import com.hrms.payroll.dto.ProvisionRuleDto;
import com.hrms.payroll.service.ProvisionRuleService;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/v1/provision-rules")
public class ProvisionRuleController {

    private final ProvisionRuleService service;

    public ProvisionRuleController(ProvisionRuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProvisionRuleDto> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionRuleDto create(@RequestBody ProvisionRuleDto dto) {
        return service.save(dto);
    }

    @PutMapping("/{id}")
    public ProvisionRuleDto update(@PathVariable UUID id, @RequestBody ProvisionRuleDto dto) {
        dto.setId(id);
        return service.save(dto);
    }

    @PostMapping("/initialize-defaults")
    public List<ProvisionRuleDto> initializeDefaults() {
        return service.initializeDefaults();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
