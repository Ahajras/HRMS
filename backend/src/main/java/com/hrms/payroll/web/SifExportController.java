package com.hrms.payroll.web;

import com.hrms.payroll.dto.SifExportDtos.SifExportResult;
import com.hrms.payroll.service.SifExportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Qatar WPS Salary Information File (SIF) generation. Regular staff can
 * only generate once every payroll run for the period is LOCKED; a
 * manager (holding the extra 'payroll.sif.generate_unlocked' permission)
 * can generate a preview at any time. */
@RestController
@RequestMapping("/api/v1/payroll/sif")
public class SifExportController {

    private final SifExportService service;

    public SifExportController(SifExportService service) {
        this.service = service;
    }

    public record GenerateRequest(UUID periodId, List<UUID> projectIds) {
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('payroll.sif.generate')")
    public SifExportResult generate(@RequestBody GenerateRequest request) {
        boolean allowUnlocked = hasUnlockedPreviewAuthority();
        return service.generate(request.periodId(), request.projectIds(), allowUnlocked);
    }

    private boolean hasUnlockedPreviewAuthority() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "payroll.sif.generate_unlocked".equals(a.getAuthority()));
    }
}
