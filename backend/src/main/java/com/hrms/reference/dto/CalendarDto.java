package com.hrms.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Transport object for {@link com.hrms.reference.domain.Calendar}.
 */
public class CalendarDto {

    private UUID id;

    private UUID companyId;

    @NotNull
    private Integer year;

    @NotBlank
    @Size(max = 100)
    private String name;

    private String status = "ACTIVE";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
