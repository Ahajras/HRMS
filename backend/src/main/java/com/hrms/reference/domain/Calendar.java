package com.hrms.reference.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Yearly calendar that defines working days and holidays for payroll periods
 * (FTDD Vol.1 Ch.2 Calendar Domain). A null {@code companyId} denotes a global
 * calendar shared across companies.
 */
@Entity
@Table(name = "calendar")
public class Calendar extends AuditableEntity {

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
