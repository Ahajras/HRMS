package com.hrms.workpackage.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_package")
public class WorkPackage extends AuditableEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "cost_code_id")
    private UUID costCodeId;
    @Column(name = "code", nullable = false, length = 40)
    private String code;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "planned_start")
    private LocalDate plannedStart;
    @Column(name = "planned_end")
    private LocalDate plannedEnd;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "PLANNED";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getCostCodeId() { return costCodeId; }
    public void setCostCodeId(UUID costCodeId) { this.costCodeId = costCodeId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDate plannedStart) { this.plannedStart = plannedStart; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDate plannedEnd) { this.plannedEnd = plannedEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
