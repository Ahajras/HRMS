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
import com.hrms.migration.dto.ImportSummary;
import com.hrms.payroll.domain.ProvisionRule;
import com.hrms.payroll.repository.ProvisionRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TicketService {
    private static final int DEFAULT_CYCLE_MONTHS = 12;

    private final TicketFareRepository fareRepo;
    private final TicketLedgerRepository ledgerRepo;
    private final EmployeeRepository employeeRepo;
    private final ProvisionRuleRepository provisionRuleRepo;

    public TicketService(TicketFareRepository fareRepo, TicketLedgerRepository ledgerRepo, EmployeeRepository employeeRepo,
                         ProvisionRuleRepository provisionRuleRepo) {
        this.fareRepo = fareRepo;
        this.ledgerRepo = ledgerRepo;
        this.employeeRepo = employeeRepo;
        this.provisionRuleRepo = provisionRuleRepo;
    }

    @Transactional(readOnly = true)
    public List<TicketDtos.FareDto> fares() {
        UUID companyId = TenantContext.requireCompanyId();
        return fareRepo.findByCompanyIdOrderByFromAirportCodeAscToAirportCodeAsc(companyId).stream().map(this::toDto).toList();
    }

    public TicketDtos.FareDto saveFare(TicketDtos.FareDto dto) {
        UUID companyId = TenantContext.requireCompanyId();
        String from = requiredCode(dto.getFromAirportCode(), "fromAirportCode");
        String to = requiredCode(dto.getToAirportCode(), "toAirportCode");
        TicketFare fare = dto.getId() == null
                ? fareRepo.findFirstByCompanyIdAndFromAirportCodeIgnoreCaseAndToAirportCodeIgnoreCaseOrderByEffectiveFromDescCreatedAtDesc(companyId, from, to)
                    .orElseGet(TicketFare::new)
                : fareRepo.findById(dto.getId()).filter(f -> companyId.equals(f.getCompanyId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket fare", dto.getId()));
        fare.setCompanyId(companyId);
        fare.setFromAirportCode(from);
        fare.setToAirportCode(to);
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

    public ImportSummary importFares(MultipartFile file) {
        UUID companyId = TenantContext.requireCompanyId();
        ImportSummary summary = new ImportSummary();
        summary.setCommitted(true);
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("ticket.import.file_required", "Ticket fare import file is required.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv")) {
            throw new BusinessRuleException("ticket.import.csv_required", "Upload a CSV file exported from Excel.");
        }
        List<String> lines;
        try {
            lines = new String(file.getBytes(), StandardCharsets.UTF_8).lines().toList();
        } catch (IOException ex) {
            throw new BusinessRuleException("ticket.import.read_failed", "Unable to read ticket fare import file.");
        }
        if (lines.isEmpty()) {
            throw new BusinessRuleException("ticket.import.empty", "Ticket fare import file is empty.");
        }
        Map<String, Integer> header = headerMap(parseCsvLine(lines.get(0)));
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) continue;
            summary.setSourceHeaderRows(summary.getSourceHeaderRows() + 1);
            List<String> cells = parseCsvLine(line);
            try {
                String from = requiredCode(value(cells, header, "from_airport_code", "from_airport", "from"), "from_airport_code");
                String to = requiredCode(value(cells, header, "to_airport_code", "to_airport", "to"), "to_airport_code");
                BigDecimal amount = money(value(cells, header, "amount", "fare", "ticket_amount"));
                String currency = firstNonBlank(value(cells, header, "currency_code", "currency"), "QAR");
                LocalDate effectiveFrom = date(value(cells, header, "effective_from", "from_date"), "effective_from");
                LocalDate effectiveTo = optionalDate(value(cells, header, "effective_to", "to_date"));
                TicketFare fare = fareRepo
                        .findFirstByCompanyIdAndFromAirportCodeIgnoreCaseAndToAirportCodeIgnoreCaseOrderByEffectiveFromDescCreatedAtDesc(companyId, from, to)
                        .orElseGet(TicketFare::new);
                boolean inserted = fare.getId() == null;
                fare.setCompanyId(companyId);
                fare.setFromAirportCode(from);
                fare.setToAirportCode(to);
                fare.setAmount(amount);
                fare.setCurrencyCode(code(currency));
                fare.setEffectiveFrom(effectiveFrom);
                fare.setEffectiveTo(effectiveTo);
                fare.setStatus(firstNonBlank(value(cells, header, "status"), "ACTIVE").toUpperCase(Locale.ROOT));
                fare.setSource("IMPORT");
                fare.setProvider("CSV");
                fare.setFetchedAt(OffsetDateTime.now());
                fare.setRemarks(value(cells, header, "remarks", "note", "notes"));
                fareRepo.save(fare);
                summary.bump(inserted ? "ticket_fare_inserted" : "ticket_fare_updated");
            } catch (RuntimeException ex) {
                summary.bump("ticket_fare_skipped");
                summary.getWarnings().add("Row " + (i + 1) + ": " + ex.getMessage());
            }
        }
        return summary;
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
        List<TicketLedger> ledgerRows = ledgerRepo.findByCompanyIdAndEmployeeIdAndStatusAndEntryDateLessThanEqual(companyId, employeeId, "ACTIVE", asOf);
        ProvisionRule rule = ticketRule(companyId, null, employee.getPayStatus(), asOf);
        return balanceForEmployee(companyId, employee, asOf, ledgerRows, rule);
    }

    @Transactional(readOnly = true)
    public TicketDtos.AccrualReportDto accrualReport(UUID projectId, String payGroup, LocalDate asOfDate) {
        UUID companyId = TenantContext.requireCompanyId();
        LocalDate asOf = asOfDate == null ? LocalDate.now() : asOfDate;
        LocalDate periodStart = asOf.withDayOfMonth(1);
        String normalizedPayGroup = payGroup == null || payGroup.isBlank() ? "ALL" : payGroup.trim().toUpperCase();
        List<Employee> employees = employeeRepo.findProvisionScope(companyId, periodStart, asOf, projectId, normalizedPayGroup);
        List<UUID> employeeIds = employees.stream().map(Employee::getId).toList();
        Map<UUID, List<TicketLedger>> ledgerByEmployee = employeeIds.isEmpty()
                ? Map.of()
                : ledgerRepo.findByCompanyIdAndEmployeeIdInAndStatusAndEntryDateLessThanEqual(companyId, employeeIds, "ACTIVE", asOf)
                    .stream().collect(java.util.stream.Collectors.groupingBy(TicketLedger::getEmployeeId));

        TicketDtos.AccrualReportDto report = new TicketDtos.AccrualReportDto();
        report.setAsOfDate(asOf);
        report.setProjectId(projectId);
        report.setPayGroup(normalizedPayGroup);
        report.setEmployeeCount(employees.size());
        List<TicketDtos.BalanceDto> rows = new ArrayList<>();
        BigDecimal totalTicket = BigDecimal.ZERO;
        BigDecimal totalAccrued = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalUsed = BigDecimal.ZERO;
        BigDecimal totalBalance = BigDecimal.ZERO;
        int missing = 0;
        for (Employee employee : employees) {
            ProvisionRule rule = ticketRule(companyId, projectId, employee.getPayStatus(), asOf);
            TicketDtos.BalanceDto row = balanceForEmployee(companyId, employee, asOf, ledgerByEmployee.getOrDefault(employee.getId(), List.of()), rule);
            rows.add(row);
            if (!blank(row.getMessage())) missing++;
            totalTicket = totalTicket.add(nz(row.getTicketAmount()));
            totalAccrued = totalAccrued.add(nz(row.getAccruedAmount()));
            totalCredit = totalCredit.add(nz(row.getAdjustmentCredit()));
            totalUsed = totalUsed.add(nz(row.getUsedAmount()));
            totalBalance = totalBalance.add(nz(row.getBalance()));
        }
        report.setRows(rows);
        report.setMissingSetupCount(missing);
        report.setTotalTicketAmount(scale(totalTicket));
        report.setTotalAccruedAmount(scale(totalAccrued));
        report.setTotalAdjustmentCredit(scale(totalCredit));
        report.setTotalUsedAmount(scale(totalUsed));
        report.setTotalBalance(scale(totalBalance));
        return report;
    }

    private TicketDtos.BalanceDto balanceForEmployee(UUID companyId, Employee employee, LocalDate asOf, List<TicketLedger> ledgerRows, ProvisionRule rule) {
        TicketDtos.BalanceDto dto = new TicketDtos.BalanceDto();
        dto.setEmployeeId(employee.getId());
        dto.setEmployeeNumber(employee.getEmployeeNumber());
        dto.setEmployeeName((employee.getFirstName() + " " + employee.getLastName()).trim());
        dto.setHireDate(employee.getHireDate());
        dto.setAsOfDate(asOf);
        dto.setFromAirportCode(employee.getWorkAirportCode());
        dto.setToAirportCode(employee.getHomeAirportCode());
        int cycleMonths = rule == null ? DEFAULT_CYCLE_MONTHS : Math.max(1, rule.getTicketCycleMonths());
        BigDecimal ticketQuantity = rule == null || rule.getTicketQuantity() == null ? BigDecimal.ONE : rule.getTicketQuantity();
        int expiryMonths = rule == null ? 0 : Math.max(0, rule.getTicketExpiryMonths());
        dto.setCycleMonths(cycleMonths);
        dto.setTicketQuantity(ticketQuantity);
        dto.setExpiryMonths(expiryMonths);

        if (rule == null) {
            dto.setMessage("No active ticket provision rule for pay group " + employee.getPayStatus() + ".");
            return dto;
        }
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
                .divide(BigDecimal.valueOf(cycleMonths), 8, RoundingMode.HALF_UP)
                .multiply(ticketQuantity)
                .multiply(months);
        BigDecimal used = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
        for (TicketLedger row : ledgerRows) {
            BigDecimal amount = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
            String type = row.getEntryType() == null ? "" : row.getEntryType().toUpperCase();
            if (type.contains("CREDIT") || "OPENING_ACCRUED".equals(type)) credit = credit.add(amount);
            else used = used.add(amount);
        }
        dto.setAccruedMonths(months);
        dto.setAccruedAmount(scale(accrued));
        dto.setAdjustmentCredit(scale(credit));
        dto.setUsedAmount(scale(used));
        TicketEntitlement entitlement = entitlement(employee.getHireDate(), asOf, cycleMonths, ticketQuantity, expiryMonths);
        BigDecimal usedTickets = ticketCount(used, fare.getAmount());
        BigDecimal creditTickets = ticketCount(credit, fare.getAmount());
        BigDecimal availableTickets = entitlement.accruedTickets().add(creditTickets).subtract(entitlement.expiredTickets()).subtract(usedTickets);
        if (availableTickets.compareTo(BigDecimal.ZERO) < 0) availableTickets = BigDecimal.ZERO;
        dto.setAccruedTicketCount(scale(entitlement.accruedTickets()));
        dto.setExpiredTicketCount(scale(entitlement.expiredTickets()));
        dto.setUsedTicketCount(scale(usedTickets));
        dto.setAvailableTicketCount(scale(availableTickets));
        dto.setNextDueDate(entitlement.nextDueDate());
        dto.setBalance(scale(availableTickets.multiply(fare.getAmount())));
        return dto;
    }

    private TicketEntitlement entitlement(LocalDate hireDate, LocalDate asOf, int cycleMonths, BigDecimal ticketQuantity, int expiryMonths) {
        if (hireDate == null || asOf.isBefore(hireDate)) {
            return new TicketEntitlement(BigDecimal.ZERO, BigDecimal.ZERO, hireDate);
        }
        BigDecimal accruedTickets = BigDecimal.ZERO;
        BigDecimal expiredTickets = BigDecimal.ZERO;
        LocalDate dueDate = hireDate.plusMonths(cycleMonths);
        while (!dueDate.isAfter(asOf)) {
            accruedTickets = accruedTickets.add(ticketQuantity);
            if (expiryMonths > 0 && dueDate.plusMonths(expiryMonths).isBefore(asOf)) {
                expiredTickets = expiredTickets.add(ticketQuantity);
            }
            dueDate = dueDate.plusMonths(cycleMonths);
        }
        return new TicketEntitlement(accruedTickets, expiredTickets, dueDate);
    }

    private BigDecimal ticketCount(BigDecimal amount, BigDecimal ticketAmount) {
        if (amount == null || ticketAmount == null || ticketAmount.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        return amount.divide(ticketAmount, 4, RoundingMode.HALF_UP);
    }

    private record TicketEntitlement(BigDecimal accruedTickets, BigDecimal expiredTickets, LocalDate nextDueDate) {}

    private ProvisionRule ticketRule(UUID companyId, UUID projectId, String payGroup, LocalDate asOf) {
        String group = blank(payGroup) ? "ALL" : payGroup.trim().toUpperCase();
        return provisionRuleRepo.findMatching(companyId, "TICKET", projectId, group, asOf)
                .stream().findFirst().orElse(null);
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

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private Map<String, Integer> headerMap(List<String> headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String key = normalizeHeader(headers.get(i));
            if (!key.isBlank()) map.put(key, i);
        }
        return map;
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    }

    private String value(List<String> cells, Map<String, Integer> header, String... keys) {
        for (String key : keys) {
            Integer index = header.get(normalizeHeader(key));
            if (index != null && index >= 0 && index < cells.size()) return cells.get(index).trim();
        }
        return null;
    }

    private BigDecimal money(String value) {
        if (blank(value)) throw new BusinessRuleException("ticket.import.amount_required", "amount is required.");
        return new BigDecimal(value.replace(",", "").trim());
    }

    private LocalDate date(String value, String field) {
        if (blank(value)) throw new BusinessRuleException("ticket.import.date_required", field + " is required.");
        return LocalDate.parse(value.trim());
    }

    private LocalDate optionalDate(String value) {
        return blank(value) ? null : LocalDate.parse(value.trim());
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

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
