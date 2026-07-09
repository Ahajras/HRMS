package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "provision_run")
public class ProvisionRun extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "period_id", nullable = false)
    private UUID periodId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "pay_group", nullable = false, length = 30)
    private String payGroup = "ALL";

    @Column(name = "provision_type", nullable = false, length = 30)
    private String provisionType;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "CALCULATED";

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    @Column(name = "employee_count", nullable = false)
    private int employeeCount;

    @Column(name = "total_eligible_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalEligibleAmount = BigDecimal.ZERO;

    @Column(name = "total_provision_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalProvisionAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getPeriodId() { return periodId; }
    public void setPeriodId(UUID periodId) { this.periodId = periodId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
    public String getProvisionType() { return provisionType; }
    public void setProvisionType(String provisionType) { this.provisionType = provisionType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
    public int getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
    public BigDecimal getTotalEligibleAmount() { return totalEligibleAmount; }
    public void setTotalEligibleAmount(BigDecimal totalEligibleAmount) { this.totalEligibleAmount = totalEligibleAmount; }
    public BigDecimal getTotalProvisionAmount() { return totalProvisionAmount; }
    public void setTotalProvisionAmount(BigDecimal totalProvisionAmount) { this.totalProvisionAmount = totalProvisionAmount; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
