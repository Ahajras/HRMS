package com.hrms.benefits.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TicketDtos {
    private TicketDtos() {}

    public static class FareDto {
        private UUID id;
        private String fromAirportCode;
        private String toAirportCode;
        private BigDecimal amount = BigDecimal.ZERO;
        private String currencyCode;
        private LocalDate effectiveFrom;
        private LocalDate effectiveTo;
        private String status = "ACTIVE";
        private String source = "MANUAL";
        private String provider;
        private String providerOfferId;
        private OffsetDateTime fetchedAt;
        private String remarks;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getFromAirportCode() { return fromAirportCode; }
        public void setFromAirportCode(String fromAirportCode) { this.fromAirportCode = fromAirportCode; }
        public String getToAirportCode() { return toAirportCode; }
        public void setToAirportCode(String toAirportCode) { this.toAirportCode = toAirportCode; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        public LocalDate getEffectiveFrom() { return effectiveFrom; }
        public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
        public LocalDate getEffectiveTo() { return effectiveTo; }
        public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getProviderOfferId() { return providerOfferId; }
        public void setProviderOfferId(String providerOfferId) { this.providerOfferId = providerOfferId; }
        public OffsetDateTime getFetchedAt() { return fetchedAt; }
        public void setFetchedAt(OffsetDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    public static class LedgerDto {
        private UUID id;
        private UUID employeeId;
        private UUID leaveRequestId;
        private String entryType;
        private LocalDate entryDate;
        private BigDecimal amount = BigDecimal.ZERO;
        private String fromAirportCode;
        private String toAirportCode;
        private String status = "ACTIVE";
        private String remarks;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getEmployeeId() { return employeeId; }
        public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
        public UUID getLeaveRequestId() { return leaveRequestId; }
        public void setLeaveRequestId(UUID leaveRequestId) { this.leaveRequestId = leaveRequestId; }
        public String getEntryType() { return entryType; }
        public void setEntryType(String entryType) { this.entryType = entryType; }
        public LocalDate getEntryDate() { return entryDate; }
        public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getFromAirportCode() { return fromAirportCode; }
        public void setFromAirportCode(String fromAirportCode) { this.fromAirportCode = fromAirportCode; }
        public String getToAirportCode() { return toAirportCode; }
        public void setToAirportCode(String toAirportCode) { this.toAirportCode = toAirportCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    public static class BalanceDto {
        private UUID employeeId;
        private String employeeNumber;
        private String employeeName;
        private LocalDate hireDate;
        private LocalDate asOfDate;
        private String fromAirportCode;
        private String toAirportCode;
        private BigDecimal ticketAmount = BigDecimal.ZERO;
        private int cycleMonths = 12;
        private BigDecimal accruedMonths = BigDecimal.ZERO;
        private BigDecimal accruedAmount = BigDecimal.ZERO;
        private BigDecimal adjustmentCredit = BigDecimal.ZERO;
        private BigDecimal usedAmount = BigDecimal.ZERO;
        private BigDecimal balance = BigDecimal.ZERO;
        private String message;

        public UUID getEmployeeId() { return employeeId; }
        public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
        public String getEmployeeNumber() { return employeeNumber; }
        public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        public LocalDate getHireDate() { return hireDate; }
        public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
        public LocalDate getAsOfDate() { return asOfDate; }
        public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
        public String getFromAirportCode() { return fromAirportCode; }
        public void setFromAirportCode(String fromAirportCode) { this.fromAirportCode = fromAirportCode; }
        public String getToAirportCode() { return toAirportCode; }
        public void setToAirportCode(String toAirportCode) { this.toAirportCode = toAirportCode; }
        public BigDecimal getTicketAmount() { return ticketAmount; }
        public void setTicketAmount(BigDecimal ticketAmount) { this.ticketAmount = ticketAmount; }
        public int getCycleMonths() { return cycleMonths; }
        public void setCycleMonths(int cycleMonths) { this.cycleMonths = cycleMonths; }
        public BigDecimal getAccruedMonths() { return accruedMonths; }
        public void setAccruedMonths(BigDecimal accruedMonths) { this.accruedMonths = accruedMonths; }
        public BigDecimal getAccruedAmount() { return accruedAmount; }
        public void setAccruedAmount(BigDecimal accruedAmount) { this.accruedAmount = accruedAmount; }
        public BigDecimal getAdjustmentCredit() { return adjustmentCredit; }
        public void setAdjustmentCredit(BigDecimal adjustmentCredit) { this.adjustmentCredit = adjustmentCredit; }
        public BigDecimal getUsedAmount() { return usedAmount; }
        public void setUsedAmount(BigDecimal usedAmount) { this.usedAmount = usedAmount; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class AccrualReportDto {
        private LocalDate asOfDate;
        private UUID projectId;
        private String payGroup;
        private int employeeCount;
        private int missingSetupCount;
        private BigDecimal totalTicketAmount = BigDecimal.ZERO;
        private BigDecimal totalAccruedAmount = BigDecimal.ZERO;
        private BigDecimal totalAdjustmentCredit = BigDecimal.ZERO;
        private BigDecimal totalUsedAmount = BigDecimal.ZERO;
        private BigDecimal totalBalance = BigDecimal.ZERO;
        private List<BalanceDto> rows = new ArrayList<>();

        public LocalDate getAsOfDate() { return asOfDate; }
        public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
        public UUID getProjectId() { return projectId; }
        public void setProjectId(UUID projectId) { this.projectId = projectId; }
        public String getPayGroup() { return payGroup; }
        public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        public int getMissingSetupCount() { return missingSetupCount; }
        public void setMissingSetupCount(int missingSetupCount) { this.missingSetupCount = missingSetupCount; }
        public BigDecimal getTotalTicketAmount() { return totalTicketAmount; }
        public void setTotalTicketAmount(BigDecimal totalTicketAmount) { this.totalTicketAmount = totalTicketAmount; }
        public BigDecimal getTotalAccruedAmount() { return totalAccruedAmount; }
        public void setTotalAccruedAmount(BigDecimal totalAccruedAmount) { this.totalAccruedAmount = totalAccruedAmount; }
        public BigDecimal getTotalAdjustmentCredit() { return totalAdjustmentCredit; }
        public void setTotalAdjustmentCredit(BigDecimal totalAdjustmentCredit) { this.totalAdjustmentCredit = totalAdjustmentCredit; }
        public BigDecimal getTotalUsedAmount() { return totalUsedAmount; }
        public void setTotalUsedAmount(BigDecimal totalUsedAmount) { this.totalUsedAmount = totalUsedAmount; }
        public BigDecimal getTotalBalance() { return totalBalance; }
        public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }
        public List<BalanceDto> getRows() { return rows; }
        public void setRows(List<BalanceDto> rows) { this.rows = rows; }
    }
}
