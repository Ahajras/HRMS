package com.hrms.crew.dto;

import java.time.LocalDate;
import java.util.UUID;

public class CrewMemberDto {

    private UUID id;
    private UUID crewId;
    private UUID employeeId;
    private String employeeName;
    private String employeeNumber;
    private UUID shiftId;
    private String shiftCode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCrewId() { return crewId; }
    public void setCrewId(UUID crewId) { this.crewId = crewId; }

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public String getShiftCode() { return shiftCode; }
    public void setShiftCode(String shiftCode) { this.shiftCode = shiftCode; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
