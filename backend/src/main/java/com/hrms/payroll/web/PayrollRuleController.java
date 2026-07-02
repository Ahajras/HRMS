package com.hrms.payroll.web;

import com.hrms.payroll.dto.PayrollRuleDto;
import com.hrms.payroll.service.PayrollRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll-rules")
public class PayrollRuleController {
    private final PayrollRuleService service;

    public PayrollRuleController(PayrollRuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<PayrollRuleDto> list() {
        return service.list();
    }

    @PutMapping("/{id}")
    public PayrollRuleDto update(@PathVariable UUID id, @RequestBody PayrollRuleDto dto) {
        return service.update(id, dto);
    }
}
