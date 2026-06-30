package com.hrms.timesheet.web;

import com.hrms.timesheet.service.TimesheetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-project lock within a payroll period (FTDD Vol.1 Ch.4). */
@RestController
@RequestMapping("/api/v1/period-locks")
public class ProjectLockController {

    private final TimesheetService service;

    public ProjectLockController(TimesheetService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> statuses(@RequestParam UUID periodId) {
        return service.projectLockStatuses(periodId);
    }

    @PostMapping("/lock")
    public Map<String, Object> lock(@RequestParam UUID periodId, @RequestParam UUID projectId) {
        return service.lockProject(periodId, projectId);
    }

    @PostMapping("/close")
    public Map<String, Object> close(@RequestParam UUID periodId, @RequestParam UUID projectId) {
        return service.closeProject(periodId, projectId);
    }

    @PostMapping("/reopen")
    public Map<String, Object> reopen(@RequestParam UUID periodId, @RequestParam UUID projectId) {
        return service.reopenProject(periodId, projectId);
    }
}
