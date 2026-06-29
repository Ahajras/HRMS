package com.hrms.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class OvertimeCategoryDto {

    private UUID id;

    @NotBlank
    @Size(max = 20)
    private String code;

    @NotBlank
    @Size(max = 150)
    private String name;

    private boolean otEligible = true;

    private String status = "ACTIVE";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isOtEligible() { return otEligible; }
    public void setOtEligible(boolean otEligible) { this.otEligible = otEligible; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
