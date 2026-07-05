package com.hrms.leave.dto;

import java.util.UUID;

public class LeaveTypeDto {
    private UUID id;
    private String code;
    private String name;
    private UUID timeTypeId;
    private String timeTypeCode;
    private boolean deductsBalance = true;
    private boolean paid = true;
    private boolean requiresTicketDefault;
    private String status = "ACTIVE";
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getTimeTypeId() { return timeTypeId; }
    public void setTimeTypeId(UUID timeTypeId) { this.timeTypeId = timeTypeId; }
    public String getTimeTypeCode() { return timeTypeCode; }
    public void setTimeTypeCode(String timeTypeCode) { this.timeTypeCode = timeTypeCode; }
    public boolean isDeductsBalance() { return deductsBalance; }
    public void setDeductsBalance(boolean deductsBalance) { this.deductsBalance = deductsBalance; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public boolean isRequiresTicketDefault() { return requiresTicketDefault; }
    public void setRequiresTicketDefault(boolean requiresTicketDefault) { this.requiresTicketDefault = requiresTicketDefault; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
