package com.hrms.crew.domain;

import com.hrms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/** A required job title (trade) and its planned head-count for a {@link Crew}. */
@Entity
@Table(name = "crew_trade")
public class CrewTrade extends AuditableEntity {

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "crew_id", nullable = false)
    private UUID crewId;

    @Column(name = "trade_code", nullable = false, length = 40)
    private String tradeCode;

    @Column(name = "trade_name", length = 150)
    private String tradeName;

    @Column(name = "planned_count", nullable = false)
    private int plannedCount;

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getCrewId() { return crewId; }
    public void setCrewId(UUID crewId) { this.crewId = crewId; }

    public String getTradeCode() { return tradeCode; }
    public void setTradeCode(String tradeCode) { this.tradeCode = tradeCode; }

    public String getTradeName() { return tradeName; }
    public void setTradeName(String tradeName) { this.tradeName = tradeName; }

    public int getPlannedCount() { return plannedCount; }
    public void setPlannedCount(int plannedCount) { this.plannedCount = plannedCount; }
}
