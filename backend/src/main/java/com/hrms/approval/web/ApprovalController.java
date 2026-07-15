package com.hrms.approval.web;

import com.hrms.approval.dto.ApprovalTaskDto;
import com.hrms.approval.service.ApprovalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {
    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("hasAuthority('employee.read')")
    public List<ApprovalTaskDto> myTasks() {
        return service.myPendingTasks();
    }

    @PostMapping("/timesheet/approve")
    @PreAuthorize("hasAuthority('employee.write')")
    public boolean approveTimesheet(@RequestParam UUID timesheetId) {
        return service.approveTimesheetStep(timesheetId);
    }
}
