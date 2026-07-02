package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TimeTypePayrollRuleDto {
    private UUID id;
    private UUID timeTypeId;
    private UUID payrollComponentId;
    private String payrollComponentCode;
    private String payrollComponentName;
    private String action = "PAY";
    private BigDecimal percent = new BigDecimal("100.00");
    private String basis = "HOURS";
    private boolean affectsOvertime;
    private boolean processSeparately;
    private int sortOrder = 100;
    private String remarks;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTimeTypeId() { return timeTypeId; }
    public void setTimeTypeId(UUID timeTypeId) { this.timeTypeId = timeTypeId; }
    public UUID getPayrollComponentId() { return payrollComponentId; }
    public void setPayrollComponentId(UUID payrollComponentId) { this.payrollComponentId = payrollComponentId; }
    public String getPayrollComponentCode() { return payrollComponentCode; }
    public void setPayrollComponentCode(String payrollComponentCode) { this.payrollComponentCode = payrollComponentCode; }
    public String getPayrollComponentName() { return payrollComponentName; }
    public void setPayrollComponentName(String payrollComponentName) { this.payrollComponentName = payrollComponentName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public BigDecimal getPercent() { return percent; }
    public void setPercent(BigDecimal percent) { this.percent = percent; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
    public boolean isAffectsOvertime() { return affectsOvertime; }
    public void setAffectsOvertime(boolean affectsOvertime) { this.affectsOvertime = affectsOvertime; }
    public boolean isProcessSeparately() { return processSeparately; }
    public void setProcessSeparately(boolean processSeparately) { this.processSeparately = processSeparately; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
