package com.hrms.timesheet.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A configurable day/segment classification (FTDD Vol.1 Ch.5). {@code factor}
 * is a reference multiplier; authoritative pay rates live in the Rule Engine.
 * Categories: REGULAR, OVERTIME, REST, HOLIDAY, ABSENCE, LEAVE.
 */
@Entity
@Table(name = "time_type")
public class TimeType extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "paid", nullable = false)
    private boolean paid = true;

    @Column(name = "counts_as_worked", nullable = false)
    private boolean countsAsWorked = true;

    @Column(name = "affects_leave", nullable = false)
    private boolean affectsLeave;

    @Column(name = "factor", nullable = false, precision = 6, scale = 3)
    private BigDecimal factor = BigDecimal.ONE;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public boolean isCountsAsWorked() { return countsAsWorked; }
    public void setCountsAsWorked(boolean countsAsWorked) { this.countsAsWorked = countsAsWorked; }

    public boolean isAffectsLeave() { return affectsLeave; }
    public void setAffectsLeave(boolean affectsLeave) { this.affectsLeave = affectsLeave; }

    public BigDecimal getFactor() { return factor; }
    public void setFactor(BigDecimal factor) { this.factor = factor; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
