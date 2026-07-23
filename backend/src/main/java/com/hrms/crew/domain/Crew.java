package com.hrms.crew.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A crew (foreman's team) belonging to a project (FTDD Vol.1 Ch.4; legacy PAYREF).
 * Employees are added as {@link CrewMember}s; a crew can be split across shifts
 * because each member carries its own shift.
 */
@Entity
@Table(name = "crew")
public class Crew extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "foreman_employee_id")
    private UUID foremanEmployeeId;

    @Column(name = "supervisor_employee_id")
    private UUID supervisorEmployeeId;

    @Column(name = "timekeeper_employee_id")
    private UUID timekeeperEmployeeId;

    @Column(name = "parent_crew_id")
    private UUID parentCrewId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getForemanEmployeeId() { return foremanEmployeeId; }
    public void setForemanEmployeeId(UUID foremanEmployeeId) { this.foremanEmployeeId = foremanEmployeeId; }

    public UUID getSupervisorEmployeeId() { return supervisorEmployeeId; }
    public void setSupervisorEmployeeId(UUID supervisorEmployeeId) { this.supervisorEmployeeId = supervisorEmployeeId; }

    public UUID getTimekeeperEmployeeId() { return timekeeperEmployeeId; }
    public void setTimekeeperEmployeeId(UUID timekeeperEmployeeId) { this.timekeeperEmployeeId = timekeeperEmployeeId; }

    public UUID getParentCrewId() { return parentCrewId; }
    public void setParentCrewId(UUID parentCrewId) { this.parentCrewId = parentCrewId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
