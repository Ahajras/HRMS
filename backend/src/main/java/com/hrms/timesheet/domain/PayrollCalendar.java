package com.hrms.timesheet.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A company's payroll calendar (FTDD Vol.1 Ch.4). Periods and weeks are
 * generated from it. {@code frequency} is MONTHLY for now; {@code weekStart}
 * is the day-of-week each week begins on (MON..SUN).
 */
@Entity
@Table(name = "payroll_calendar")
public class PayrollCalendar extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "frequency", nullable = false, length = 20)
    private String frequency = "MONTHLY";

    @Column(name = "week_start", nullable = false, length = 3)
    private String weekStart = "SAT";

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
