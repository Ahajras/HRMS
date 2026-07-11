package com.hrms.payroll.web;

import com.hrms.common.tenant.TenantContext;
import com.hrms.payroll.dto.ProvisionDtos;
import com.hrms.payroll.service.ProvisionService;
import com.hrms.timesheet.dto.BulkStatusJobDto;
import com.hrms.timesheet.service.BulkStatusJobService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/provisions")
public class ProvisionController {

    private final ProvisionService service;
    private final BulkStatusJobService bulkStatusJobService;

    public ProvisionController(ProvisionService service, BulkStatusJobService bulkStatusJobService) {
        this.service = service;
        this.bulkStatusJobService = bulkStatusJobService;
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

    @PostMapping("/calculate-jobs")
    public BulkStatusJobDto startCalculate(@RequestBody ProvisionDtos.CreateRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        return bulkStatusJobService.start("Calculating provisions", companyId, progress -> {
            ProvisionDtos.RunDto run = service.calculate(request, progress);
            Map<String, Object> result = new HashMap<>();
            result.put("runId", run.getId());
            result.put("employees", run.getEmployeeCount());
            result.put("eligible", run.getTotalEligibleAmount());
            result.put("provision", run.getTotalProvisionAmount());
            result.put("type", run.getProvisionType());
            result.put("payGroup", run.getPayGroup());
            return result;
        });
    }

    @GetMapping("/calculate-jobs/{id}")
    public BulkStatusJobDto getCalculateJob(@PathVariable UUID id) {
        return bulkStatusJobService.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
