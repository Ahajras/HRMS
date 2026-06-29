package com.hrms.reference.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Overtime eligibility category. When {@code otEligible} is false, overtime
 * worked by employees in this category is NOT counted (the timesheet zeroes
 * their OT regardless of hours worked).
 */
@Entity
@Table(name = "overtime_category")
public class OvertimeCategory extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "ot_eligible", nullable = false)
    private boolean otEligible = true;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isOtEligible() { return otEligible; }
    public void setOtEligible(boolean otEligible) { this.otEligible = otEligible; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
