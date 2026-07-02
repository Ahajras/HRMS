package com.hrms.timesheet.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "time_type_payroll_rule")
public class TimeTypePayrollRule extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "time_type_id", nullable = false)
    private UUID timeTypeId;

    @Column(name = "payroll_component_id", nullable = false)
    private UUID payrollComponentId;

    @Column(name = "action", nullable = false, length = 20)
    private String action = "PAY";

    @Column(name = "percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal percent = new BigDecimal("100.00");

    @Column(name = "basis", nullable = false, length = 20)
    private String basis = "HOURS";

    @Column(name = "affects_overtime", nullable = false)
    private boolean affectsOvertime;

    @Column(name = "process_separately", nullable = false)
    private boolean processSeparately;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 100;

    @Column(name = "remarks", length = 500)
    private String remarks;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getTimeTypeId() { return timeTypeId; }
    public void setTimeTypeId(UUID timeTypeId) { this.timeTypeId = timeTypeId; }
    public UUID getPayrollComponentId() { return payrollComponentId; }
    public void setPayrollComponentId(UUID payrollComponentId) { this.payrollComponentId = payrollComponentId; }
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
