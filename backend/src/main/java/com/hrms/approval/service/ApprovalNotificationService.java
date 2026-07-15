package com.hrms.approval.service;

import com.hrms.approval.domain.ApprovalInstance;
import com.hrms.approval.domain.ApprovalInstanceStep;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.domain.LeaveRequest;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.security.domain.AppUser;
import com.hrms.security.repository.AppUserRepository;
import com.hrms.timesheet.domain.Timesheet;
import com.hrms.timesheet.repository.TimesheetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;

@Service
public class ApprovalNotificationService {
    private static final Logger log = LoggerFactory.getLogger(ApprovalNotificationService.class);

    private final JavaMailSender mailSender;
    private final Environment environment;
    private final AppUserRepository appUserRepo;
    private final EmployeeRepository employeeRepo;
    private final TimesheetRepository timesheetRepo;
    private final LeaveRequestRepository leaveRequestRepo;
    private final LeaveTypeRepository leaveTypeRepo;

    public ApprovalNotificationService(JavaMailSender mailSender,
                                       Environment environment,
                                       AppUserRepository appUserRepo,
                                       EmployeeRepository employeeRepo,
                                       TimesheetRepository timesheetRepo,
                                       LeaveRequestRepository leaveRequestRepo,
                                       LeaveTypeRepository leaveTypeRepo) {
        this.mailSender = mailSender;
        this.environment = environment;
        this.appUserRepo = appUserRepo;
        this.employeeRepo = employeeRepo;
        this.timesheetRepo = timesheetRepo;
        this.leaveRequestRepo = leaveRequestRepo;
        this.leaveTypeRepo = leaveTypeRepo;
    }

    public void notifyPending(ApprovalInstance instance, ApprovalInstanceStep step) {
        String host = environment.getProperty("spring.mail.host");
        if (!StringUtils.hasText(host) || step.getApproverEmployeeId() == null) {
            return;
        }
        String recipient = approverEmail(instance.getCompanyId(), step.getApproverEmployeeId());
        if (!StringUtils.hasText(recipient)) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject(instance));
            message.setText(body(instance, step));
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Could not send approval notification for instance {}", instance.getId(), ex);
        }
    }

    private String approverEmail(UUID companyId, UUID employeeId) {
        return appUserRepo.findByCompanyIdAndEmployeeIdAndStatus(companyId, employeeId, "ACTIVE").stream()
                .map(AppUser::getEmail)
                .filter(StringUtils::hasText)
                .findFirst()
                .or(() -> employeeRepo.findById(employeeId).map(Employee::getEmail).filter(StringUtils::hasText))
                .orElse(null);
    }

    private String subject(ApprovalInstance instance) {
        if (ApprovalService.TIMESHEET_ENTITY.equals(instance.getEntityType())) {
            return timesheetRepo.findById(instance.getEntityId())
                    .map(ts -> "HRMS approval required: Timesheet " + periodLabel(ts))
                    .orElse("HRMS approval required");
        }
        if (ApprovalService.LEAVE_ENTITY.equals(instance.getEntityType())) {
            return leaveRequestRepo.findById(instance.getEntityId())
                    .map(leave -> "HRMS approval required: Leave " + leave.getStartDate() + " - " + leave.getEndDate())
                    .orElse("HRMS approval required");
        }
        return "HRMS approval required: " + instance.getProcessCode();
    }

    private String body(ApprovalInstance instance, ApprovalInstanceStep step) {
        String employee = employeeRepo.findById(instance.getEmployeeId())
                .map(e -> safe(e.getEmployeeNumber()) + " - " + (safe(e.getFirstName()) + " " + safe(e.getLastName())).trim())
                .orElse(String.valueOf(instance.getEmployeeId()));
        String details = approvalDetails(instance);
        return """
                A new approval task is waiting for you.

                Process: %s
                Step: %s
                Employee: %s
                %s

                Please open HRMS > My Approvals to review and approve.
                """.formatted(instance.getProcessCode(), step.getName(), employee, details);
    }

    private String approvalDetails(ApprovalInstance instance) {
        if (ApprovalService.LEAVE_ENTITY.equals(instance.getEntityType())) {
            return leaveRequestRepo.findById(instance.getEntityId()).map(this::leaveDetails).orElse("Leave request: -");
        }
        return "Period: " + timesheetRepo.findById(instance.getEntityId()).map(this::periodLabel).orElse("-");
    }

    private String leaveDetails(LeaveRequest leave) {
        String type = leaveTypeRepo.findById(leave.getLeaveTypeId())
                .map(t -> safe(t.getCode()) + " - " + safe(t.getName()))
                .orElse("-");
        return "Leave type: " + type + "\nDates: " + leave.getStartDate() + " - " + leave.getEndDate()
                + "\nDays: " + leave.getTotalDays();
    }

    private String periodLabel(Timesheet timesheet) {
        String month = Month.of(timesheet.getPeriodMonth()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        return month + " " + timesheet.getPeriodYear();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
