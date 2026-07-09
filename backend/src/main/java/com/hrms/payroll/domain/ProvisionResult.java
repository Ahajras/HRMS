package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "provision_result")
public class ProvisionResult extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "employee_number", length = 50)
    private String employeeNumber;

    @Column(name = "employee_name", length = 250)
    private String employeeName;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "pay_group", length = 30)
    private String payGroup;

    @Column(name = "eligible_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal eligibleAmount = BigDecimal.ZERO;

    @Column(name = "provision_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal provisionAmount = BigDecimal.ZERO;

    @Column(name = "formula_note", length = 500)
    private String formulaNote;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "OK";

    @Column(name = "message", length = 500)
    private String message;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
    public BigDecimal getEligibleAmount() { return eligibleAmount; }
    public void setEligibleAmount(BigDecimal eligibleAmount) { this.eligibleAmount = eligibleAmount; }
    public BigDecimal getProvisionAmount() { return provisionAmount; }
    public void setProvisionAmount(BigDecimal provisionAmount) { this.provisionAmount = provisionAmount; }
    public String getFormulaNote() { return formulaNote; }
    public void setFormulaNote(String formulaNote) { this.formulaNote = formulaNote; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
