package com.hrms.payroll.web;

import com.hrms.payroll.dto.PayrollListingReportDto;
import com.hrms.payroll.dto.PayrollListingSummaryDto;
import com.hrms.payroll.service.PayrollReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll/reports")
public class PayrollReportController {

    private final PayrollReportService service;

    public PayrollReportController(PayrollReportService service) {
        this.service = service;
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
}
