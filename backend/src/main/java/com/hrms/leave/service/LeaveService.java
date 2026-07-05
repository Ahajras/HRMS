package com.hrms.leave.service;

import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.domain.LeaveAdjustment;
import com.hrms.leave.domain.LeaveRequest;
import com.hrms.leave.domain.LeaveType;
import com.hrms.leave.dto.LeaveAdjustmentDto;
import com.hrms.leave.dto.LeaveBalanceDto;
import com.hrms.leave.dto.LeaveRequestDto;
import com.hrms.leave.dto.LeaveTypeDto;
import com.hrms.leave.repository.LeaveAdjustmentRepository;
import com.hrms.leave.repository.LeaveRequestRepository;
import com.hrms.leave.repository.LeaveTypeRepository;
import com.hrms.rule.domain.Rule;
import com.hrms.rule.repository.CompanyRulePackageRepository;
import com.hrms.rule.repository.RulePackageRepository;
import com.hrms.rule.repository.RuleRepository;
import com.hrms.timesheet.domain.TimeType;
import com.hrms.timesheet.repository.TimeTypeRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class LeaveService {
    private static final Set<String> PENDING = Set.of("DRAFT", "SUBMITTED");
    private final LeaveTypeRepository typeRepo;
    private final LeaveRequestRepository requestRepo;
    private final LeaveAdjustmentRepository adjustmentRepo;
    private final EmployeeRepository employeeRepo;
    private final TimeTypeRepository timeTypeRepo;
    private final CompanyRulePackageRepository companyRulePackageRepo;
    private final RulePackageRepository rulePackageRepo;
    private final RuleRepository ruleRepo;

    public LeaveService(LeaveTypeRepository typeRepo, LeaveRequestRepository requestRepo,
                        LeaveAdjustmentRepository adjustmentRepo, EmployeeRepository employeeRepo,
                        TimeTypeRepository timeTypeRepo, CompanyRulePackageRepository companyRulePackageRepo,
                        RulePackageRepository rulePackageRepo, RuleRepository ruleRepo) {
        this.typeRepo = typeRepo;
        this.requestRepo = requestRepo;
        this.adjustmentRepo = adjustmentRepo;
        this.employeeRepo = employeeRepo;
        this.timeTypeRepo = timeTypeRepo;
        this.companyRulePackageRepo = companyRulePackageRepo;
        this.rulePackageRepo = rulePackageRepo;
        this.ruleRepo = ruleRepo;
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeDto> listTypes() {
        UUID companyId = TenantContext.requireCompanyId();
        return typeRepo.findByCompanyIdOrderByCode(companyId).stream().map(this::toDto).toList();
    }

    public LeaveTypeDto saveType(LeaveTypeDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        LeaveType type = dto.getId() != null ? getType(dto.getId()) : new LeaveType();
        type.setCompanyId(companyId);
        type.setCode(dto.getCode() != null ? dto.getCode().trim().toUpperCase() : null);
        type.setName(dto.getName());
        type.setTimeTypeId(dto.getTimeTypeId());
        type.setDeductsBalance(dto.isDeductsBalance());
        type.setPaid(dto.isPaid());
        type.setRequiresTicketDefault(dto.isRequiresTicketDefault());
        type.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        return toDto(typeRepo.save(type));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listRequests(UUID employeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        return (employeeId != null
                ? requestRepo.findByCompanyIdAndEmployeeIdOrderByStartDateDesc(companyId, employeeId)
                : requestRepo.findByCompanyIdOrderByStartDateDesc(companyId))
                .stream().map(this::toDto).toList();
    }

    public LeaveRequestDto saveRequest(LeaveRequestDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        requireEmployee(companyId, dto.getEmployeeId());
        LeaveType type = getType(dto.getLeaveTypeId());
        LeaveRequest row = dto.getId() != null ? getRequest(dto.getId()) : new LeaveRequest();
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessRuleException("leave.date.order", "Leave end date must be after start date.");
        }
        row.setCompanyId(companyId);
        row.setEmployeeId(dto.getEmployeeId());
        row.setLeaveTypeId(dto.getLeaveTypeId());
        row.setStartDate(dto.getStartDate());
        row.setEndDate(dto.getEndDate());
        row.setReturnDate(dto.getReturnDate());
        row.setTotalDays(dto.getTotalDays() != null && dto.getTotalDays().compareTo(BigDecimal.ZERO) > 0
                ? dto.getTotalDays() : daysInclusive(dto.getStartDate(), dto.getEndDate()));
        row.setReason(dto.getReason());
        row.setStatus(dto.getStatus() != null ? dto.getStatus() : "DRAFT");
        row.setRequiresTicket(dto.isRequiresTicket() || type.isRequiresTicketDefault());
        row.setTicketFrom(dto.getTicketFrom());
        row.setTicketTo(dto.getTicketTo());
        row.setTravelDate(dto.getTravelDate());
        row.setReturnTravelDate(dto.getReturnTravelDate());
        row.setDestination(dto.getDestination());
        row.setTravelRemarks(dto.getTravelRemarks());
        row.setContactPhone(dto.getContactPhone());
        row.setContactEmail(dto.getContactEmail());
        row.setAddressDuringLeave(dto.getAddressDuringLeave());
        row.setEmergencyContactName(dto.getEmergencyContactName());
        row.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        if ("APPROVED".equalsIgnoreCase(row.getStatus()) && row.getHrApprovedAt() == null) {
            row.setHrApprovedAt(Instant.now());
            row.setHrApprovedBy(currentUsername());
        }
        return toDto(requestRepo.save(row));
    }

    public LeaveRequestDto setRequestStatus(UUID id, String status) {
        LeaveRequest row = getRequest(id);
        row.setStatus(status != null ? status.toUpperCase() : "DRAFT");
        if ("APPROVED".equals(row.getStatus())) {
            row.setHrApprovedAt(Instant.now());
            row.setHrApprovedBy(currentUsername());
        }
        return toDto(requestRepo.save(row));
    }

    @Transactional(readOnly = true)
    public List<LeaveAdjustmentDto> listAdjustments(UUID employeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        return adjustmentRepo.findByCompanyIdAndEmployeeIdOrderByEffectiveDateDesc(companyId, employeeId)
                .stream().map(this::toDto).toList();
    }

    public LeaveAdjustmentDto saveAdjustment(LeaveAdjustmentDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        requireEmployee(companyId, dto.getEmployeeId());
        getType(dto.getLeaveTypeId());
        LeaveAdjustment row = dto.getId() != null
                ? adjustmentRepo.findById(dto.getId()).orElseThrow(() -> new ResourceNotFoundException("Leave adjustment not found: " + dto.getId()))
                : new LeaveAdjustment();
        row.setCompanyId(companyId);
        row.setEmployeeId(dto.getEmployeeId());
        row.setLeaveTypeId(dto.getLeaveTypeId());
        row.setAdjustmentType(dto.getAdjustmentType() != null ? dto.getAdjustmentType() : "OPENING_USED");
        row.setDays(dto.getDays() != null ? dto.getDays() : BigDecimal.ZERO);
        row.setEffectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDate.now());
        row.setReason(dto.getReason());
        return toDto(adjustmentRepo.save(row));
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceDto> balances(UUID employeeId, LocalDate asOf) {
        UUID companyId = TenantContext.requireCompanyId();
        Employee employee = requireEmployee(companyId, employeeId);
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        return typeRepo.findByCompanyIdOrderByCode(companyId).stream()
                .filter(LeaveType::isDeductsBalance)
                .map(type -> balance(companyId, employee, type, date))
                .toList();
    }

    private LeaveBalanceDto balance(UUID companyId, Employee employee, LeaveType type, LocalDate asOf) {
        LeaveBalanceDto dto = new LeaveBalanceDto();
        dto.setEmployeeId(employee.getId());
        dto.setLeaveTypeId(type.getId());
        dto.setLeaveTypeCode(type.getCode());
        dto.setLeaveTypeName(type.getName());
        dto.setAsOfDate(asOf);
        BigDecimal annualRate = annualLeaveRate(companyId, employee, asOf);
        BigDecimal entitlement = "ANNUAL".equalsIgnoreCase(type.getCode())
                ? proratedEntitlement(companyId, employee.getHireDate(), asOf)
                : BigDecimal.ZERO;
        BigDecimal adjustments = adjustments(companyId, employee.getId(), type.getId(), asOf);
        BigDecimal approved = requestDays(companyId, employee.getId(), type.getId(), Set.of("APPROVED"), asOf);
        BigDecimal pending = requestDays(companyId, employee.getId(), type.getId(), PENDING, asOf);
        dto.setAnnualRate(annualRate);
        dto.setEntitledToDate(entitlement);
        dto.setAdjustments(adjustments);
        dto.setUsedApproved(approved);
        dto.setPending(pending);
        dto.setBalance(entitlement.add(adjustments).subtract(approved));
        return dto;
    }

    private BigDecimal adjustments(UUID companyId, UUID employeeId, UUID leaveTypeId, LocalDate asOf) {
        BigDecimal total = BigDecimal.ZERO;
        for (LeaveAdjustment a : adjustmentRepo.findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndEffectiveDateLessThanEqual(companyId, employeeId, leaveTypeId, asOf)) {
            BigDecimal days = a.getDays() != null ? a.getDays() : BigDecimal.ZERO;
            if ("MANUAL_CREDIT".equalsIgnoreCase(a.getAdjustmentType())) total = total.add(days);
            else total = total.subtract(days);
        }
        return total;
    }

    private BigDecimal requestDays(UUID companyId, UUID employeeId, UUID leaveTypeId, Set<String> statuses, LocalDate asOf) {
        return requestRepo.findByCompanyIdAndEmployeeIdAndLeaveTypeIdAndStatusInAndStartDateLessThanEqual(
                        companyId, employeeId, leaveTypeId, statuses, asOf).stream()
                .map(r -> r.getTotalDays() != null ? r.getTotalDays() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal proratedEntitlement(UUID companyId, LocalDate hireDate, LocalDate asOf) {
        if (hireDate == null || asOf.isBefore(hireDate)) return BigDecimal.ZERO;
        LocalDate fiveYear = hireDate.plusYears(5);
        BigDecimal under = rule(companyId, "ANNUAL_LEAVE_DAYS_UNDER_5Y", new BigDecimal("21"));
        BigDecimal plus = rule(companyId, "ANNUAL_LEAVE_DAYS_5Y_PLUS", new BigDecimal("28"));
        BigDecimal total = BigDecimal.ZERO;
        if (hireDate.isBefore(fiveYear)) {
            LocalDate end = asOf.isBefore(fiveYear) ? asOf : fiveYear.minusDays(1);
            total = total.add(prorate(under, hireDate, end));
        }
        if (!asOf.isBefore(fiveYear)) total = total.add(prorate(plus, fiveYear, asOf));
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal annualLeaveRate(UUID companyId, Employee employee, LocalDate asOf) {
        if (employee.getHireDate() != null && !asOf.isBefore(employee.getHireDate().plusYears(5))) {
            return rule(companyId, "ANNUAL_LEAVE_DAYS_5Y_PLUS", new BigDecimal("28"));
        }
        return rule(companyId, "ANNUAL_LEAVE_DAYS_UNDER_5Y", new BigDecimal("21"));
    }

    private BigDecimal prorate(BigDecimal annual, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) return BigDecimal.ZERO;
        long days = ChronoUnit.DAYS.between(start, end.plusDays(1));
        return annual.multiply(BigDecimal.valueOf(days)).divide(new BigDecimal("365"), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal rule(UUID companyId, String code, BigDecimal fallback) {
        String packageCode = companyRulePackageRepo.findById(companyId).map(c -> c.getPackageCode()).orElse("QATAR");
        return rulePackageRepo.findByCompanyIdIsNullAndCode(packageCode)
                .flatMap(pkg -> ruleRepo.findByPackageIdAndCodeAndStatus(pkg.getId(), code, "ACTIVE").stream().findFirst())
                .map(Rule::getValueNumber).orElse(fallback);
    }

    private Employee requireEmployee(UUID companyId, UUID employeeId) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        if (!companyId.equals(employee.getCompanyId())) throw new ResourceNotFoundException("Employee not found: " + employeeId);
        return employee;
    }

    private LeaveType getType(UUID id) {
        LeaveType type = typeRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave type not found: " + id));
        if (!TenantContext.requireCompanyId().equals(type.getCompanyId())) throw new ResourceNotFoundException("Leave type not found: " + id);
        return type;
    }

    private LeaveRequest getRequest(UUID id) {
        LeaveRequest row = requestRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + id));
        if (!TenantContext.requireCompanyId().equals(row.getCompanyId())) throw new ResourceNotFoundException("Leave request not found: " + id);
        return row;
    }

    private LeaveTypeDto toDto(LeaveType t) {
        LeaveTypeDto dto = new LeaveTypeDto();
        dto.setId(t.getId());
        dto.setCode(t.getCode());
        dto.setName(t.getName());
        dto.setTimeTypeId(t.getTimeTypeId());
        timeTypeRepo.findById(t.getTimeTypeId()).ifPresent(tt -> dto.setTimeTypeCode(tt.getCode()));
        dto.setDeductsBalance(t.isDeductsBalance());
        dto.setPaid(t.isPaid());
        dto.setRequiresTicketDefault(t.isRequiresTicketDefault());
        dto.setStatus(t.getStatus());
        return dto;
    }

    private LeaveRequestDto toDto(LeaveRequest r) {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setId(r.getId());
        dto.setEmployeeId(r.getEmployeeId());
        employeeRepo.findById(r.getEmployeeId()).ifPresent(e -> {
            dto.setEmployeeNumber(e.getEmployeeNumber());
            dto.setEmployeeName((e.getFirstName() + " " + e.getLastName()).trim());
        });
        dto.setLeaveTypeId(r.getLeaveTypeId());
        typeRepo.findById(r.getLeaveTypeId()).ifPresent(t -> { dto.setLeaveTypeCode(t.getCode()); dto.setLeaveTypeName(t.getName()); });
        dto.setStartDate(r.getStartDate());
        dto.setEndDate(r.getEndDate());
        dto.setReturnDate(r.getReturnDate());
        dto.setTotalDays(r.getTotalDays());
        dto.setReason(r.getReason());
        dto.setStatus(r.getStatus());
        dto.setRequiresTicket(r.isRequiresTicket());
        dto.setTicketFrom(r.getTicketFrom());
        dto.setTicketTo(r.getTicketTo());
        dto.setTravelDate(r.getTravelDate());
        dto.setReturnTravelDate(r.getReturnTravelDate());
        dto.setDestination(r.getDestination());
        dto.setTravelRemarks(r.getTravelRemarks());
        dto.setContactPhone(r.getContactPhone());
        dto.setContactEmail(r.getContactEmail());
        dto.setAddressDuringLeave(r.getAddressDuringLeave());
        dto.setEmergencyContactName(r.getEmergencyContactName());
        dto.setEmergencyContactPhone(r.getEmergencyContactPhone());
        return dto;
    }

    private LeaveAdjustmentDto toDto(LeaveAdjustment a) {
        LeaveAdjustmentDto dto = new LeaveAdjustmentDto();
        dto.setId(a.getId());
        dto.setEmployeeId(a.getEmployeeId());
        dto.setLeaveTypeId(a.getLeaveTypeId());
        typeRepo.findById(a.getLeaveTypeId()).ifPresent(t -> dto.setLeaveTypeCode(t.getCode()));
        dto.setAdjustmentType(a.getAdjustmentType());
        dto.setDays(a.getDays());
        dto.setEffectiveDate(a.getEffectiveDate());
        dto.setReason(a.getReason());
        return dto;
    }

    private static BigDecimal daysInclusive(LocalDate start, LocalDate end) {
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(start, end) + 1);
    }

    private static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() ? auth.getName() : "system";
    }
}
