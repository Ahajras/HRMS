package com.hrms.migration.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Faithful archive of a single employee's legacy FoxPro/DBF row, exactly as
 * exported from the old system. Stores the full header row and all detail (pay)
 * lines as JSONB so that EVERY legacy column has a place with us — even columns
 * that are empty in the current snapshot but may be populated in a future
 * export.
 *
 * <p>This is an archive, not business data: the engines read from the normalized
 * tables (employee, contract, ...). One row per (company, employee_number);
 * re-importing a fresh snapshot overwrites it (idempotent).
 */
@Entity
@Table(name = "legacy_employee_raw")
public class LegacyEmployeeRaw extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "employee_number", nullable = false, length = 50)
    private String employeeNumber;

    @Column(name = "source", length = 50)
    private String source;

    /** Full legacy header row (all columns), serialized as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "header_json")
    private String headerJson;

    /** Array of all legacy detail (pay) lines, serialized as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail_json")
    private String detailJson;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt = Instant.now();

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getHeaderJson() { return headerJson; }
    public void setHeaderJson(String headerJson) { this.headerJson = headerJson; }

    public String getDetailJson() { return detailJson; }
    public void setDetailJson(String detailJson) { this.detailJson = detailJson; }

    public Instant getImportedAt() { return importedAt; }
    public void setImportedAt(Instant importedAt) { this.importedAt = importedAt; }
}
