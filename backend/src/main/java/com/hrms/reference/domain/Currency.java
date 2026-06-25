package com.hrms.reference.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * ISO 4217 currency. {@code minorUnits} is the decimal scale used for rounding
 * money in this currency (FTDD Vol.2 Ch.24 currency precision).
 */
@Entity
@Table(name = "currency")
public class Currency extends AuditableEntity {

    @Column(name = "code", nullable = false, length = 3)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "symbol", length = 8)
    private String symbol;

    @Column(name = "minor_units", nullable = false)
    private int minorUnits = 2;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getMinorUnits() { return minorUnits; }
    public void setMinorUnits(int minorUnits) { this.minorUnits = minorUnits; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
