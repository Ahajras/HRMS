package com.hrms.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Transport object for {@link com.hrms.reference.domain.Country}.
 */
public class CountryDto {

    private UUID id;

    @NotBlank
    @Size(min = 2, max = 2)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(min = 3, max = 3)
    private String defaultCurrencyCode;

    private String status = "ACTIVE";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDefaultCurrencyCode() { return defaultCurrencyCode; }
    public void setDefaultCurrencyCode(String defaultCurrencyCode) { this.defaultCurrencyCode = defaultCurrencyCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
