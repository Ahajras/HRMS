package com.hrms.payroll.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_rule")
public class PayrollRule extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "pay_group", nullable = false, length = 20)
    private String payGroup;

    @Column(name = "pay_item_basis", nullable = false, length = 30)
    private String payItemBasis = "FIXED_AMOUNT";

    @Column(name = "quantity_source", nullable = false, length = 30)
    private String quantitySource = "PAYABLE_SCHEDULE";

    @Column(name = "ot_multiplier", nullable = false, precision = 8, scale = 4)
    private BigDecimal otMultiplier = new BigDecimal("1.2500");

    @Column(name = "standard_hours_per_day", nullable = false, precision = 5, scale = 2)
    private BigDecimal standardHoursPerDay = new BigDecimal("8.00");

    @Column(name = "month_divisor", nullable = false, precision = 6, scale = 2)
    private BigDecimal monthDivisor = new BigDecimal("30.00");

    @Column(name = "divisor_mode", nullable = false, length = 20)
    private String divisorMode = "FIXED";

    @Column(name = "rest_day_ot_multiplier", nullable = false, precision = 8, scale = 4)
    private BigDecimal restDayOtMultiplier = new BigDecimal("1.5000");

    @Column(name = "weekly_rest_paid", nullable = false)
    private boolean weeklyRestPaid = true;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    /** If set (e.g. 22), any day after this day-of-month gets marked
     * "estimated" when the project is locked — Day Zero early-close. Null
     * means the feature is off for this rule (normal end-of-month lock). */
    @Column(name = "day_zero_cutoff_day")
    private Integer dayZeroCutoffDay;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getPayGroup() { return payGroup; }
    public void setPayGroup(String payGroup) { this.payGroup = payGroup; }
    public String getPayItemBasis() { return payItemBasis; }
    public void setPayItemBasis(String payItemBasis) { this.payItemBasis = payItemBasis; }
    public String getQuantitySource() { return quantitySource; }
    public void setQuantitySource(String quantitySource) { this.quantitySource = quantitySource; }
    public BigDecimal getOtMultiplier() { return otMultiplier; }
    public void setOtMultiplier(BigDecimal otMultiplier) { this.otMultiplier = otMultiplier; }
    public BigDecimal getStandardHoursPerDay() { return standardHoursPerDay; }
    public void setStandardHoursPerDay(BigDecimal standardHoursPerDay) { this.standardHoursPerDay = standardHoursPerDay; }
    public BigDecimal getMonthDivisor() { return monthDivisor; }
    public void setMonthDivisor(BigDecimal monthDivisor) { this.monthDivisor = monthDivisor; }
    public String getDivisorMode() { return divisorMode; }
    public void setDivisorMode(String divisorMode) { this.divisorMode = divisorMode; }
    public BigDecimal getRestDayOtMultiplier() { return restDayOtMultiplier; }
    public void setRestDayOtMultiplier(BigDecimal restDayOtMultiplier) { this.restDayOtMultiplier = restDayOtMultiplier; }
    public boolean isWeeklyRestPaid() { return weeklyRestPaid; }
    public void setWeeklyRestPaid(boolean weeklyRestPaid) { this.weeklyRestPaid = weeklyRestPaid; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getDayZeroCutoffDay() { return dayZeroCutoffDay; }
    public void setDayZeroCutoffDay(Integer dayZeroCutoffDay) { this.dayZeroCutoffDay = dayZeroCutoffDay; }
}
