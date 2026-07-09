package com.hrms.payroll.web;

import com.hrms.payroll.dto.CostCodeLineDto;
import com.hrms.payroll.dto.EmployeeCostBreakdownDto;
import com.hrms.payroll.dto.PayrollCostControlReportDto;
import com.hrms.payroll.dto.PayrollListingReportDto;
import com.hrms.payroll.dto.PayrollListingSummaryDto;
import com.hrms.payroll.service.PayrollCostReportService;
import com.hrms.payroll.service.PayrollReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll/reports")
public class PayrollReportController {

    private final PayrollReportService service;
    private final PayrollCostReportService costReportService;

    public PayrollReportController(PayrollReportService service, PayrollCostReportService costReportService) {
        this.service = service;
        this.costReportService = costReportService;
    }

    @GetMapping("/payroll-listing/{runId}/summary")
    public PayrollListingSummaryDto payrollListingSummary(@PathVariable UUID runId) {
        return service.summary(runId);
    }

    @GetMapping("/payroll-listing/{runId}/rows")
    public com.hrms.common.web.PageResponse<PayrollListingReportDto.Row> payrollListingRows(
            @PathVariable UUID runId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search) {
        return service.pagedRows(runId, page, size, search);
    }

    /** Whole-period cost allocation — combines every project's run for the
     * period (or just one project, if given) into one report, since each
     * project is calculated as its own separate payroll run. */
    @GetMapping("/cost-allocation/summary")
    public List<CostCodeLineDto> costAllocationSummary(
            @RequestParam UUID periodId,
            @RequestParam(required = false) UUID projectId) {
        return costReportService.buildSummaryForPeriod(periodId, projectId);
    }

    @GetMapping("/cost-allocation/employees")
    public com.hrms.common.web.PageResponse<EmployeeCostBreakdownDto> costAllocationEmployees(
            @RequestParam UUID periodId,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search) {
        return costReportService.pagedByEmployeeForPeriod(periodId, projectId, page, size, search);
    }

    @GetMapping("/cost-control")
    public PayrollCostControlReportDto costControl(
            @RequestParam UUID periodId,
            @RequestParam(required = false) UUID projectId) {
        return costReportService.buildCostControl(periodId, projectId);
    }
}
