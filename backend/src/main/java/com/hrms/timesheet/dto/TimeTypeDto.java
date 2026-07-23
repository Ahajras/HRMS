package com.hrms.timesheet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class TimeTypeDto {

    private UUID id;
    private UUID companyId;
    private String code;
    private String name;
    private String category;
    private boolean paid = true;
    private boolean countsAsWorked = true;
    private boolean affectsLeave;
    private BigDecimal factor = BigDecimal.ONE;
    private int sortOrder;
    private String colorHex = "#64748b";
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public boolean isCountsAsWorked() { return countsAsWorked; }
    public void setCountsAsWorked(boolean countsAsWorked) { this.countsAsWorked = countsAsWorked; }

    public boolean isAffectsLeave() { return affectsLeave; }
    public void setAffectsLeave(boolean affectsLeave) { this.affectsLeave = affectsLeave; }

    public BigDecimal getFactor() { return factor; }
    public void setFactor(BigDecimal factor) { this.factor = factor; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
