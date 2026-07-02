package com.hrms.timesheet.web;

import com.hrms.timesheet.dto.TimeTypePayrollRuleDto;
import com.hrms.timesheet.service.TimeTypePayrollRuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/time-types/{timeTypeId}/payroll-rules")
public class TimeTypePayrollRuleController {

    private final TimeTypePayrollRuleService service;

    public TimeTypePayrollRuleController(TimeTypePayrollRuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<TimeTypePayrollRuleDto> list(@PathVariable UUID timeTypeId) {
        return service.findByTimeType(timeTypeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeTypePayrollRuleDto save(@PathVariable UUID timeTypeId, @RequestBody TimeTypePayrollRuleDto dto) {
        return service.save(timeTypeId, dto);
    }

    @DeleteMapping("/{componentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID timeTypeId, @PathVariable UUID componentId) {
        service.delete(timeTypeId, componentId);
        return ResponseEntity.noContent().build();
    }
}
