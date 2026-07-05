package com.hrms.leave.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_adjustment")
public class LeaveAdjustment extends AuditableEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;
    @Column(name = "leave_type_id", nullable = false)
    private UUID leaveTypeId;
    @Column(name = "adjustment_type", nullable = false, length = 30)
    private String adjustmentType = "OPENING_USED";
    @Column(name = "days", nullable = false, precision = 8, scale = 2)
    private BigDecimal days = BigDecimal.ZERO;
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    @Column(name = "reason", length = 500)
    private String reason;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public UUID getLeaveTypeId() { return leaveTypeId; }
    public void setLeaveTypeId(UUID leaveTypeId) { this.leaveTypeId = leaveTypeId; }
    public String getAdjustmentType() { return adjustmentType; }
    public void setAdjustmentType(String adjustmentType) { this.adjustmentType = adjustmentType; }
    public BigDecimal getDays() { return days; }
    public void setDays(BigDecimal days) { this.days = days; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
