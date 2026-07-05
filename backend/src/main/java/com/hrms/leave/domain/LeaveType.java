package com.hrms.leave.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "leave_type")
public class LeaveType extends AuditableEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "code", nullable = false, length = 30)
    private String code;
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    @Column(name = "time_type_id", nullable = false)
    private UUID timeTypeId;
    @Column(name = "deducts_balance", nullable = false)
    private boolean deductsBalance = true;
    @Column(name = "paid", nullable = false)
    private boolean paid = true;
    @Column(name = "requires_ticket_default", nullable = false)
    private boolean requiresTicketDefault;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getTimeTypeId() { return timeTypeId; }
    public void setTimeTypeId(UUID timeTypeId) { this.timeTypeId = timeTypeId; }
    public boolean isDeductsBalance() { return deductsBalance; }
    public void setDeductsBalance(boolean deductsBalance) { this.deductsBalance = deductsBalance; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public boolean isRequiresTicketDefault() { return requiresTicketDefault; }
    public void setRequiresTicketDefault(boolean requiresTicketDefault) { this.requiresTicketDefault = requiresTicketDefault; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
