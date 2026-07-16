package com.hrms.security.web;

import com.hrms.common.tenant.EmployeeContext;
import com.hrms.leave.dto.LeaveBalanceDto;
import com.hrms.leave.dto.LeaveRequestDto;
import com.hrms.leave.dto.LeaveTypeDto;
import com.hrms.leave.service.LeaveService;
import com.hrms.payroll.dto.PayrollResultDto;
import com.hrms.payroll.service.PayrollRunService;
import com.hrms.timesheet.dto.TimesheetDto;
import com.hrms.timesheet.service.TimesheetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Employee Self-Service — "my own data" endpoints. Every method here reads
 * the employee id from {@link EmployeeContext} (populated from the JWT's
 * own claim at login), NEVER from a request parameter — so there is no
 * way, by design, for one employee's session to view or submit data for
 * another employee, regardless of what a client sends.
 */
@RestController
@RequestMapping("/api/v1/me")
public class SelfServiceController {

    private final PayrollRunService payrollRunService;
    private final TimesheetService timesheetService;
    private final LeaveService leaveService;

    public SelfServiceController(PayrollRunService payrollRunService, TimesheetService timesheetService,
                                 LeaveService leaveService) {
        this.payrollRunService = payrollRunService;
        this.timesheetService = timesheetService;
        this.leaveService = leaveService;
    }

    @GetMapping("/payslips")
    @PreAuthorize("hasAuthority('self.payslip.read')")
    public List<PayrollResultDto> myPayslips() {
        return payrollRunService.findMyPayslips(EmployeeContext.requireEmployeeId());
    }

    @GetMapping("/payslips/{resultId}")
    @PreAuthorize("hasAuthority('self.payslip.read')")
    public PayrollResultDto myPayslipDetail(@PathVariable UUID resultId) {
        return payrollRunService.findMyPayslipDetail(EmployeeContext.requireEmployeeId(), resultId)
                .orElseThrow(() -> new com.hrms.common.exception.ResourceNotFoundException("Payslip not found: " + resultId));
    }

    @GetMapping("/timesheet")
    @PreAuthorize("hasAuthority('self.timesheet.read')")
    public TimesheetDto myTimesheet(@RequestParam int year, @RequestParam int month) {
        TimesheetDto dto = timesheetService.findMyTimesheet(EmployeeContext.requireEmployeeId(), year, month);
        if (dto == null) {
            throw new com.hrms.common.exception.ResourceNotFoundException(
                    "No timesheet found for " + year + "-" + month);
        }
        return dto;
    }

    @GetMapping("/leave-types")
    @PreAuthorize("hasAuthority('self.leave.write')")
    public List<LeaveTypeDto> leaveTypes() {
        return leaveService.listTypes();
    }

    @GetMapping("/leave-requests")
    @PreAuthorize("hasAuthority('self.leave.read')")
    public List<LeaveRequestDto> myLeaveRequests() {
        return leaveService.listRequests(EmployeeContext.requireEmployeeId());
    }

    @GetMapping("/leave-balance")
    @PreAuthorize("hasAuthority('self.leave.read')")
    public List<LeaveBalanceDto> myLeaveBalance() {
        return leaveService.balances(EmployeeContext.requireEmployeeId(), LocalDate.now());
    }

    /** Submit a new leave/sick request. The employee id, request id, and
     * status are always forced server-side — never taken from the
     * request body — so this can only ever create a NEW, SUBMITTED
     * request for the caller's own employee record. Approval remains a
     * manager action through the normal leave-management screen. */
    @PostMapping("/leave-requests")
    @PreAuthorize("hasAuthority('self.leave.write')")
    public LeaveRequestDto submitLeaveRequest(@RequestBody LeaveRequestDto dto) {
        dto.setId(null);
        dto.setEmployeeId(EmployeeContext.requireEmployeeId());
        dto.setStatus("SUBMITTED");
        return leaveService.saveRequest(dto);
    }

    @PutMapping("/leave-requests/{id}")
    @PreAuthorize("hasAuthority('self.leave.write')")
    public LeaveRequestDto resubmitLeaveRequest(@PathVariable UUID id, @RequestBody LeaveRequestDto dto) {
        return leaveService.resubmitOwnRequest(EmployeeContext.requireEmployeeId(), id, dto);
    }
}
