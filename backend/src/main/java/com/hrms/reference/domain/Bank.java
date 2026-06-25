package com.hrms.reference.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Bank reference master data, used by employee bank accounts and WPS export.
 * {@code companyId == null} is a global default.
 */
@Entity
@Table(name = "bank")
public class Bank extends AuditableEntity {

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "swift_code", length = 20)
    private String swiftCode;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSwiftCode() { return swiftCode; }
    public void setSwiftCode(String swiftCode) { this.swiftCode = swiftCode; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
