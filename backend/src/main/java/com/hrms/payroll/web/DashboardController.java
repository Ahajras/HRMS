package com.hrms.payroll.web;

import com.hrms.payroll.dto.DashboardDto;
import com.hrms.payroll.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('payroll.config.read')")
    public DashboardDto summary(@RequestParam(required = false) UUID periodId) {
        return service.summary(periodId);
    }
}
