package com.hrms.organization.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Recursive view of the organisation tree for hierarchical display.
 */
public class OrgUnitTreeNode {

    private UUID id;
    private UUID parentId;
    private UUID typeId;
    private String code;
    private String name;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;
    private List<OrgUnitTreeNode> children = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public UUID getTypeId() { return typeId; }
    public void setTypeId(UUID typeId) { this.typeId = typeId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<OrgUnitTreeNode> getChildren() { return children; }
    public void setChildren(List<OrgUnitTreeNode> children) { this.children = children; }
}
