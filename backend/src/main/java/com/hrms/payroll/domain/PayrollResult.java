package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** One employee's computed payslip header within a {@link PayrollRun}. */
@Entity
@Table(name = "payroll_result")
public class PayrollResult extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "pay_status", length = 20)
    private String payStatus;

    @Column(name = "rate_basis", length = 20)
    private String rateBasis;

    @Column(name = "divisor", precision = 6, scale = 2)
    private BigDecimal divisor;

    @Column(name = "daily_rate", nullable = false, precision = 18, scale = 4)
    private BigDecimal dailyRate = BigDecimal.ZERO;

    @Column(name = "hourly_rate", nullable = false, precision = 18, scale = 4)
    private BigDecimal hourlyRate = BigDecimal.ZERO;

    @Column(name = "worked_days", nullable = false, precision = 6, scale = 2)
    private BigDecimal workedDays = BigDecimal.ZERO;

    @Column(name = "normal_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal normalHours = BigDecimal.ZERO;

    @Column(name = "ot_hours", nullable = false, precision = 8, scale = 2)
    private BigDecimal otHours = BigDecimal.ZERO;

    @Column(name = "gross", nullable = false, precision = 18, scale = 4)
    private BigDecimal gross = BigDecimal.ZERO;

    @Column(name = "total_earnings", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 18, scale = 4)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "net", nullable = false, precision = 18, scale = 4)
    private BigDecimal net = BigDecimal.ZERO;

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

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getPayStatus() { return payStatus; }
    public void setPayStatus(String payStatus) { this.payStatus = payStatus; }

    public String getRateBasis() { return rateBasis; }
    public void setRateBasis(String rateBasis) { this.rateBasis = rateBasis; }

    public BigDecimal getDivisor() { return divisor; }
    public void setDivisor(BigDecimal divisor) { this.divisor = divisor; }

    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }

    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }

    public BigDecimal getWorkedDays() { return workedDays; }
    public void setWorkedDays(BigDecimal workedDays) { this.workedDays = workedDays; }

    public BigDecimal getNormalHours() { return normalHours; }
    public void setNormalHours(BigDecimal normalHours) { this.normalHours = normalHours; }

    public BigDecimal getOtHours() { return otHours; }
    public void setOtHours(BigDecimal otHours) { this.otHours = otHours; }

    public BigDecimal getGross() { return gross; }
    public void setGross(BigDecimal gross) { this.gross = gross; }

    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getNet() { return net; }
    public void setNet(BigDecimal net) { this.net = net; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
