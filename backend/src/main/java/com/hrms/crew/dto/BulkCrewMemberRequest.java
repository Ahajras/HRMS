package com.hrms.crew.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Add many employees to a crew at once, all on the same shift. */
public class BulkCrewMemberRequest {

    private UUID shiftId;
    private LocalDate effectiveFrom;
    private List<UUID> employeeIds = new ArrayList<>();

    public UUID getShiftId() { return shiftId; }
    public void setShiftId(UUID shiftId) { this.shiftId = shiftId; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public List<UUID> getEmployeeIds() { return employeeIds; }
    public void setEmployeeIds(List<UUID> employeeIds) { this.employeeIds = employeeIds; }
}
