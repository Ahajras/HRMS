package com.hrms.payroll.web;

import com.hrms.payroll.dto.PayrollRunDto;
import com.hrms.payroll.service.PayrollRunService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll-runs")
public class PayrollRunController {

    private final PayrollRunService service;

    public PayrollRunController(PayrollRunService service) {
        this.service = service;
    }

    @GetMapping
    public List<PayrollRunDto> list(@RequestParam(required = false) UUID periodId) {
        return service.list(periodId);
    }

    @GetMapping("/{id}")
    public PayrollRunDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PayrollRunDto create(@RequestParam UUID periodId,
                                @RequestParam(required = false) UUID projectId,
                                @RequestParam(required = false) String payGroup) {
        return service.create(periodId, projectId, payGroup);
    }

    @PostMapping("/{id}/calculate")
    public PayrollRunDto calculate(@PathVariable UUID id) {
        return service.calculate(id);
    }

    @PostMapping("/{id}/approve")
    public PayrollRunDto approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/lock")
    public PayrollRunDto lock(@PathVariable UUID id) {
        return service.lock(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
