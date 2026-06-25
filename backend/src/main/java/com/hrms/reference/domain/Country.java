package com.hrms.reference.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * ISO 3166-1 alpha-2 country. {@code defaultCurrencyCode} links to the
 * currency used by default for companies operating in this country.
 */
@Entity
@Table(name = "country")
public class Country extends AuditableEntity {

    @Column(name = "code", nullable = false, length = 2)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "default_currency_code", length = 3)
    private String defaultCurrencyCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDefaultCurrencyCode() { return defaultCurrencyCode; }
    public void setDefaultCurrencyCode(String defaultCurrencyCode) { this.defaultCurrencyCode = defaultCurrencyCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
