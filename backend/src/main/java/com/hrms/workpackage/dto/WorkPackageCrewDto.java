package com.hrms.workpackage.dto;

import java.time.LocalDate;
import java.util.UUID;

public class WorkPackageCrewDto {
    private UUID id;
    private UUID workPackageId;
    private UUID crewId;
    private String crewCode;
    private String crewName;
    private LocalDate plannedStart;
    private LocalDate plannedEnd;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWorkPackageId() { return workPackageId; }
    public void setWorkPackageId(UUID workPackageId) { this.workPackageId = workPackageId; }
    public UUID getCrewId() { return crewId; }
    public void setCrewId(UUID crewId) { this.crewId = crewId; }
    public String getCrewCode() { return crewCode; }
    public void setCrewCode(String crewCode) { this.crewCode = crewCode; }
    public String getCrewName() { return crewName; }
    public void setCrewName(String crewName) { this.crewName = crewName; }
    public LocalDate getPlannedStart() { return plannedStart; }
    public void setPlannedStart(LocalDate plannedStart) { this.plannedStart = plannedStart; }
    public LocalDate getPlannedEnd() { return plannedEnd; }
    public void setPlannedEnd(LocalDate plannedEnd) { this.plannedEnd = plannedEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
