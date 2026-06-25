package com.hrms.reference.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Generic configurable code-list value (FTDD configuration-first principle).
 *
 * <p>Dropdown sources (GENDER, MARITAL_STATUS, CONTRACT_TYPE, EMPLOYEE_STATUS,
 * CONTRACT_STATUS, DOCUMENT_TYPE, ...) are stored as data, not hardcoded enums.
 * {@code companyId == null} is a global default; a company may override a
 * (category, code) by inserting its own row.
 */
@Entity
@Table(name = "lookup_value")
public class LookupValue extends AuditableEntity {

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
