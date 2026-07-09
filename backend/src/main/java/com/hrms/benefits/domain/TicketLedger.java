package com.hrms.benefits.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ticket_ledger")
public class TicketLedger extends AuditableEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @Column(name = "leave_request_id")
    private UUID leaveRequestId;
    @Column(name = "entry_type", nullable = false, length = 30)
    private String entryType;
    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;
    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;
    @Column(name = "from_airport_code", length = 20)
    private String fromAirportCode;
    @Column(name = "to_airport_code", length = 20)
    private String toAirportCode;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
    @Column(name = "remarks", length = 500)
    private String remarks;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
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
