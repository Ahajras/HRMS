package com.hrms.project.dto;

import java.util.UUID;

public class SponsorDto {
    private UUID id;
    private String code;
    private String name;
    private String establishmentEid;
    private String payerQid;
    private String payerBankCode;
    private String payerIban;
    private String status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEstablishmentEid() { return establishmentEid; }
    public void setEstablishmentEid(String establishmentEid) { this.establishmentEid = establishmentEid; }
    public String getPayerQid() { return payerQid; }
    public void setPayerQid(String payerQid) { this.payerQid = payerQid; }
    public String getPayerBankCode() { return payerBankCode; }
    public void setPayerBankCode(String payerBankCode) { this.payerBankCode = payerBankCode; }
    public String getPayerIban() { return payerIban; }
    public void setPayerIban(String payerIban) { this.payerIban = payerIban; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
