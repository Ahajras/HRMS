package com.hrms.approval.web;

import com.hrms.approval.dto.ApprovalDecisionDto;
import com.hrms.approval.dto.ApprovalTaskDto;
import com.hrms.approval.service.ApprovalService;
import com.hrms.leave.dto.LeaveRequestDto;
import com.hrms.leave.service.LeaveService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalController {
    private final ApprovalService service;
    private final LeaveService leaveService;

    public ApprovalController(ApprovalService service, LeaveService leaveService) {
        this.service = service;
        this.leaveService = leaveService;
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("hasAuthority('employee.read')")
    public List<ApprovalTaskDto> myTasks() {
        return service.myPendingTasks();
    }

    @PostMapping("/timesheet/approve")
    @PreAuthorize("hasAuthority('employee.read')")
    public boolean approveTimesheet(@RequestParam UUID timesheetId) {
        return service.approveTimesheetStep(timesheetId);
    }

    @PostMapping("/leave/{leaveRequestId}/approve")
    @PreAuthorize("hasAuthority('employee.read')")
    public LeaveRequestDto approveLeave(@PathVariable UUID leaveRequestId,
                                        @RequestBody(required = false) ApprovalDecisionDto decision) {
        return leaveService.approveRequest(leaveRequestId, remarks(decision));
    }

    @PostMapping("/leave/{leaveRequestId}/reject")
    @PreAuthorize("hasAuthority('employee.read')")
    public LeaveRequestDto rejectLeave(@PathVariable UUID leaveRequestId,
                                       @RequestBody ApprovalDecisionDto decision) {
        return leaveService.rejectRequest(leaveRequestId, remarks(decision));
    }

    @PostMapping("/leave/{leaveRequestId}/return")
    @PreAuthorize("hasAuthority('employee.read')")
    public LeaveRequestDto returnLeave(@PathVariable UUID leaveRequestId,
                                       @RequestBody ApprovalDecisionDto decision) {
        return leaveService.returnRequest(leaveRequestId, remarks(decision));
    }

    private String remarks(ApprovalDecisionDto decision) {
        return decision == null ? null : decision.getRemarks();
    }
}
