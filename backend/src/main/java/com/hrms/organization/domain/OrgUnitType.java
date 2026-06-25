package com.hrms.organization.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Defines a level in the organisation hierarchy (Company, Business Unit,
 * Division, Department, Section, Team, ...). The hierarchy is a configurable
 * set of types rather than fixed named levels (FTDD Vol.2 Ch.32.7). A null
 * {@code companyId} denotes a global default level set.
 */
@Entity
@Table(name = "org_unit_type")
public class OrgUnitType extends AuditableEntity {

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "level_order", nullable = false)
    private int levelOrder;

    @Column(name = "mandatory", nullable = false)
    private boolean mandatory = false;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getLevelOrder() { return levelOrder; }
    public void setLevelOrder(int levelOrder) { this.levelOrder = levelOrder; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
