package com.hrms.payroll.web;

import com.hrms.payroll.dto.PayrollCostReportDto;
import com.hrms.payroll.dto.PayrollRunDto;
import com.hrms.payroll.service.PayrollCostReportService;
import com.hrms.payroll.service.PayrollRunService;
import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.dto.BulkStatusJobDto;
import com.hrms.timesheet.service.BulkStatusJobService;
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
    private final BulkStatusJobService bulkStatusJobService;
    private final PayrollCostReportService costReportService;

    public PayrollRunController(PayrollRunService service, BulkStatusJobService bulkStatusJobService,
                                PayrollCostReportService costReportService) {
        this.service = service;
        this.bulkStatusJobService = bulkStatusJobService;
        this.costReportService = costReportService;
    }

    @GetMapping("/{id}/cost-report/summary")
    public java.util.List<com.hrms.payroll.dto.CostCodeLineDto> costReportSummary(@PathVariable UUID id) {
        return costReportService.buildSummary(id);
    }

    @GetMapping("/{id}/cost-report/employees")
    public com.hrms.common.web.PageResponse<com.hrms.payroll.dto.EmployeeCostBreakdownDto> costReportEmployees(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search) {
        return costReportService.pagedByEmployee(id, page, size, search);
    }

    @GetMapping("/{id}/results")
    public com.hrms.common.web.PageResponse<com.hrms.payroll.dto.PayrollResultDto> results(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search) {
        return service.pagedResults(id, page, size, search);
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

    @PostMapping("/{id}/calculate/start")
    public BulkStatusJobDto startCalculate(@PathVariable UUID id) {
        UUID companyId = TenantContext.requireCompanyId();
        return bulkStatusJobService.start("Calculating payroll", companyId,
                progress -> service.calculate(id, progress));
    }

    @GetMapping("/calculate-jobs/{jobId}")
    public BulkStatusJobDto getCalculateJob(@PathVariable UUID jobId) {
        return bulkStatusJobService.get(jobId);
    }

    @PostMapping("/{id}/approve")
    public PayrollRunDto approve(@PathVariable UUID id) {
        return service.approve(id);
    }

    @PostMapping("/{id}/lock")
    public PayrollRunDto lock(@PathVariable UUID id) {
        return service.lock(id);
    }

    @PostMapping("/{id}/reopen")
    public PayrollRunDto reopen(@PathVariable UUID id) {
        return service.reopen(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
