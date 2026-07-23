package com.hrms.workpackage.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_package_crew")
public class WorkPackageCrew extends AuditableEntity {
    @Column(name = "company_id", nullable = false)
    private UUID companyId;
    @Column(name = "work_package_id", nullable = false)
    private UUID workPackageId;
    @Column(name = "crew_id", nullable = false)
    private UUID crewId;
    @Column(name = "planned_start")
    private LocalDate plannedStart;
    @Column(name = "planned_end")
    private LocalDate plannedEnd;
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getWorkPackageId() { return workPackageId; }
    public void setWorkPackageId(UUID workPackageId) { this.workPackageId = workPackageId; }
    public UUID getCrewId() { return crewId; }
    public void setCrewId(UUID crewId) { this.crewId = crewId; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDate plannedStart) { this.plannedStart = plannedStart; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDate plannedEnd) { this.plannedEnd = plannedEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
