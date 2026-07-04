package com.hrms.payroll.web;

import com.hrms.payroll.dto.PayrollListingReportDto;
import com.hrms.payroll.service.PayrollReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payroll/reports")
public class PayrollReportController {

    private final PayrollReportService service;

    public PayrollReportController(PayrollReportService service) {
        this.service = service;
    }

    @GetMapping("/payroll-listing/{runId}")
    public PayrollListingReportDto payrollListing(@PathVariable UUID runId) {
        return service.payrollListing(runId);
    }
}
