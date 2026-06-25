package com.hrms.security.domain;

import com.hrms.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A fine-grained, code-based access right (e.g. {@code employee.write}).
 *
 * <p>Permissions are global and immutable reference data; roles aggregate them
 * (FTDD Vol.2 Ch.31 - permission-driven authorisation).
 */
@Entity
@Table(name = "permission")
public class Permission extends BaseEntity {

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "name", length = 150)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
