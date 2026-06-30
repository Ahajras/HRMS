package com.hrms.crew.dto;

import java.util.UUID;

public class CrewTradeDto {

    private UUID id;
    private UUID crewId;
    private String tradeCode;
    private String tradeName;
    private int plannedCount;
    private int assignedCount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCrewId() { return crewId; }
    public void setCrewId(UUID crewId) { this.crewId = crewId; }

    public String getTradeCode() { return tradeCode; }
    public void setTradeCode(String tradeCode) { this.tradeCode = tradeCode; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    public int getPlannedCount() { return plannedCount; }
    public void setPlannedCount(int plannedCount) { this.plannedCount = plannedCount; }

    public int getAssignedCount() { return assignedCount; }
    public void setAssignedCount(int assignedCount) { this.assignedCount = assignedCount; }
}
