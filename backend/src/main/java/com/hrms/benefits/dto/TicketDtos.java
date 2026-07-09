package com.hrms.benefits.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
}
