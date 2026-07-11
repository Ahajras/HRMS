package com.hrms.benefits.service;

import com.hrms.benefits.domain.TicketFare;
import com.hrms.benefits.domain.TicketLedger;
import com.hrms.benefits.dto.TicketDtos;
import com.hrms.benefits.repository.TicketFareRepository;
import com.hrms.benefits.repository.TicketLedgerRepository;
import com.hrms.common.exception.BusinessRuleException;
import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.common.tenant.TenantContext;
import com.hrms.employee.domain.Employee;
import com.hrms.employee.repository.EmployeeRepository;
import com.hrms.leave.domain.LeaveRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TicketService {
    private static final int DEFAULT_CYCLE_MONTHS = 12;

    private final TicketFareRepository fareRepo;
    private final TicketLedgerRepository ledgerRepo;
    private final EmployeeRepository employeeRepo;
    private final TicketFareProviderService providerService;

    public TicketService(TicketFareRepository fareRepo, TicketLedgerRepository ledgerRepo, EmployeeRepository employeeRepo,
                         TicketFareProviderService providerService) {
        this.fareRepo = fareRepo;
        this.ledgerRepo = ledgerRepo;
        this.employeeRepo = employeeRepo;
        this.providerService = providerService;
    }

    @Transactional(readOnly = true)
    public List<TicketDtos.FareDto> fares() {
        UUID companyId = TenantContext.requireCompanyId();
        return fareRepo.findByCompanyIdOrderByFromAirportCodeAscToAirportCodeAsc(companyId).stream().map(this::toDto).toList();
    }

    public TicketDtos.FareDto saveFare(TicketDtos.FareDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        TicketFare fare = dto.getId() == null ? new TicketFare()
                : fareRepo.findById(dto.getId()).filter(f -> companyId.equals(f.getCompanyId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket fare", dto.getId()));
        fare.setCompanyId(companyId);
        fare.setFromAirportCode(requiredCode(dto.getFromAirportCode(), "fromAirportCode"));
        fare.setToAirportCode(requiredCode(dto.getToAirportCode(), "toAirportCode"));
        fare.setAmount(dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount());
        fare.setCurrencyCode(dto.getCurrencyCode());
        fare.setEffectiveFrom(dto.getEffectiveFrom() == null ? LocalDate.now() : dto.getEffectiveFrom());
        fare.setEffectiveTo(dto.getEffectiveTo());
        fare.setStatus(dto.getStatus() == null ? "ACTIVE" : dto.getStatus().toUpperCase());
        fare.setSource(dto.getSource() == null ? "MANUAL" : dto.getSource().trim().toUpperCase());
        fare.setProvider(code(dto.getProvider()));
        fare.setProviderOfferId(dto.getProviderOfferId());
        fare.setFetchedAt(dto.getFetchedAt());
        fare.setRemarks(dto.getRemarks());
        return toDto(fareRepo.save(fare));
    }

    public TicketDtos.FareDto lookupFare(TicketDtos.FareLookupRequest request) {
        UUID companyId = TenantContext.requireCompanyId();
        String from = requiredCode(request.getFromAirportCode(), "fromAirportCode");
        String to = requiredCode(request.getToAirportCode(), "toAirportCode");
        request.setFromAirportCode(from);
        request.setToAirportCode(to);
        TicketFareProviderService.ProviderFare providerFare = providerService.lookup(request);
        TicketFare fare = new TicketFare();
        fare.setCompanyId(companyId);
        fare.setFromAirportCode(from);
        fare.setToAirportCode(to);
        fare.setAmount(providerFare.amount());
        fare.setCurrencyCode(providerFare.currencyCode());
        fare.setEffectiveFrom(request.getEffectiveFrom() == null ? LocalDate.now() : request.getEffectiveFrom());
        fare.setStatus("ACTIVE");
        fare.setSource("API");
        fare.setProvider(providerFare.provider());
        fare.setProviderOfferId(providerFare.providerOfferId());
        fare.setFetchedAt(OffsetDateTime.now());
        fare.setRemarks("Fetched from " + providerFare.provider() + " for departure " +
                (request.getDepartureDate() == null ? LocalDate.now().plusMonths(1) : request.getDepartureDate()) + ".");
        return request.isSave() ? toDto(fareRepo.save(fare)) : toDto(fare);
    }

    @Transactional(readOnly = true)
    public List<TicketDtos.LedgerDto> ledger(UUID employeeId) {
        UUID companyId = TenantContext.requireCompanyId();
        requireEmployee(companyId, employeeId);
        return ledgerRepo.findByCompanyIdAndEmployeeIdOrderByEntryDateDesc(companyId, employeeId).stream().map(this::toDto).toList();
    }

    public TicketDtos.LedgerDto saveLedger(TicketDtos.LedgerDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        requireEmployee(companyId, dto.getEmployeeId());
        TicketLedger row = dto.getId() == null ? new TicketLedger()
                : ledgerRepo.findById(dto.getId()).filter(l -> companyId.equals(l.getCompanyId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket ledger", dto.getId()));
        row.setCompanyId(companyId);
        row.setEmployeeId(dto.getEmployeeId());
        row.setLeaveRequestId(dto.getLeaveRequestId());
        row.setEntryType(dto.getEntryType() == null ? "OPENING_USED" : dto.getEntryType().trim().toUpperCase());
        row.setEntryDate(dto.getEntryDate() == null ? LocalDate.now() : dto.getEntryDate());
        row.setAmount(dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount());
        row.setFromAirportCode(code(dto.getFromAirportCode()));
        row.setToAirportCode(code(dto.getToAirportCode()));
        row.setStatus(dto.getStatus() == null ? "ACTIVE" : dto.getStatus().toUpperCase());
        row.setRemarks(dto.getRemarks());
        return toDto(ledgerRepo.save(row));
    }

    @Transactional(readOnly = true)
    public TicketDtos.BalanceDto balance(UUID employeeId, LocalDate asOfDate) {
        UUID companyId = TenantContext.requireCompanyId();
        Employee employee = requireEmployee(companyId, employeeId);
        LocalDate asOf = asOfDate == null ? LocalDate.now() : asOfDate;
        TicketDtos.BalanceDto dto = new TicketDtos.BalanceDto();
        dto.setEmployeeId(employee.getId());
        dto.setEmployeeNumber(employee.getEmployeeNumber());
        dto.setEmployeeName((employee.getFirstName() + " " + employee.getLastName()).trim());
        dto.setHireDate(employee.getHireDate());
        dto.setAsOfDate(asOf);
        dto.setFromAirportCode(employee.getWorkAirportCode());
        dto.setToAirportCode(employee.getHomeAirportCode());
        dto.setCycleMonths(DEFAULT_CYCLE_MONTHS);

        if (blank(employee.getWorkAirportCode()) || blank(employee.getHomeAirportCode())) {
            dto.setMessage("Employee work/home airport is not configured.");
            return dto;
        }
        TicketFare fare = fareRepo.findBest(companyId, employee.getWorkAirportCode(), employee.getHomeAirportCode(), asOf).orElse(null);
        if (fare == null) {
            dto.setMessage("No active ticket fare found for route " + employee.getWorkAirportCode() + " -> " + employee.getHomeAirportCode() + ".");
            return dto;
        }
        dto.setTicketAmount(scale(fare.getAmount()));
        BigDecimal months = accruedMonths(employee.getHireDate(), asOf);
        BigDecimal accrued = fare.getAmount()
                .divide(BigDecimal.valueOf(DEFAULT_CYCLE_MONTHS), 8, RoundingMode.HALF_UP)
                .multiply(months);
        BigDecimal used = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (TicketLedger row : ledgerRepo.findByCompanyIdAndEmployeeIdAndStatusAndEntryDateLessThanEqual(companyId, employeeId, "ACTIVE", asOf)) {
            BigDecimal amount = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
            String type = row.getEntryType() == null ? "" : row.getEntryType().toUpperCase();
            if (type.contains("CREDIT") || "OPENING_ACCRUED".equals(type)) credit = credit.add(amount);
            else used = used.add(amount);
        }
        dto.setAccruedMonths(months);
        dto.setAccruedAmount(scale(accrued));
        dto.setAdjustmentCredit(scale(credit));
        dto.setUsedAmount(scale(used));
        dto.setBalance(scale(accrued.add(credit).subtract(used)));
        return dto;
    }

    @Transactional(readOnly = true)
    public BigDecimal ticketAmountForEmployee(Employee employee, LocalDate asOf) {
        if (employee == null || blank(employee.getWorkAirportCode()) || blank(employee.getHomeAirportCode())) {
            return BigDecimal.ZERO;
        }
        LocalDate effectiveDate = asOf == null ? LocalDate.now() : asOf;
        return fareRepo.findBest(employee.getCompanyId(), employee.getWorkAirportCode(), employee.getHomeAirportCode(), effectiveDate)
                .map(TicketFare::getAmount)
                .orElse(BigDecimal.ZERO);
    }

    public void syncLeaveTicket(LeaveRequest request) {
        UUID companyId = request.getCompanyId();
        if (!request.isRequiresTicket()) {
            voidLeaveTicket(companyId, request.getId());
            return;
        }
        if (!"APPROVED".equalsIgnoreCase(request.getStatus())) {
            voidLeaveTicket(companyId, request.getId());
            return;
        }
        if (ledgerRepo.findByCompanyIdAndLeaveRequestIdAndStatus(companyId, request.getId(), "ACTIVE").isPresent()) {
            return;
        }
        Employee employee = requireEmployee(companyId, request.getEmployeeId());
        LocalDate asOf = request.getTravelDate() != null ? request.getTravelDate() : request.getStartDate();
        String from = firstNonBlank(request.getTicketFrom(), employee.getWorkAirportCode());
        String to = firstNonBlank(request.getTicketTo(), employee.getHomeAirportCode(), request.getDestination());
        if (blank(from) || blank(to)) {
            throw new BusinessRuleException("ticket.route.required", "Ticket route is required before approving this leave.");
        }
        TicketFare fare = fareRepo.findBest(companyId, from, to, asOf)
                .orElseThrow(() -> new BusinessRuleException("ticket.fare.required", "No active ticket fare found for route " + from + " -> " + to + "."));
        TicketLedger row = new TicketLedger();
        row.setCompanyId(companyId);
        row.setEmployeeId(request.getEmployeeId());
        row.setLeaveRequestId(request.getId());
        row.setEntryType("USED");
        row.setEntryDate(asOf);
        row.setAmount(fare.getAmount());
        row.setFromAirportCode(fare.getFromAirportCode());
        row.setToAirportCode(fare.getToAirportCode());
        row.setStatus("ACTIVE");
        row.setRemarks("Generated from approved leave request.");
        ledgerRepo.save(row);
    }

    public void voidLeaveTicket(UUID companyId, UUID leaveRequestId) {
        ledgerRepo.findByCompanyIdAndLeaveRequestIdAndStatus(companyId, leaveRequestId, "ACTIVE").ifPresent(row -> {
            row.setStatus("VOID");
            row.setRemarks((row.getRemarks() == null ? "" : row.getRemarks() + " ") + "Voided because leave is not approved.");
            ledgerRepo.save(row);
        });
    }

    private Employee requireEmployee(UUID companyId, UUID employeeId) {
        Employee employee = employeeRepo.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        if (!companyId.equals(employee.getCompanyId())) throw new ResourceNotFoundException("Employee not found: " + employeeId);
        return employee;
    }

    private BigDecimal accruedMonths(LocalDate hireDate, LocalDate asOf) {
        if (hireDate == null || asOf.isBefore(hireDate)) return BigDecimal.ZERO;
        long months = ChronoUnit.MONTHS.between(hireDate.withDayOfMonth(1), asOf.withDayOfMonth(1)) + 1;
        return BigDecimal.valueOf(Math.max(0, months)).setScale(2, RoundingMode.HALF_UP);
    }

    private TicketDtos.FareDto toDto(TicketFare fare) {
        TicketDtos.FareDto dto = new TicketDtos.FareDto();
        dto.setId(fare.getId());
        dto.setFromAirportCode(fare.getFromAirportCode());
        dto.setToAirportCode(fare.getToAirportCode());
        dto.setAmount(fare.getAmount());
        dto.setCurrencyCode(fare.getCurrencyCode());
        dto.setEffectiveFrom(fare.getEffectiveFrom());
        dto.setEffectiveTo(fare.getEffectiveTo());
        dto.setStatus(fare.getStatus());
        dto.setSource(fare.getSource());
        dto.setProvider(fare.getProvider());
        dto.setProviderOfferId(fare.getProviderOfferId());
        dto.setFetchedAt(fare.getFetchedAt());
        dto.setRemarks(fare.getRemarks());
        return dto;
    }

    private TicketDtos.LedgerDto toDto(TicketLedger row) {
        TicketDtos.LedgerDto dto = new TicketDtos.LedgerDto();
        dto.setId(row.getId());
        dto.setEmployeeId(row.getEmployeeId());
        dto.setLeaveRequestId(row.getLeaveRequestId());
        dto.setEntryType(row.getEntryType());
        dto.setEntryDate(row.getEntryDate());
        dto.setAmount(row.getAmount());
        dto.setFromAirportCode(row.getFromAirportCode());
        dto.setToAirportCode(row.getToAirportCode());
        dto.setStatus(row.getStatus());
        dto.setRemarks(row.getRemarks());
        return dto;
    }

    private String requiredCode(String value, String field) {
        if (blank(value)) throw new BusinessRuleException("ticket.code.required", field + " is required.");
        return code(value);
    }

    private String code(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (!blank(value)) return code(value);
        return null;
    }

    private BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
