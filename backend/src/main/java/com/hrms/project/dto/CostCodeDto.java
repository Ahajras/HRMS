package com.hrms.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class CostCodeDto {

    private UUID id;
    private UUID companyId;

    @NotNull
    private UUID projectId;

    @NotBlank
    @Size(max = 40)
    private String code;

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 10)
    private String currencyCode = "QAR";

    @Size(max = 200)
    private String description;

    private Boolean active;

    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
