package com.hrms.timesheet.web;

import com.hrms.common.tenant.TenantContext;
import com.hrms.timesheet.dto.BulkStatusJobDto;
import com.hrms.timesheet.service.BulkStatusJobService;
import com.hrms.timesheet.service.TimesheetService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-project lock within a payroll period (FTDD Vol.1 Ch.4). */
@RestController
@RequestMapping("/api/v1/period-locks")
public class ProjectLockController {

    private final TimesheetService service;
    private final BulkStatusJobService bulkStatusJobService;

    public ProjectLockController(TimesheetService service, BulkStatusJobService bulkStatusJobService) {
        this.service = service;
        this.bulkStatusJobService = bulkStatusJobService;
    }

    @GetMapping
    public List<Map<String, Object>> statuses(@RequestParam UUID periodId,
                                              @RequestParam(required = false) String payGroup) {
        return service.projectLockStatuses(periodId, payGroup);
    }

    @PostMapping("/lock")
    public Map<String, Object> lock(@RequestParam UUID periodId, @RequestParam UUID projectId,
                                    @RequestParam(required = false) String payGroup) {
        return service.lockProject(periodId, projectId, payGroup);
    }

    @PostMapping("/lock-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BulkStatusJobDto startLock(@RequestParam UUID periodId, @RequestParam UUID projectId,
                                      @RequestParam(required = false) String payGroup) {
        UUID companyId = TenantContext.requireCompanyId();
        return bulkStatusJobService.start("Locking timesheets", companyId,
                progress -> new HashMap<>(service.lockProject(periodId, projectId, payGroup, progress)));
    }

    @GetMapping("/lock-jobs/{id}")
    public BulkStatusJobDto getLockJob(@PathVariable UUID id) {
        return bulkStatusJobService.get(id);
    }

    @PostMapping("/close")
    public Map<String, Object> close(@RequestParam UUID periodId, @RequestParam UUID projectId,
                                     @RequestParam(required = false) String payGroup) {
        return service.closeProject(periodId, projectId, payGroup);
    }

    @PostMapping("/reopen")
    public Map<String, Object> reopen(@RequestParam UUID periodId, @RequestParam UUID projectId,
                                      @RequestParam(required = false) String payGroup) {
        return service.reopenProject(periodId, projectId, payGroup);
    }
}
