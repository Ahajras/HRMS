package com.hrms.reference.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Transport object for {@link com.hrms.reference.domain.Currency}.
 */
public class CurrencyDto {

    private UUID id;

    @NotBlank
    @Size(min = 3, max = 3)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 8)
    private String symbol;

    @Min(0)
    private int minorUnits = 2;

    private String status = "ACTIVE";

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getMinorUnits() { return minorUnits; }
    public void setMinorUnits(int minorUnits) { this.minorUnits = minorUnits; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
